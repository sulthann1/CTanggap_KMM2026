package id.ctanggap.app.alert

import kotlinx.coroutines.flow.MutableStateFlow

enum class Phase { NONE, COUNTDOWN, ESCALATING, DONE }
enum class StageState { WAIT, NOW, DONE, SKIPPED }

data class StageUi(
    val stage: Int,
    val title: String,
    val detail: String,
    val state: StageState,
    val etaAtMs: Long? = null // waktu perkiraan tahap berikutnya (untuk hitung mundur di UI)
)

/** Status insiden yang sedang berjalan; dibaca oleh AlertActivity (layar hitungan mundur dan status). */
object IncidentState {
    val phase = MutableStateFlow(Phase.NONE)
    val deadlineMs = MutableStateFlow(0L)
    val firstContact = MutableStateFlow("")
    val locationText = MutableStateFlow("belum tersedia")
    val outcome = MutableStateFlow("")
    val stages = MutableStateFlow(initial())

    private fun initial() = listOf(
        StageUi(1, "Tahap 1: SMS ke kontak 1", "Menunggu", StageState.WAIT),
        StageUi(2, "Tahap 2: SMS ke kontak 2 dan 3", "Menunggu", StageState.WAIT),
        StageUi(3, "Tahap 3: Faskes dan polisi", "Opsional (opt-in)", StageState.WAIT),
        StageUi(4, "Tahap 4: Panggilan 112", "Opsional (opt-in)", StageState.WAIT)
    )

    fun reset() {
        phase.value = Phase.NONE
        outcome.value = ""
        stages.value = initial()
    }

    fun update(stage: Int, f: (StageUi) -> StageUi) {
        stages.value = stages.value.map { if (it.stage == stage) f(it) else it }
    }
}
