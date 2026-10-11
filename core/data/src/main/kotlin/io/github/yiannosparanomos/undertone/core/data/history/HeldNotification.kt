package io.github.yiannosparanomos.undertone.core.data.history

import io.github.yiannosparanomos.undertone.core.domain.engine.DecisionReason
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationOutcome
import java.time.LocalDateTime

/*
 * What the app and the services work with. These are the stored rows in domain terms, so no
 * caller outside core:data needs to know about Room or how a time is written to disk.
 */

/** A notification waiting in the held inbox. */
data class HeldNotification(
    val notificationKey: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: LocalDateTime,
    val releaseAt: LocalDateTime?,
)

/** One line of the log: something that was silenced or held. */
data class LogEntry(
    val id: Long,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: LocalDateTime,
    val outcome: NotificationOutcome,
    val reason: DecisionReason,
    val notificationKey: String?,
)

/** How many notifications one blocked app sent on one day. */
data class BlockedCount(
    val packageName: String,
    val day: java.time.LocalDate,
    val count: Int,
)
