package id.ctanggap.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator

/** Alarm suara + getar selama hitungan mundur. */
object AlarmPlayer {
    private var player: MediaPlayer? = null

    fun start(ctx: Context) {
        stop(ctx)
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(ctx, uri)
                isLooping = true
                prepare(); this.start()
            }
        } catch (_: Exception) { }
        @Suppress("DEPRECATION")
        (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
    }

    fun stop(ctx: Context) {
        try { player?.stop(); player?.release() } catch (_: Exception) { }
        player = null
        (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
    }
}
