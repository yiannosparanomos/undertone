package io.github.yiannosparanomos.undertone.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Rows of the encrypted database.
 *
 * Everything in here that could identify a person or repeat what they were told -- notification
 * titles and text, phone numbers -- lives ONLY in this database, which is encrypted at rest with
 * a key held in the Android Keystore (ADR 0007). None of it may be written to Logcat, the
 * diagnostic log, a file or an export (invariant 2).
 *
 * Times are stored as ISO-8601 local date-times ("2026-10-12T18:30:05"). The engine works in
 * local time with no zone, and ISO text sorts in the same order as the instants it describes, so
 * range queries and expiry work directly on the stored string.
 */

/** One entry in the log: everything silenced or held. Spec §3. */
@Entity(
    tableName = "log_entries",
    indices = [Index("postedAt"), Index("packageName")],
)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    /** Notification content. Never leaves this table. */
    val title: String?,
    val text: String?,
    val postedAt: String,
    /** `NotificationOutcome` name: SILENT, HIDDEN. Blocked is counted, not logged (spec §2). */
    val outcome: String,
    /** `DecisionReason` name, so the log can say why. */
    val reason: String,
    /** The listener's key for the notification, to tie a log row to a held one. */
    val notificationKey: String? = null,
)

/** A Hidden notification waiting to be released. Spec §3. */
@Entity(
    tableName = "held_notifications",
    indices = [Index("releaseAt"), Index("packageName")],
)
data class HeldNotificationEntity(
    /** The listener's notification key: one row per held notification. */
    @PrimaryKey val notificationKey: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: String,
    /** When the app's level stops being Hidden. Null when nothing in the next week releases it. */
    val releaseAt: String?,
)

/**
 * A recent incoming call, for the repeat-caller rule.
 *
 * The number is stored because the rule has to recognise the same caller ringing again; it is
 * content and stays in this table only.
 */
@Entity(
    tableName = "recent_calls",
    indices = [Index("at"), Index("number")],
)
data class RecentCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val at: String,
    /** `CallOutcome` name: RING or SILENT. */
    val outcome: String,
    /** The `CallTier` that applied, when UnderTone decided. */
    val tier: String?,
)

/**
 * How many notifications a blocked app sent on one day.
 *
 * Blocked notifications are counted and nothing else is kept (spec §2), which is why this is a
 * count per app per day rather than a row per notification.
 */
@Entity(tableName = "blocked_counts", primaryKeys = ["packageName", "day"])
data class BlockedCountEntity(
    val packageName: String,
    /** ISO-8601 local date, "2026-10-12". */
    val day: String,
    val count: Int,
)

/**
 * How a user treats one app's notifications: how many they swiped away without opening, and how
 * many they opened.
 *
 * This is what the block suggestions are built on — "you never open these" (spec §2). Counts per
 * app per day, so there is no per-notification row and nothing to leak.
 */
@Entity(tableName = "dismissal_counts", primaryKeys = ["packageName", "day"])
data class DismissalCountEntity(
    val packageName: String,
    /** ISO-8601 local date, "2026-10-12". */
    val day: String,
    val swipedAway: Int = 0,
    val opened: Int = 0,
)
