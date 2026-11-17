package id.ctanggap.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationProvider(ctx: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(ctx.applicationContext)
    private var callback: LocationCallback? = null

    /** Pembaruan berkala (untuk kecepatan GPS) selama mode berkendara aktif. */
    @SuppressLint("MissingPermission")
    fun startUpdates(onLocation: (Location) -> Unit) {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000)
            .setMinUpdateIntervalMillis(1_000).build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) { r.lastLocation?.let(onLocation) }
        }
        callback = cb
        client.requestLocationUpdates(req, cb, Looper.getMainLooper())
    }

    fun stopUpdates() { callback?.let { client.removeLocationUpdates(it) }; callback = null }

    /** Lokasi terbaik saat insiden: coba lokasi aktual (maks. 10 detik), cadangan lokasi terakhir. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = try {
        withTimeoutOrNull(10_000) {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        } ?: client.lastLocation.await()
    } catch (e: Exception) { null }
}
