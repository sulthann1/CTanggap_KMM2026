package id.ctanggap.app.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import id.ctanggap.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Mendeteksi balasan "YA" dari kontak darurat agar eskalasi berikutnya dihentikan. */
class SmsReplyReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contacts = AppDatabase.get(ctx).dao().contactsOnce()
                for (m in msgs) {
                    val isYes = m.messageBody?.trim()?.uppercase()?.startsWith("YA") == true
                    val fromContact = contacts.any { PhoneUtil.same(it.phone, m.originatingAddress ?: "") }
                    if (isYes && fromContact) ResponseState.responded.value = true
                }
            } finally { pending.finish() }
        }
    }
}
