package io.github.yiannosparanomos.undertone.core.domain.engine

import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import java.time.LocalDateTime

/** What happens to one notification. */
enum class NotificationOutcome {
    RING,
    SILENT,
    HIDDEN,
    BLOCKED,

    /** UnderTone steps aside (Allow all or System DND profile). */
    PASS_THROUGH,
    ;

    companion object {
        fun of(level: Level): NotificationOutcome =
            when (level) {
                Level.RING -> RING
                Level.SILENT -> SILENT
                Level.HIDDEN -> HIDDEN
                Level.BLOCKED -> BLOCKED
            }
    }
}

/** What happens to one incoming call. Calls are never rejected, only silenced. */
enum class CallOutcome {
    RING,
    SILENT,

    /** UnderTone steps aside (Allow all or System DND profile). */
    PASS_THROUGH,
}

/** Why a decision was made. Shown on the Home status line and in the log. */
enum class DecisionReason {
    ALLOW_ALL_PROFILE,
    SYSTEM_DND_PROFILE,
    BLOCKED_APP,
    MANUAL_OVERRIDE,
    CALENDAR_RULE,
    APP_SCHEDULE,
    ALL_APPS_DEFAULT,
    RING_TIER,
    REPEAT_CALLER,
    TIER_SILENCED,
}

data class NotificationDecision(
    val outcome: NotificationOutcome,
    val reason: DecisionReason,
    /** For [NotificationOutcome.HIDDEN]: when the notification should come back. Null if never (within a week). */
    val releaseAt: LocalDateTime? = null,
)

data class CallDecision(
    val outcome: CallOutcome,
    val reason: DecisionReason,
    /** The ring tier that applied, or null when UnderTone stepped aside. */
    val tier: CallTier? = null,
)
