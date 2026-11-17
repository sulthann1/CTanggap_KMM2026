package id.ctanggap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.ctanggap.app.data.AppDatabase
import id.ctanggap.app.data.Contact
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val share = SharingStarted.WhileSubscribed(5_000)

    companion object { const val MAX_CONTACTS = 5 }

    val contacts = dao.contacts().stateIn(viewModelScope, share, emptyList())
    val incidents = dao.incidents().stateIn(viewModelScope, share, emptyList())
    val trips = dao.trips().stateIn(viewModelScope, share, emptyList())

    fun incident(id: Long) = dao.incident(id)
    fun logsFor(id: Long) = dao.logsFor(id)

    /** Mengembalikan true jika kontak tersimpan. */
    fun addContact(name: String, phone: String, priority: Int): Boolean {
        val digits = phone.count { it.isDigit() }
        if (name.isBlank() || digits < 8 || contacts.value.size >= MAX_CONTACTS) return false
        viewModelScope.launch { dao.insert(Contact(name = name.trim(), phone = phone.trim(), priority = priority)) }
        return true
    }
    fun deleteContact(c: Contact) = viewModelScope.launch { dao.delete(c) }
}
