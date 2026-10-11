package io.github.yiannosparanomos.undertone.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntryEntity): Long

    /** Newest first, as the Held and log screen shows it. */
    @Query("SELECT * FROM log_entries ORDER BY postedAt DESC, id DESC LIMIT :limit")
    fun recent(limit: Int = 500): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entries WHERE packageName = :packageName ORDER BY postedAt DESC, id DESC")
    fun forApp(packageName: String): Flow<List<LogEntryEntity>>

    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun count(): Int

    /** Retention: everything older than [cutoff] goes (spec §3, default 7 days). */
    @Query("DELETE FROM log_entries WHERE postedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: String): Int

    @Query("DELETE FROM log_entries")
    suspend fun clear()
}

@Dao
interface HeldDao {
    /** A notification re-posted by its app replaces the held row rather than adding a second. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(held: HeldNotificationEntity)

    @Query("SELECT * FROM held_notifications ORDER BY postedAt DESC")
    fun all(): Flow<List<HeldNotificationEntity>>

    @Query("SELECT COUNT(*) FROM held_notifications")
    fun count(): Flow<Int>

    @Query("SELECT * FROM held_notifications WHERE releaseAt IS NOT NULL AND releaseAt <= :at")
    suspend fun dueAt(at: String): List<HeldNotificationEntity>

    @Query("SELECT * FROM held_notifications WHERE notificationKey = :key")
    suspend fun byKey(key: String): HeldNotificationEntity?

    @Query("DELETE FROM held_notifications WHERE notificationKey = :key")
    suspend fun delete(key: String): Int

    /** Release all, and what Allow all does to the inbox (spec §3). */
    @Query("DELETE FROM held_notifications")
    suspend fun clear()

    @Query("DELETE FROM held_notifications WHERE postedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: String): Int
}

@Dao
interface RecentCallDao {
    @Insert
    suspend fun insert(call: RecentCallEntity): Long

    @Query("SELECT * FROM recent_calls ORDER BY at DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<RecentCallEntity>>

    /**
     * Calls from [number] at or after [since], newest first. This is what the repeat-caller rule
     * counts: "2 calls within 2 minutes" by default.
     */
    @Query("SELECT * FROM recent_calls WHERE number = :number AND at >= :since ORDER BY at DESC")
    suspend fun from(
        number: String,
        since: String,
    ): List<RecentCallEntity>

    @Query("DELETE FROM recent_calls WHERE at < :cutoff")
    suspend fun deleteOlderThan(cutoff: String): Int

    @Query("DELETE FROM recent_calls")
    suspend fun clear()
}

@Dao
interface BlockedCountDao {
    /**
     * One more blocked notification from [packageName] today.
     *
     * Two statements rather than SQLite's UPSERT: `ON CONFLICT ... DO UPDATE` needs SQLite 3.24
     * and Android 10 ships 3.22, so an UPSERT fails outright on minSdk against the platform
     * database. SQLCipher bundles a newer SQLite, which production uses, but relying on that
     * would leave the query broken anywhere the bundled library is not in play.
     */
    @Transaction
    suspend fun increment(
        packageName: String,
        day: String,
    ) {
        insertIfAbsent(BlockedCountEntity(packageName, day, 0))
        bump(packageName, day)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(row: BlockedCountEntity)

    @Query("UPDATE blocked_counts SET count = count + 1 WHERE packageName = :packageName AND day = :day")
    suspend fun bump(
        packageName: String,
        day: String,
    )

    @Query("SELECT * FROM blocked_counts WHERE day = :day ORDER BY count DESC")
    fun forDay(day: String): Flow<List<BlockedCountEntity>>

    @Query("SELECT COALESCE(SUM(count), 0) FROM blocked_counts WHERE day = :day")
    fun totalForDay(day: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(count), 0) FROM blocked_counts WHERE packageName = :packageName AND day >= :since")
    suspend fun totalForApp(
        packageName: String,
        since: String,
    ): Int

    @Query("DELETE FROM blocked_counts WHERE day < :cutoff")
    suspend fun deleteOlderThan(cutoff: String): Int

    @Query("DELETE FROM blocked_counts")
    suspend fun clear()
}
