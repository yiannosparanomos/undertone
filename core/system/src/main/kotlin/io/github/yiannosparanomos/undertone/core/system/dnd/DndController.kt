package io.github.yiannosparanomos.undertone.core.system.dnd

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import io.github.yiannosparanomos.undertone.core.domain.model.Profile

/**
 * Owns Do Not Disturb while My rules is active.
 *
 * UnderTone turns DND on so the system stays quiet and plays Ring sounds itself. Two rules come
 * straight out of the M1 spike (ADR 0006) and are not negotiable:
 *
 * 1. **Never `INTERRUPTION_FILTER_NONE`.** It mutes the alarm stream, and alarms ring in every
 *    profile (spec §9). `PRIORITY` is used instead, with a policy that lets alarms through.
 * 2. **Never read the ringer mode** to decide anything. Under `PRIORITY` the system reports
 *    `RINGER_MODE_SILENT` while the notification and ring streams are not muted at all.
 *
 * UnderTone only ever restores DND to the state it found, and only touches it for
 * [Profile.MY_RULES]: under Allow all and System DND the phone is the user's own business.
 */
class DndController(
    private val notificationManager: NotificationManager,
) {
    val hasPolicyAccess: Boolean
        get() = notificationManager.isNotificationPolicyAccessGranted

    val currentFilter: Int
        get() = notificationManager.currentInterruptionFilter

    val isOwnedByUndertone: Boolean
        get() = currentFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY

    /**
     * Applies the DND state [profile] calls for.
     *
     * @return true if the state is now what it should be; false if policy access is missing,
     *   which the watchdog turns into a loud failure rather than a silent one.
     */
    fun apply(profile: Profile): Boolean {
        if (!hasPolicyAccess) {
            Log.w(TAG, "No notification policy access; cannot own DND")
            return false
        }
        return when (profile) {
            Profile.MY_RULES -> turnOn()
            // Both of these mean "step aside": the user's own DND settings apply again.
            Profile.ALLOW_ALL, Profile.SYSTEM_DND -> release()
        }
    }

    private fun turnOn(): Boolean =
        runCatching {
            notificationManager.notificationPolicy =
                NotificationManager.Policy(
                    // Alarms always ring (spec §9), and so do media and system sounds, so the
                    // phone does not go unexpectedly dead quiet.
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                        NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                        NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM,
                    0,
                    0,
                )
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }.onFailure { Log.w(TAG, "Could not turn DND on: ${it.javaClass.simpleName}") }
            .isSuccess

    /** Hands DND back. Called when stepping aside, and by the watchdog when UnderTone fails. */
    fun release(): Boolean =
        runCatching {
            if (isOwnedByUndertone) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }.onFailure { Log.w(TAG, "Could not release DND: ${it.javaClass.simpleName}") }
            .isSuccess

    companion object {
        private const val TAG = "UndertoneDnd"

        fun from(context: Context): DndController =
            DndController(context.getSystemService(NotificationManager::class.java))
    }
}
