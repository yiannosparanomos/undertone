package io.github.yiannosparanomos.undertone.core.data.db

import androidx.test.platform.app.InstrumentationRegistry
import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.domain.engine.DecisionReason
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationDecision
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

/**
 * The invariant that matters most about this module: what lands on disk is unreadable without
 * the Keystore key. These checks look at the raw file, not at Room.
 */
class EncryptionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearDatabaseFiles() {
        context
            .getDatabasePath(UndertoneDatabase.FILE_NAME)
            .parentFile
            ?.listFiles()
            ?.forEach { it.delete() }
        File(context.filesDir, "undertone-db.key").delete()
    }

    @Test
    fun theDatabaseFileIsNotPlaintextSqlite() =
        runTest {
            val database = openFresh()
            val store = HistoryStore.from(database)
            // A title and a body a person would recognise if it leaked.
            store.record(
                packageName = "com.example.chat",
                notificationKey = "k1",
                title = "Mum",
                text = "Are you coming for dinner on Sunday?",
                postedAt = LocalDateTime.of(2026, 10, 12, 18, 0),
                decision = NotificationDecision(NotificationOutcome.SILENT, DecisionReason.APP_SCHEDULE),
            )
            database.close()

            val file = context.getDatabasePath(UndertoneDatabase.FILE_NAME)
            assertTrue("the database should exist on disk", file.exists())

            val bytes = file.readBytes()

            // A plaintext SQLite database starts with this magic string; an encrypted one does not.
            val header = bytes.copyOfRange(0, 16).decodeToString()
            assertFalse(
                "the database header must not be plaintext SQLite: $header",
                header.startsWith("SQLite format 3"),
            )

            // And the content itself must not be findable anywhere in the file.
            val raw = bytes.decodeToString()
            listOf("Mum", "dinner on Sunday", "com.example.chat").forEach { secret ->
                assertFalse("\"$secret\" must not appear in the database file", raw.contains(secret))
            }
        }

    @Test
    fun theSameKeyReopensTheDatabaseAcrossRestarts() =
        runTest {
            val first = openFresh()
            HistoryStore.from(first).record(
                packageName = "com.example.chat",
                notificationKey = "k1",
                title = "t",
                text = "b",
                postedAt = LocalDateTime.of(2026, 10, 12, 18, 0),
                decision = NotificationDecision(NotificationOutcome.SILENT, DecisionReason.APP_SCHEDULE),
            )
            first.close()

            // A new process would build the database again and unwrap the same stored passphrase.
            val second = openFresh()
            assertEquals(1, second.logDao().count())
            second.close()
        }

    @Test
    fun aWrongPassphraseCannotOpenIt() =
        runTest {
            val first = openFresh()
            first.logDao().insert(
                LogEntryEntity(
                    packageName = "com.example.chat",
                    title = null,
                    text = null,
                    postedAt = LocalDateTime.of(2026, 10, 12, 18, 0).toString(),
                    outcome = NotificationOutcome.SILENT.name,
                    reason = DecisionReason.APP_SCHEDULE.name,
                ),
            )
            first.close()

            val opened =
                runCatching {
                    val database = openWith("not-the-real-passphrase".toByteArray())
                    database.logDao().count().also { database.close() }
                }
            assertTrue("a wrong passphrase must not open the database", opened.isFailure)
        }

    private fun openFresh(): UndertoneDatabase = openWith(DatabaseKeyTestAccess.passphrase(context))

    private fun openWith(passphrase: ByteArray): UndertoneDatabase {
        System.loadLibrary("sqlcipher")
        return androidx.room.Room
            .databaseBuilder(context, UndertoneDatabase::class.java, UndertoneDatabase.FILE_NAME)
            .openHelperFactory(
                net.zetetic.database.sqlcipher
                    .SupportOpenHelperFactory(passphrase),
            ).build()
    }
}
