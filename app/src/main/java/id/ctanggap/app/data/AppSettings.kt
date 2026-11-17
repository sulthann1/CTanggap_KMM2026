package id.ctanggap.app.data

import android.content.Context
import id.ctanggap.app.detection.CrashDetector

/** Pengaturan sederhana berbasis SharedPreferences. */
class AppSettings(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("ctanggap", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboardingDone", false)
        set(v) = sp.edit().putBoolean("onboardingDone", v).apply()

    /** Wilayah uji (batasan wilayah), ditampilkan di Pengaturan dan dialog opt-in. */
    var regionName: String
        get() = sp.getString("regionName", "[Wilayah uji]") ?: ""
        set(v) = sp.edit().putString("regionName", v).apply()

    var userName: String
        get() = sp.getString("userName", "Pengguna C-Tanggap") ?: ""
        set(v) = sp.edit().putString("userName", v).apply()

    var medicalInfo: String // contoh: "Gol. darah O, alergi penisilin"
        get() = sp.getString("medicalInfo", "") ?: ""
        set(v) = sp.edit().putString("medicalInfo", v).apply()

    /** Nomor uji milik tim. Dipakai pada mode simulasi menggantikan nomor instansi sungguhan. */
    var testNumber: String
        get() = sp.getString("testNumber", "") ?: ""
        set(v) = sp.edit().putString("testNumber", v).apply()

    var countdownSec: Int
        get() = sp.getInt("countdownSec", 30)
        set(v) = sp.edit().putInt("countdownSec", v).apply()

    /** Waktu tunggu (detik) sejak hitungan mundur selesai sebelum eskalasi ke instansi. */
    var facilityWaitSec: Int
        get() = sp.getInt("facilityWaitSec", 180)
        set(v) = sp.edit().putInt("facilityWaitSec", v).apply()

    /** Persetujuan eskalasi ke fasilitas kesehatan, polisi, dan 112 (opt-in). */
    var escalationOptIn: Boolean
        get() = sp.getBoolean("escalationOptIn", false)
        set(v) = sp.edit().putBoolean("escalationOptIn", v).apply()

    /** DEFAULT TRUE: tahap instansi diarahkan ke nomor uji, bukan instansi sungguhan. */
    var simulationMode: Boolean
        get() = sp.getBoolean("simulationMode", true)
        set(v) = sp.edit().putBoolean("simulationMode", v).apply()

    var impactG: Float
        get() = sp.getFloat("impactG", 4f)
        set(v) = sp.edit().putFloat("impactG", v).apply()

    var logSensors: Boolean
        get() = sp.getBoolean("logSensors", false)
        set(v) = sp.edit().putBoolean("logSensors", v).apply()

    fun detectorConfig() = CrashDetector.Config(impactG = impactG)
}
