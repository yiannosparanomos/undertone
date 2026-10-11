package io.github.yiannosparanomos.undertone.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * The encrypted local database: log, held inbox, recent calls and daily blocked counts.
 *
 * Encrypted with SQLCipher, the passphrase wrapped by an Android Keystore key ([DatabaseKey]).
 * This is the only place notification content and phone numbers are stored (invariant 2).
 */
@Database(
    entities = [
        LogEntryEntity::class,
        HeldNotificationEntity::class,
        RecentCallEntity::class,
        BlockedCountEntity::class,
        DismissalCountEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UndertoneDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    abstract fun heldDao(): HeldDao

    abstract fun recentCallDao(): RecentCallDao

    abstract fun blockedCountDao(): BlockedCountDao

    abstract fun dismissalDao(): DismissalDao

    companion object {
        const val FILE_NAME = "undertone.db"

        @Volatile
        private var instance: UndertoneDatabase? = null

        fun get(context: Context): UndertoneDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): UndertoneDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(DatabaseKey.passphrase(context))
            return Room
                .databaseBuilder(context, UndertoneDatabase::class.java, FILE_NAME)
                .openHelperFactory(factory)
                // No fallbackToDestructiveMigration: losing the user's held notifications on an
                // upgrade would be a silent failure. Migrations are written by hand and the
                // schema JSON under core/data/schemas is checked in so they can be reviewed.
                .build()
        }
    }
}
