package id.ctanggap.app.detection

import android.content.Context
import java.io.File

/**
 * Merekam data sensor ke CSV untuk kalibrasi ambang.
 * Lokasi: Android/data/id.ctanggap.app/files/sensor_log_<waktu>.csv
 */
class SensorLogger(ctx: Context) {
    private val file = File(ctx.getExternalFilesDir(null), "sensor_log_${System.currentTimeMillis()}.csv")
    private val writer = file.bufferedWriter().also { it.write("t_ms,ax,ay,az,speed_kmh,label\n") }
    @Volatile var speedKmh = 0f
    @Volatile var label = ""   // isi manual saat uji, mis. "jatuh", "jalan_rusak"

    @Synchronized fun log(tMs: Long, ax: Float, ay: Float, az: Float) {
        writer.write("$tMs,$ax,$ay,$az,$speedKmh,$label\n")
    }
    @Synchronized fun close() { writer.flush(); writer.close() }
}
