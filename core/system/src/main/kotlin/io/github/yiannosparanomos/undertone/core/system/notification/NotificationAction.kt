package io.github.yiannosparanomos.undertone.core.system.notification

/**
 * What the listener should do with a notification the engine has just judged.
 *
 * Kept separate from the decision itself so the pipeline stays a pure-ish function that can be
 * tested without a live `NotificationListenerService`: the service's only job is to carry these
 * out.
 */
sealed interface NotificationAction {
    /** Leave it in the shade. Ring and Silent both land here; Ring also plays a sound. */
    data object Leave : NotificationAction

    /** Hold it. [durationMs] is how long until the system should bring it back by itself. */
    data class Snooze(
        val durationMs: Long,
    ) : NotificationAction

    /** Blocked: dismiss it and never bring it back. */
    data object Cancel : NotificationAction

    /** UnderTone is stepping aside (Allow all, System DND). */
    data object PassThrough : NotificationAction
}
