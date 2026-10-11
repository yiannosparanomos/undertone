package io.github.yiannosparanomos.undertone.core.system.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import io.github.yiannosparanomos.undertone.core.data.settings.SoundsDto

/**
 * Plays a Ring app's sound while DND is on.
 *
 * The system will not do it: UnderTone holds DND, so it owns the sound too (spec §3). From the
 * M1 spike (ADR 0006): `USAGE_NOTIFICATION` is the right usage, which follows
 * `STREAM_NOTIFICATION` — the stream the per-app volume setting adjusts — and the ringer mode
 * must not be consulted, because under a `PRIORITY` filter it reads as silent while the stream
 * is perfectly audible.
 */
class SoundPlayer(
    private val context: Context,
    private val audioManager: AudioManager,
    private val vibrator: Vibrator?,
) {
    /**
     * Plays the sound configured for [packageName], falling back to the default Ring sound and
     * then to the system notification sound.
     *
     * Fire and forget: the player releases itself when the sound finishes. A failure is logged as
     * a class name and swallowed — a sound that will not play must never take the listener down
     * with it, because that would silence the phone instead of just one notification.
     */
    fun play(
        packageName: String,
        sounds: SoundsDto,
    ) {
        val perApp = sounds.forApp(packageName)
        val uri =
            perApp?.soundUri?.let(Uri::parse)
                ?: sounds.defaultSoundUri?.let(Uri::parse)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (sounds.volumePercent > 0 && uri != null) {
            playSound(uri, sounds.volumePercent)
        }
        if (perApp?.vibrate ?: sounds.vibrate) {
            vibrate()
        }
    }

    private fun playSound(
        uri: Uri,
        volumePercent: Int,
    ) {
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                // On top of the user's notification stream volume, not instead of it.
                setVolume(volumePercent / 100f, volumePercent / 100f)
                setOnCompletionListener { it.release() }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    true
                }
                prepare()
                start()
            }
        }.onFailure { Log.w(TAG, "Could not play a Ring sound: ${it.javaClass.simpleName}") }
    }

    private fun vibrate() {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        runCatching {
            device.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        }.onFailure { Log.w(TAG, "Could not vibrate: ${it.javaClass.simpleName}") }
    }

    /**
     * Whether a sound would actually be heard right now.
     *
     * Decided from the stream, never the ringer mode (ADR 0006). The Health check screen uses
     * this to tell the user their Ring apps are silent because the notification volume is down.
     */
    fun wouldBeAudible(): Boolean =
        !audioManager.isStreamMute(AudioManager.STREAM_NOTIFICATION) &&
            audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) > 0

    companion object {
        private const val TAG = "UndertoneSound"
        private const val VIBRATION_MS = 250L

        fun from(context: Context): SoundPlayer =
            SoundPlayer(
                context = context,
                audioManager = context.getSystemService(AudioManager::class.java),
                vibrator =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Vibrator::class.java)
                    },
            )
    }
}
