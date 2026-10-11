package io.github.yiannosparanomos.undertone.core.data.history

import io.github.yiannosparanomos.undertone.core.data.db.BlockedCountDao
import io.github.yiannosparanomos.undertone.core.data.db.BlockedCountEntity
import io.github.yiannosparanomos.undertone.core.data.db.DismissalDao
import io.github.yiannosparanomos.undertone.core.data.db.HeldDao
import io.github.yiannosparanomos.undertone.core.data.db.HeldNotificationEntity
import io.github.yiannosparanomos.undertone.core.data.db.LogDao
import io.github.yiannosparanomos.undertone.core.data.db.LogEntryEntity
import io.github.yiannosparanomos.undertone.core.data.db.RecentCallDao
import io.github.yiannosparanomos.undertone.core.data.db.RecentCallEntity
import io.github.yiannosparanomos.undertone.core.data.db.UndertoneDatabase
import io.github.yiannosparanomos.undertone.core.domain.engine.CallDecision
import io.github.yiannosparanomos.undertone.core.domain.engine.DecisionReason
import io.github.yiannosparanomos.undertone.core.domain.engine.IncomingCall
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationDecision
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationOutcome
import io.github.yiannosparanomos.undertone.core.domain.engine.PastCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Everything written after a decision: the log, the held inbox, recent calls and daily blocked
 * counts. One place, so the services don't each decide what to record.
 *
 * Content (titles, text, numbers) is passed in and stored; it is never returned anywhere that
 * leaves the app and never logged (invariant 2).
 */
class HistoryStore(
    private val logDao: LogDao,
    private val heldDao: HeldDao,
    private val recentCallDao: RecentCallDao,
    private val blockedCountDao: BlockedCountDao,
    private val dismissalDao: DismissalDao,
) {
    // --- Notifications ------------------------------------------------------------------------

    /**
     * Records what the engine decided about one notification.
     *
     * - Ring and Pass-through are not recorded at all: the shade already shows them (spec §2).
     * - Silent and Hidden go in the log; Hidden also joins the held inbox.
     * - Blocked is counted for the day and nothing else is kept.
     */
    suspend fun record(
        packageName: String,
        notificationKey: String,
        title: String?,
        text: String?,
        postedAt: LocalDateTime,
        decision: NotificationDecision,
    ) {
        when (decision.outcome) {
            NotificationOutcome.RING, NotificationOutcome.PASS_THROUGH -> Unit

            NotificationOutcome.BLOCKED ->
                blockedCountDao.increment(packageName, postedAt.toLocalDate().toString())

            NotificationOutcome.SILENT ->
                logDao.insert(
                    LogEntryEntity(
                        packageName = packageName,
                        title = title,
                        text = text,
                        postedAt = postedAt.toString(),
                        outcome = decision.outcome.name,
                        reason = decision.reason.name,
                        notificationKey = notificationKey,
                    ),
                )

            NotificationOutcome.HIDDEN -> {
                logDao.insert(
                    LogEntryEntity(
                        packageName = packageName,
                        title = title,
                        text = text,
                        postedAt = postedAt.toString(),
                        outcome = decision.outcome.name,
                        reason = decision.reason.name,
                        notificationKey = notificationKey,
                    ),
                )
                heldDao.upsert(
                    HeldNotificationEntity(
                        notificationKey = notificationKey,
                        packageName = packageName,
                        title = title,
                        text = text,
                        postedAt = postedAt.toString(),
                        releaseAt = decision.releaseAt?.toString(),
                    ),
                )
            }
        }
    }

    val log: Flow<List<LogEntry>> = logDao.recent().map { rows -> rows.map { it.toDomain() } }

    fun logForApp(packageName: String): Flow<List<LogEntry>> =
        logDao.forApp(packageName).map { rows -> rows.map { it.toDomain() } }

    // --- Held inbox ---------------------------------------------------------------------------

    val held: Flow<List<HeldNotification>> = heldDao.all().map { rows -> rows.map { it.toDomain() } }

    /** The count the widget shows. */
    val heldCount: Flow<Int> = heldDao.count()

    /** Held notifications whose release time has arrived. */
    suspend fun heldDueAt(at: LocalDateTime): List<HeldNotification> =
        heldDao.dueAt(at.toString()).map { it.toDomain() }

    /**
     * Drops one held notification — released into the shade, opened by the user, or withdrawn by
     * its own app while it waited (spec §3).
     */
    suspend fun releaseHeld(notificationKey: String): Boolean = heldDao.delete(notificationKey) > 0

    /** Release all, and what switching to Allow all does to the inbox. */
    suspend fun releaseAllHeld() = heldDao.clear()

    // --- Calls --------------------------------------------------------------------------------

    /** Records an incoming call so the repeat-caller rule can see it next time. */
    suspend fun record(
        call: IncomingCall,
        decision: CallDecision,
    ) {
        recentCallDao.insert(
            RecentCallEntity(
                number = call.number,
                at = call.at.toString(),
                outcome = decision.outcome.name,
                tier = decision.tier?.name,
            ),
        )
    }

    /**
     * The calls from [number] that the repeat-caller rule should consider: those inside [window]
     * before [at]. Returned as [PastCall] because that is what `CallEngine.decide` takes.
     */
    suspend fun recentCallsFrom(
        number: String,
        at: LocalDateTime,
        window: Duration,
    ): List<PastCall> =
        recentCallDao
            .from(number, at.minus(window).toString())
            .map { PastCall(number = it.number, at = LocalDateTime.parse(it.at)) }

    // --- Blocked counts -----------------------------------------------------------------------

    fun blockedCountsOn(day: LocalDate): Flow<List<BlockedCount>> =
        blockedCountDao.forDay(day.toString()).map { rows ->
            rows.map { BlockedCount(it.packageName, LocalDate.parse(it.day), it.count) }
        }

    fun blockedTotalOn(day: LocalDate): Flow<Int> = blockedCountDao.totalForDay(day.toString())

    /** Used by the block suggestions: how noisy an app has been lately (spec §2). */
    suspend fun blockedTotalForApp(
        packageName: String,
        since: LocalDate,
    ): Int = blockedCountDao.totalForApp(packageName, since.toString())

    // --- Swipes and block suggestions ---------------------------------------------------------

    /** The user swiped a notification away without opening it. */
    suspend fun recordSwipedAway(
        packageName: String,
        at: LocalDateTime,
    ) = dismissalDao.recordSwipedAway(packageName, at.toLocalDate().toString())

    /** The user opened a notification. */
    suspend fun recordOpened(
        packageName: String,
        at: LocalDateTime,
    ) = dismissalDao.recordOpened(packageName, at.toLocalDate().toString())

    /**
     * Apps worth suggesting Silent or Block for: swiped away at least [threshold] times since
     * [since] and never opened (spec §2 suggests 10 in a week).
     *
     * "Never opened" rather than "rarely opened" on purpose — a suggestion that fires on an app
     * the user does sometimes read would train them to dismiss suggestions.
     */
    suspend fun blockSuggestions(
        since: LocalDate,
        threshold: Int = DEFAULT_SUGGESTION_THRESHOLD,
    ): List<BlockSuggestion> =
        dismissalDao
            .totalsSince(since.toString())
            .filter { it.opened == 0 && it.swipedAway >= threshold }
            .sortedByDescending { it.swipedAway }
            .map { BlockSuggestion(it.packageName, it.swipedAway) }

    /** An app the user keeps swiping away and never opens. */
    data class BlockSuggestion(
        val packageName: String,
        val swipedAway: Int,
    )

    // --- Retention ----------------------------------------------------------------------------

    /**
     * Deletes everything older than [retentionDays] before [now]. Spec §3: log entries delete
     * themselves after 7 days, and the period is adjustable.
     *
     * Held notifications are swept by when they arrived, not by their release time: one that has
     * waited longer than the retention period is never going to be released.
     */
    suspend fun deleteExpired(
        now: LocalDateTime,
        retentionDays: Int,
    ): Expired {
        require(retentionDays >= 1) { "Retention must be at least one day" }
        val cutoff = now.minusDays(retentionDays.toLong())
        return Expired(
            logEntries = logDao.deleteOlderThan(cutoff.toString()),
            heldNotifications = heldDao.deleteOlderThan(cutoff.toString()),
            recentCalls = recentCallDao.deleteOlderThan(cutoff.toString()),
            blockedCountDays = blockedCountDao.deleteOlderThan(cutoff.toLocalDate().toString()),
            dismissalDays = dismissalDao.deleteOlderThan(cutoff.toLocalDate().toString()),
        )
    }

    /** How many rows the last sweep removed. Counts only — safe to show or count, never content. */
    data class Expired(
        val logEntries: Int,
        val heldNotifications: Int,
        val recentCalls: Int,
        val blockedCountDays: Int,
        val dismissalDays: Int,
    ) {
        val total: Int
            get() = logEntries + heldNotifications + recentCalls + blockedCountDays + dismissalDays
    }

    /** Wipes every row. Behind "Delete my data" and used by tests. */
    suspend fun deleteEverything() {
        logDao.clear()
        heldDao.clear()
        recentCallDao.clear()
        blockedCountDao.clear()
        dismissalDao.clear()
    }

    companion object {
        /** Spec §2: "e.g. 10 in a week". */
        const val DEFAULT_SUGGESTION_THRESHOLD = 10

        fun from(database: UndertoneDatabase): HistoryStore =
            HistoryStore(
                logDao = database.logDao(),
                heldDao = database.heldDao(),
                recentCallDao = database.recentCallDao(),
                blockedCountDao = database.blockedCountDao(),
                dismissalDao = database.dismissalDao(),
            )
    }
}

private fun LogEntryEntity.toDomain(): LogEntry =
    LogEntry(
        id = id,
        packageName = packageName,
        title = title,
        text = text,
        postedAt = LocalDateTime.parse(postedAt),
        outcome = NotificationOutcome.valueOf(outcome),
        reason = DecisionReason.valueOf(reason),
        notificationKey = notificationKey,
    )

private fun HeldNotificationEntity.toDomain(): HeldNotification =
    HeldNotification(
        notificationKey = notificationKey,
        packageName = packageName,
        title = title,
        text = text,
        postedAt = LocalDateTime.parse(postedAt),
        releaseAt = releaseAt?.let { LocalDateTime.parse(it) },
    )
