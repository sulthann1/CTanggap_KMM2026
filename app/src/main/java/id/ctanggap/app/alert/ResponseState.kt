package id.ctanggap.app.alert

import kotlinx.coroutines.flow.MutableStateFlow

/** Menyimpan apakah salah satu kontak sudah membalas "YA" (menuju lokasi) pada insiden berjalan. */
object ResponseState {
    val responded = MutableStateFlow(false)
    fun reset() { responded.value = false }
}
