package id.ctanggap.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import id.ctanggap.app.alert.AlertManager
import id.ctanggap.app.alert.IncidentState
import id.ctanggap.app.alert.Phase
import id.ctanggap.app.data.AppDatabase
import id.ctanggap.app.data.AppSettings
import id.ctanggap.app.data.Incident
import id.ctanggap.app.data.Trip
import id.ctanggap.app.detection.CrashDetector
import id.ctanggap.app.detection.SensorLogger
import id.ctanggap.app.facility.FacilityRepository
import id.ctanggap.app.location.LocationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/**
 * Foreground service: memantau sensor, mencatat perjalanan, menjalankan hitungan mundur, lalu memicu eskalasi.
 * Service (bukan Activity) memegang timer agar tetap berjalan walau layar hitungan mundur gagal tampil.
 */
class DrivingService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "id.ctanggap.START"
        const val ACTION_STOP = "id.ctanggap.STOP"
        const val ACTION_SOS = "id.ctanggap.SOS"
        const val ACTION_SAFE = "id.ctanggap.SAFE"
        const val ACTION_SEND_NOW = "id.ctanggap.SEND_NOW"
        val running = MutableStateFlow(false)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var settings: AppSettings
    private lateinit var sensors: SensorManager
    private lateinit var detector: CrashDetector
    private lateinit var locationProvider: LocationProvider
    private val dao by lazy { AppDatabase.get(this).dao() }
    private var logger: SensorLogger? = null

    private var countdownJob: Job? = null
    private var escalationJob: Job? = null
    private var alertManager: AlertManager? = null
    private var incidentId: Long = -1

    // catatan perjalanan
    private var tripId = -1L
    private var tripStart = 0L
    private var tripDistance = 0f
    private var tripHadIncident = false
    private var lastLoc: Location? = null

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
        sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationProvider = LocationProvider(this)
        detector = CrashDetector(settings.detectorConfig()) { startCountdown(trigger = "AUTO") }
        Notifier.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Setiap start via startForegroundService() wajib memanggil startForeground().
        val n = Notifier.monitoring(this)
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(Notifier.ID_MONITOR, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(Notifier.ID_MONITOR, n)

        when (intent?.action) {
            ACTION_STOP -> { stopMonitoring(); stopSelf() }
            ACTION_SOS -> { startMonitoring(); startCountdown(trigger = "SOS") }
            ACTION_SAFE -> userIsSafe()
            ACTION_SEND_NOW -> sendNow()
            else -> startMonitoring()
        }
        return START_STICKY
    }

    // ---------- pemantauan dan perjalanan ----------
    private fun startMonitoring() {
        if (running.value) return
        running.value = true
        tripStart = System.currentTimeMillis(); tripDistance = 0f; tripHadIncident = false; lastLoc = null
        scope.launch { tripId = dao.insertTrip(Trip(startMs = tripStart)) }

        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        if (settings.logSensors) logger = SensorLogger(this)
        try {
            locationProvider.startUpdates { loc: Location ->
                val prev = lastLoc
                if (prev != null && loc.hasSpeed() && loc.speed > 1f) tripDistance += prev.distanceTo(loc)
                lastLoc = loc
                IncidentState.locationText.value =
                    String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude)
                if (loc.hasSpeed()) {
                    val kmh = loc.speed * 3.6f
                    detector.onSpeed(SystemClock.elapsedRealtime(), kmh)
                    logger?.speedKmh = kmh
                }
            }
        } catch (_: SecurityException) { /* izin lokasi belum diberikan */ }
    }

    private fun stopMonitoring() {
        val was = running.value
        running.value = false
        sensors.unregisterListener(this)
        locationProvider.stopUpdates()
        logger?.close(); logger = null
        detector.reset()
        if (was && tripId > 0) {
            val t = Trip(id = tripId, startMs = tripStart, endMs = System.currentTimeMillis(),
                distanceM = tripDistance, hadIncident = tripHadIncident)
            CoroutineScope(Dispatchers.IO).launch { dao.updateTrip(t) }
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        val tMs = e.timestamp / 1_000_000L // basis waktu elapsedRealtime
        detector.onAccel(tMs, e.values[0], e.values[1], e.values[2])
        logger?.log(tMs, e.values[0], e.values[1], e.values[2])
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    // ---------- hitungan mundur dan eskalasi ----------
    private fun startCountdown(trigger: String) {
        if (countdownJob?.isActive == true || escalationJob?.isActive == true) return
        val sec = settings.countdownSec
        IncidentState.reset()
        IncidentState.deadlineMs.value = System.currentTimeMillis() + sec * 1_000L
        IncidentState.phase.value = Phase.COUNTDOWN
        tripHadIncident = true
        AlarmPlayer.start(this)
        Notifier.showCountdown(this)
        countdownJob = scope.launch {
            IncidentState.firstContact.value = dao.contactsOnce().firstOrNull()?.name ?: ""
            incidentId = dao.insertIncident(Incident(timeMs = System.currentTimeMillis(), lat = null, lng = null, trigger = trigger))
            delay(sec * 1_000L)
            AlarmPlayer.stop(this@DrivingService)
            escalate()
        }
    }

    /** "Kirim Bantuan Segera": lewati sisa hitungan mundur. */
    private fun sendNow() {
        if (IncidentState.phase.value != Phase.COUNTDOWN) return
        countdownJob?.cancel()
        AlarmPlayer.stop(this)
        escalate()
    }

    private fun escalate() {
        IncidentState.phase.value = Phase.ESCALATING
        Notifier.showStatus(this)
        escalationJob = scope.launch {
            val loc = locationProvider.currentLocation()
            updateIncident(incidentId) { it.copy(lat = loc?.latitude, lng = loc?.longitude, status = "TERKIRIM") }
            val am = AlertManager(this@DrivingService, dao, settings, FacilityRepository(this@DrivingService))
            alertManager = am
            am.run(incidentId, loc)
        }
    }

    private fun userIsSafe() {
        val phase = IncidentState.phase.value
        countdownJob?.cancel()
        escalationJob?.cancel()
        AlarmPlayer.stop(this)
        Notifier.cancelAlert(this)
        detector.reset()
        if (phase != Phase.NONE) {
            val id = incidentId
            val am = alertManager
            alertManager = null
            scope.launch {
                if (phase != Phase.COUNTDOWN) am?.sendCancellation(id)
                updateIncident(id) { it.copy(status = "DIBATALKAN") }
            }
        }
        IncidentState.phase.value = Phase.NONE
    }

    private suspend fun updateIncident(id: Long, f: (Incident) -> Incident) {
        dao.incidentOnce(id)?.let { dao.updateIncident(f(it)) }
    }

    override fun onDestroy() { stopMonitoring(); scope.cancel(); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
