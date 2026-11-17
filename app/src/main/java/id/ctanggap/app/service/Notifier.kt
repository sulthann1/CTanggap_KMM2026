package id.ctanggap.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import id.ctanggap.app.ui.AlertActivity

object Notifier {
    const val CH_MONITOR = "monitor"
    const val CH_ALERT = "alert"
    const val ID_MONITOR = 1
    const val ID_ALERT = 2

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_MONITOR, "Pemantauan berkendara", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Peringatan darurat", NotificationManager.IMPORTANCE_HIGH).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
    }

    fun monitoring(ctx: Context): Notification {
        val stop = PendingIntent.getService(
            ctx, 0, Intent(ctx, DrivingService::class.java).setAction(DrivingService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(ctx, CH_MONITOR)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("C-Tanggap aktif")
            .setContentText("Memantau indikasi benturan selama berkendara")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Berhenti", stop).build())
            .build()
    }

    private fun alertIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, AlertActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(ctx, 1, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Layar hitungan mundur: muncul penuh di atas layar kunci. */
    fun showCountdown(ctx: Context) {
        val pi = alertIntent(ctx)
        val n = Notification.Builder(ctx, CH_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Terdeteksi indikasi benturan")
            .setContentText("Ketuk jika Anda baik-baik saja")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(ID_ALERT, n)
    }

    /** Setelah hitungan mundur selesai: notifikasi berubah menjadi status peringatan. */
    fun showStatus(ctx: Context) {
        val n = Notification.Builder(ctx, CH_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Peringatan terkirim")
            .setContentText("Ketuk untuk melihat status")
            .setContentIntent(alertIntent(ctx))
            .setOngoing(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(ID_ALERT, n)
    }

    fun cancelAlert(ctx: Context) =
        ctx.getSystemService(NotificationManager::class.java).cancel(ID_ALERT)
}
