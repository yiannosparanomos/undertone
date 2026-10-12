package io.github.yiannosparanomos.undertone.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.domain.model.UndertoneSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime

/**
 * The single place the active profile, the user's rules, the current override and the retention
 * period are read from and written to.
 *
 * Everything is exposed as a [Flow] so the UI, the widget and the platform services all see the
 * same state without one of them having to tell the others. [ruleState] assembles the snapshot
 * the engines take; calendar events are added by the caller that can read them.
 */
class UndertoneSettingsStore(
    private val dataStore: DataStore<StoredStateDto>,
) {
    val state: Flow<StoredStateDto> = dataStore.data

    val profile: Flow<Profile> = state.map { it.profileOrDefault() }

    val settings: Flow<UndertoneSettings> = state.map { it.settings.toDomain() }

    val retentionDays: Flow<Int> = state.map { it.retentionDays }

    /** What Ring apps sound like. Not part of the engine: it decides whether, not what. */
    val sounds: Flow<SoundsDto> = state.map { it.sounds }

    /** Who may ring on the Allow list tier. Resolved against contacts by `core:system`. */
    val allowList: Flow<AllowListDto> = state.map { it.allowList }

    /**
     * The current override, or null when none is set **or it has already run out**. Overrides end
     * by themselves (spec §4), so the stored one is filtered by [at] rather than cleared on a timer.
     */
    fun override(at: LocalDateTime): Flow<Override?> =
        state.map { stored -> stored.override?.toDomain()?.takeIf { it.isActiveAt(at) } }

    /** The engine snapshot, without calendar events: the caller adds those it can read. */
    fun ruleState(at: LocalDateTime): Flow<RuleState> =
        state.map { stored ->
            RuleState(
                profile = stored.profileOrDefault(),
                settings = stored.settings.toDomain(),
                override = stored.override?.toDomain()?.takeIf { it.isActiveAt(at) },
            )
        }

    suspend fun current(): StoredStateDto = state.first()

    suspend fun setProfile(profile: Profile) {
        dataStore.updateData { it.copy(profile = profile.name) }
    }

    suspend fun setSettings(settings: UndertoneSettings) {
        dataStore.updateData { it.copy(settings = settings.toDto()) }
    }

    suspend fun updateSettings(transform: (UndertoneSettings) -> UndertoneSettings) {
        dataStore.updateData { stored ->
            stored.copy(settings = transform(stored.settings.toDomain()).toDto())
        }
    }

    suspend fun updateSounds(transform: (SoundsDto) -> SoundsDto) {
        val updated = transform(current().sounds)
        require(updated.volumePercent in 0..100) { "Volume must be between 0 and 100" }
        dataStore.updateData { it.copy(sounds = updated) }
    }

    suspend fun updateAllowList(transform: (AllowListDto) -> AllowListDto) {
        dataStore.updateData { it.copy(allowList = transform(it.allowList)) }
    }

    suspend fun setOverride(override: Override?) {
        dataStore.updateData { it.copy(override = override?.toDto()) }
    }

    /** Retention applies to the log and the held inbox. Spec default is 7 days. */
    suspend fun setRetentionDays(days: Int) {
        require(days >= 1) { "Retention must be at least one day" }
        dataStore.updateData { it.copy(retentionDays = days) }
    }

    /**
     * Writes the user's settings to [output] — rules, package names and times only, never
     * notification content (invariant 2). The active override is deliberately left out: it is a
     * momentary state, not a setting worth carrying to another phone.
     */
    suspend fun export(output: OutputStream) {
        val stored = current().copy(override = null)
        output.write(SettingsCodec.encode(stored).encodeToByteArray())
    }

    /**
     * Replaces the stored settings with the contents of [input].
     *
     * @throws StoredStateException if the file is not readable UnderTone settings. The stored
     *   state is left untouched when that happens: the file is parsed in full before anything
     *   is written.
     */
    suspend fun import(input: InputStream) {
        val incoming = SettingsCodec.decode(input.readBytes().decodeToString())
        dataStore.updateData { existing ->
            incoming.copy(
                version = StoredStateDto.SCHEMA_VERSION,
                // An import carries rules, not the moment-to-moment state of this phone.
                override = existing.override,
            )
        }
    }

    companion object {
        private const val FILE_NAME = "undertone-settings.json"

        @Volatile
        private var instance: UndertoneSettingsStore? = null

        /**
         * The process-wide settings store.
         *
         * **One per process, not one per caller.** DataStore allows only a single active
         * instance per file and throws otherwise, and UnderTone has several components in one
         * process that all need settings: the notification listener, the call screening service,
         * the boot receiver and the UI. Creating a second store made call screening throw, which
         * the service caught and answered by letting the call ring — the safe outcome, but the
         * user's tier was ignored. Hence the singleton.
         *
         * A settings file that cannot be read is replaced with the defaults rather than left to
         * crash the app on every start. The user is told on the Health check screen, because
         * silently reverting someone's rules would be worse than saying so.
         */
        fun get(context: Context): UndertoneSettingsStore =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): UndertoneSettingsStore =
            UndertoneSettingsStore(
                DataStoreFactory.create(
                    serializer = StoredStateSerializer,
                    corruptionHandler = ReplaceFileCorruptionHandler { StoredStateDto() },
                    produceFile = { File(context.filesDir, FILE_NAME) },
                ),
            )
    }
}
