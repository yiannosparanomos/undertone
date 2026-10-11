package io.github.yiannosparanomos.undertone.core.system.notification

import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.settings.SoundsDto
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.data.settings.profileOrDefault
import io.github.yiannosparanomos.undertone.core.data.settings.toDomain
import io.github.yiannosparanomos.undertone.core.domain.engine.DecisionReason
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationDecision
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationEngine
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationOutcome
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.system.calendar.CalendarEventSource
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime

/** Plays a Ring app's sound. Implemented by `SoundPlayer`; an interface so this is testable. */
fun interface Ringer {
    fun ring(
        packageName: String,
        sounds: SoundsDto,
    )
}

/**
 * One notification in, one decision and one action out.
 *
 * This is where the engine meets the phone: it assembles the [RuleState], asks
 * [NotificationEngine], records what happened and says what the listener should do. It holds no
 * Android types, so every path through it is unit-testable.
 *
 * Notification content passes through here and goes into the encrypted database. It is never
 * logged (invariant 2) — not even at debug level, which is why there is no logging in this file
 * at all.
 */
class NotificationPipeline(
    private val settings: UndertoneSettingsStore,
    private val history: HistoryStore,
    private val calendar: CalendarEventSource = CalendarEventSource.None,
    private val ringer: Ringer,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    /** The engine's view of the world at [at]. */
    suspend fun ruleState(at: LocalDateTime): RuleState {
        val stored = settings.current()
        return RuleState(
            profile = stored.profileOrDefault(),
            settings = stored.settings.toDomain(),
            events = calendar.eventsAround(at),
            override = stored.override?.toDomain()?.takeIf { it.isActiveAt(at) },
        )
    }

    /**
     * Judges one posted notification and records the outcome.
     *
     * [isOngoing] notifications — a music player, a navigation session — are left alone whatever
     * the rules say: holding one would break the app it belongs to, and it makes no sound anyway.
     */
    suspend fun onPosted(
        packageName: String,
        notificationKey: String,
        title: String?,
        text: String?,
        postedAt: LocalDateTime = now(),
        isOngoing: Boolean = false,
    ): Outcome {
        val state = ruleState(postedAt)
        if (isOngoing) {
            return Outcome(
                NotificationDecision(NotificationOutcome.PASS_THROUGH, state.passAsideReason()),
                NotificationAction.PassThrough,
            )
        }

        val decision = NotificationEngine.decide(packageName, postedAt, state)

        history.record(
            packageName = packageName,
            notificationKey = notificationKey,
            title = title,
            text = text,
            postedAt = postedAt,
            decision = decision,
        )

        val action =
            when (decision.outcome) {
                NotificationOutcome.RING -> {
                    ringer.ring(packageName, settings.sounds.first())
                    NotificationAction.Leave
                }
                NotificationOutcome.SILENT -> NotificationAction.Leave
                NotificationOutcome.BLOCKED -> NotificationAction.Cancel
                NotificationOutcome.PASS_THROUGH -> NotificationAction.PassThrough
                NotificationOutcome.HIDDEN -> NotificationAction.Snooze(snoozeFor(postedAt, decision))
            }
        return Outcome(decision, action)
    }

    /**
     * How long to snooze a hidden notification.
     *
     * The engine's `releaseAt` is the moment the app stops being Hidden. When it finds none
     * within its week-long lookahead, the notification is snoozed for the retention period
     * instead: it will be swept by then anyway, and an indefinite snooze would strand it in the
     * system with no record of why.
     */
    private suspend fun snoozeFor(
        at: LocalDateTime,
        decision: NotificationDecision,
    ): Long {
        val releaseAt = decision.releaseAt
        return if (releaseAt != null) {
            Duration.between(at, releaseAt).toMillis().coerceAtLeast(MIN_SNOOZE_MS)
        } else {
            Duration.ofDays(settings.retentionDays.first().toLong()).toMillis()
        }
    }

    /**
     * The user swiped a notification away, or opened it. Feeds the block suggestions (spec §2)
     * and keeps the held inbox honest when an app withdraws something it was holding.
     */
    suspend fun onRemoved(
        packageName: String,
        notificationKey: String,
        reason: RemovalReason,
        at: LocalDateTime = now(),
    ) {
        when (reason) {
            RemovalReason.SWIPED_AWAY -> history.recordSwipedAway(packageName, at)
            RemovalReason.OPENED -> history.recordOpened(packageName, at)
            RemovalReason.OTHER -> Unit
        }
        // Whatever the reason, a notification that is gone should not still be in the inbox.
        history.releaseHeld(notificationKey)
    }

    /** Why a notification left the shade, in the terms the suggestions care about. */
    enum class RemovalReason {
        SWIPED_AWAY,
        OPENED,
        OTHER,
    }

    data class Outcome(
        val decision: NotificationDecision,
        val action: NotificationAction,
    )

    private fun RuleState.passAsideReason() =
        when (profile) {
            Profile.ALLOW_ALL -> DecisionReason.ALLOW_ALL_PROFILE
            Profile.SYSTEM_DND -> DecisionReason.SYSTEM_DND_PROFILE
            Profile.MY_RULES -> DecisionReason.ALL_APPS_DEFAULT
        }

    private companion object {
        /** A snooze of zero would post straight back; give the system a moment. */
        const val MIN_SNOOZE_MS = 1_000L
    }
}
