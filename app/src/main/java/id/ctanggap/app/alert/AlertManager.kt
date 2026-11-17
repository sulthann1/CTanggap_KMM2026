package id.ctanggap.app.alert

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.telephony.SmsManager
import id.ctanggap.app.data.*
import id.ctanggap.app.facility.Facility
import id.ctanggap.app.facility.FacilityRepository
import id.ctanggap.app.facility.Kind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Eskalasi bertahap (bagian 6.8 proposal), dengan status yang dipublikasikan ke IncidentState:
 *  Tahap 1: SMS ke kontak prioritas 1
 *  Tahap 2: +60 detik tanpa balasan YA -> SMS ke kontak 2 dan 3
 *  Tahap 3: +facilityWaitSec tanpa balasan dan opt-in -> SMS ke faskes dan polisi terdekat
 *  Tahap 4: panggilan ke 112 (opt-in)
 * Mode simulasi: tahap 3 dan 4 diarahkan ke settings.testNumber.
 */
class AlertManager(
    private val ctx: Context,
    private val dao: AppDao,
    private val settings: AppSettings,
    private val facilities: FacilityRepository
) {
    private val notified = mutableListOf<Pair<String, String>>() // (nama, nomor)

    private fun clock() = SimpleDateFormat("HH:mm:ss", Locale("id")).format(Date())
    private fun set(stage: Int, state: StageState, detail: String, eta: Long? = null) =
        IncidentState.update(stage) { it.copy(state = state, detail = detail, etaAtMs = eta) }

    suspend fun run(incidentId: Long, loc: Location?) {
        ResponseState.reset()
        IncidentState.phase.value = Phase.ESCALATING
        val contacts = dao.contactsOnce().sortedBy { it.priority }
        val msg = contactMessage(loc)

        // Tahap 1
        val c1 = contacts.firstOrNull()
        if (c1 == null) set(1, StageState.SKIPPED, "Belum ada kontak darurat")
        else {
            set(1, StageState.NOW, "Mengirim ke ${c1.name}...")
            val ok = sendSms(incidentId, 1, "KONTAK", c1.name, c1.phone, msg)
            set(1, StageState.DONE, "${c1.name}, ${if (ok) "terkirim" else "gagal"} ${clock()}")
        }
        set(2, StageState.NOW, "Menunggu balasan YA", System.currentTimeMillis() + 60_000)
        if (waitReply(60)) return handled(incidentId)

        // Tahap 2
        val others = contacts.drop(1).take(2)
        if (others.isEmpty()) set(2, StageState.SKIPPED, "Tidak ada kontak 2 dan 3")
        else {
            others.forEach { sendSms(incidentId, 2, "KONTAK", it.name, it.phone, msg) }
            set(2, StageState.DONE, "${others.joinToString { it.name }}, terkirim ${clock()}")
        }
        val wait3 = (settings.facilityWaitSec - 60).coerceAtLeast(0)
        set(3, StageState.WAIT, "Opsional (opt-in)", System.currentTimeMillis() + wait3 * 1_000L)
        if (waitReply(wait3)) return handled(incidentId)

        // Tahap 3 dan 4 (opt-in)
        if (!settings.escalationOptIn) {
            set(3, StageState.SKIPPED, "Eskalasi instansi nonaktif")
            set(4, StageState.SKIPPED, "Eskalasi instansi nonaktif")
            log(incidentId, 3, "FASKES", "-", "-", "SMS", "DILEWATI (opt-in nonaktif)")
            return finish(incidentId, "Peringatan sudah dikirim ke kontak darurat.")
        }
        val sim = settings.simulationMode
        set(3, StageState.NOW, "Mencari fasilitas terdekat...")
        val targets: List<Facility> = if (loc != null)
            facilities.pick(facilities.nearby(loc.latitude, loc.longitude)) else emptyList()
        val fMsg = facilityMessage(loc)
        for (f in targets) {
            val number = if (sim) settings.testNumber else f.phone.orEmpty()
            val type = if (f.kind == Kind.POLISI) "POLISI" else "FASKES"
            sendSms(incidentId, 3, type, if (sim) "[SIMULASI] ${f.name}" else f.name, number, fMsg)
        }
        set(3, StageState.DONE, "${targets.size} fasilitas diberi tahu${if (sim) " (simulasi)" else ""}")
        set(4, StageState.NOW, "Menghubungi ${if (sim) "nomor uji" else "112"}...")
        call(incidentId, 4, if (sim) settings.testNumber else "112")
        set(4, StageState.DONE, "Panggilan dibuka${if (sim) " (simulasi)" else ""}")
        finish(incidentId, "Eskalasi ke instansi selesai.")
    }

    /** Dipanggil ketika pengguna menyatakan aman setelah pesan terkirim. */
    suspend fun sendCancellation(incidentId: Long) {
        val text = "C-Tanggap: ${settings.userName} menyatakan AMAN. Abaikan peringatan sebelumnya."
        notified.toList().forEach { (name, phone) ->
            sms(phone, text)
            log(incidentId, 0, "KONTAK", name, phone, "SMS", "DIBATALKAN")
        }
    }

    private suspend fun handled(id: Long) {
        IncidentState.stages.value.filter { it.state == StageState.WAIT || it.state == StageState.NOW }
            .forEach { set(it.stage, StageState.SKIPPED, "Dihentikan karena balasan YA") }
        setStatus(id, "DITANGANI")
        IncidentState.outcome.value = "Kontak membalas YA. Eskalasi dihentikan."
        IncidentState.phase.value = Phase.DONE
    }

    private suspend fun finish(id: Long, text: String) {
        setStatus(id, "TERKIRIM")
        IncidentState.outcome.value = text
        IncidentState.phase.value = Phase.DONE
    }

    private suspend fun setStatus(id: Long, status: String) {
        dao.incidentOnce(id)?.let { dao.updateIncident(it.copy(status = status)) }
    }

    private suspend fun waitReply(sec: Int): Boolean =
        withTimeoutOrNull(sec * 1_000L) { ResponseState.responded.first { it } } != null

    private suspend fun sendSms(id: Long, stage: Int, type: String, name: String, phone: String, text: String): Boolean {
        if (phone.isBlank()) { log(id, stage, type, name, phone, "SMS", "DILEWATI (nomor kosong)"); return false }
        val ok = sms(phone, text)
        if (ok) notified += name to phone
        log(id, stage, type, name, phone, "SMS", if (ok) "TERKIRIM" else "GAGAL")
        return ok
    }

    private fun sms(phone: String, text: String): Boolean = try {
        val m = if (android.os.Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java)
        else @Suppress("DEPRECATION") SmsManager.getDefault()
        m.sendMultipartTextMessage(phone, null, m.divideMessage(text), null, null)
        true
    } catch (e: Exception) { false }

    /** ACTION_CALL hanya membuka sambungan; Android tidak mengizinkan menyuntikkan suara ke panggilan. */
    private suspend fun call(id: Long, stage: Int, number: String) {
        if (number.isBlank()) { log(id, stage, "112", "-", number, "CALL", "DILEWATI (nomor kosong)"); return }
        val ok = try {
            ctx.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) { false }
        log(id, stage, "112", if (settings.simulationMode) "[SIMULASI]" else "112", number, "CALL", if (ok) "TERKIRIM" else "GAGAL")
    }

    private suspend fun log(id: Long, stage: Int, type: String, name: String, phone: String, ch: String, status: String) =
        dao.insertLog(EscalationLog(incidentId = id, stage = stage, recipientType = type, recipientName = name,
            phone = phone, channel = ch, timeMs = System.currentTimeMillis(), status = status))

    // ----- isi pesan -----
    private fun time() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id")).format(Date())
    private fun link(l: Location?) = l?.let { "https://maps.google.com/?q=${it.latitude},${it.longitude}" } ?: "lokasi tidak tersedia"
    private fun medical() = settings.medicalInfo.takeIf { it.isNotBlank() }?.let { " Info medis: $it." } ?: ""

    private fun contactMessage(l: Location?) =
        "DARURAT C-Tanggap: ${settings.userName} diduga mengalami kecelakaan pada ${time()}. Lokasi: ${link(l)}.${medical()} " +
            "Balas YA jika Anda menuju lokasi. Pesan otomatis."

    private fun facilityMessage(l: Location?) =
        "PESAN OTOMATIS C-Tanggap: dugaan kecelakaan lalu lintas pada ${time()}. Korban: ${settings.userName}. " +
            "Lokasi: ${link(l)}.${medical()} Mohon verifikasi dan bantuan jika memungkinkan."
}
