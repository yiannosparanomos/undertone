package io.github.yiannosparanomos.undertone.spike

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.provider.Settings

/**
 * Probe 2: do UnderTone's own sounds play while DND is on, and which volume stream do they follow?
 *
 * Plays the default notification sound under a chosen AudioAttributes usage and reports what the
 * system says about the stream and the current zen mode. On an emulator the API answers are real;
 * whether a speaker actually makes a noise is a phone question.
 */
object SoundProbe {
    fun play(
        context: Context,
        usageName: String,
    ) {
        val usage =
            when (usageName) {
                "alarm" -> AudioAttributes.USAGE_ALARM
                "notification" -> AudioAttributes.USAGE_NOTIFICATION
                "ringtone" -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                "media" -> AudioAttributes.USAGE_MEDIA
                else -> AudioAttributes.USAGE_NOTIFICATION
            }
        val stream =
            when (usage) {
                AudioAttributes.USAGE_ALARM -> AudioManager.STREAM_ALARM
                AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> AudioManager.STREAM_RING
                AudioAttributes.USAGE_MEDIA -> AudioManager.STREAM_MUSIC
                else -> AudioManager.STREAM_NOTIFICATION
            }

        val audio = context.getSystemService(AudioManager::class.java)
        val zen = runCatching { Settings.Global.getInt(context.contentResolver, "zen_mode") }.getOrNull()

        Probe.log(
            "sound-before",
            "usage" to usageName,
            "zenMode" to zen,
            "ringerMode" to audio.ringerMode,
            "streamVolume" to audio.getStreamVolume(stream),
            "streamMax" to audio.getStreamMaxVolume(stream),
            "streamMuted" to audio.isStreamMute(stream),
            "notificationPolicyAccess" to
                context.getSystemService(android.app.NotificationManager::class.java).isNotificationPolicyAccessGranted,
        )

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            player.setDataSource(context, uri)
            player.setOnCompletionListener {
                Probe.log("sound-complete", "usage" to usageName)
                it.release()
            }
            player.prepare()
            player.start()
            Probe.log(
                "sound-started",
                "usage" to usageName,
                "isPlaying" to player.isPlaying,
                "durationMs" to player.duration,
            )
        } catch (e: Exception) {
            Probe.log("sound-error", "usage" to usageName, "error" to e.javaClass.simpleName)
        }
    }
}
