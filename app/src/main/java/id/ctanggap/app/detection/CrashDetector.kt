package id.ctanggap.app.detection

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Detektor indikasi benturan. Kelas Kotlin murni (tanpa dependensi Android) agar mudah diuji unit.
 *
 * Alur: MONITORING -> (lonjakan percepatan + sebelumnya melaju) -> VERIFYING
 *       -> (setelah jeda "settle", ponsel relatif diam dan kecepatan rendah) -> onSuspected()
 *
 * Semua nilai ambang adalah TITIK AWAL dan harus dikalibrasi dari data uji (lihat SensorLogger).
 */
class CrashDetector(
    private val cfg: Config = Config(),
    private val onSuspected: (Event) -> Unit
) {
    data class Config(
        val impactG: Float = 4f,            // ambang lonjakan percepatan (g)
        val minSpeedKmh: Float = 15f,       // dianggap "sedang berkendara" jika >= nilai ini
        val speedMemoryMs: Long = 5_000,    // lonjakan sah jika kecepatan tinggi tercatat dalam rentang ini
        val settleMs: Long = 1_000,         // jeda setelah benturan sebelum mengukur diam
        val stillWindowMs: Long = 10_000,   // lama pengamatan diam
        val stillStdMaxG: Float = 0.12f,    // simpangan baku besaran percepatan maksimum agar dianggap diam
        val maxSpeedAfterKmh: Float = 5f,   // kecepatan maksimum setelah benturan
        val cooldownMs: Long = 60_000
    )

    data class Event(val timeMs: Long, val peakG: Float, val speedBeforeKmh: Float)

    private enum class State { MONITORING, VERIFYING }

    private var state = State.MONITORING
    private var cooldownUntil = 0L

    private var lastSpeed = 0f
    private var lastSpeedT = Long.MIN_VALUE / 2
    private var lastFastT = Long.MIN_VALUE / 2
    private var fastSpeed = 0f

    private var impactT = 0L
    private var peakG = 0f
    private var speedBefore = 0f

    // Statistik berjalan (Welford) untuk besaran percepatan setelah benturan
    private var n = 0
    private var mean = 0.0
    private var m2 = 0.0

    fun reset() { state = State.MONITORING; n = 0; mean = 0.0; m2 = 0.0 }

    /** Kecepatan dari GPS (km/jam). tMs memakai basis waktu yang sama dengan onAccel. */
    fun onSpeed(tMs: Long, kmh: Float) {
        lastSpeed = kmh
        lastSpeedT = tMs
        if (kmh >= cfg.minSpeedKmh) { lastFastT = tMs; fastSpeed = kmh }
    }

    /** Percepatan dalam m/s^2 (termasuk gravitasi). */
    fun onAccel(tMs: Long, ax: Float, ay: Float, az: Float) {
        val g = sqrt(ax * ax + ay * ay + az * az) / 9.80665f
        when (state) {
            State.MONITORING -> {
                val wasMoving = tMs - lastFastT <= cfg.speedMemoryMs
                if (tMs >= cooldownUntil && g >= cfg.impactG && wasMoving) {
                    state = State.VERIFYING
                    impactT = tMs; peakG = g; speedBefore = fastSpeed
                    n = 0; mean = 0.0; m2 = 0.0
                }
            }
            State.VERIFYING -> {
                peakG = max(peakG, g)
                val dt = tMs - impactT
                if (dt >= cfg.settleMs) {
                    n++
                    val d = g - mean
                    mean += d / n
                    m2 += d * (g - mean)
                }
                if (dt >= cfg.settleMs + cfg.stillWindowMs) evaluate(tMs)
            }
        }
    }

    private fun evaluate(tMs: Long) {
        val std = if (n > 1) sqrt(m2 / (n - 1)) else Double.MAX_VALUE
        val gpsFresh = tMs - lastSpeedT <= 5_000
        val slow = !gpsFresh || lastSpeed <= cfg.maxSpeedAfterKmh
        val still = n >= 10 && std <= cfg.stillStdMaxG
        state = State.MONITORING
        if (still && slow) {
            cooldownUntil = tMs + cfg.cooldownMs
            onSuspected(Event(impactT, peakG, speedBefore))
        }
    }
}
