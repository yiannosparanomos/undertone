package io.github.yiannosparanomos.undertone.core.data.settings

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Reads and writes [StoredStateDto] as JSON.
 *
 * One codec serves both DataStore and settings export, so what the user exports is exactly what
 * the app stores. Unknown keys are ignored on purpose: a file written by a newer version should
 * still load what this version understands rather than refusing outright.
 */
object SettingsCodec {
    private val json =
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun encode(state: StoredStateDto): String = json.encodeToString(state)

    /**
     * @throws StoredStateException if the text is not valid JSON, is from a schema version this
     *   build cannot read, or holds a value no enum or time format accepts.
     */
    fun decode(text: String): StoredStateDto {
        val dto =
            try {
                json.decodeFromString<StoredStateDto>(text)
            } catch (e: SerializationException) {
                throw StoredStateException("This file is not an UnderTone settings file.", e)
            } catch (e: IllegalArgumentException) {
                throw StoredStateException("This file is not an UnderTone settings file.", e)
            }
        if (dto.version > StoredStateDto.SCHEMA_VERSION) {
            throw StoredStateException(
                "These settings were saved by a newer version of UnderTone " +
                    "(format ${dto.version}, this build reads ${StoredStateDto.SCHEMA_VERSION}).",
            )
        }
        if (dto.retentionDays < 1) {
            throw StoredStateException("retentionDays: must be at least 1, was ${dto.retentionDays}.")
        }
        if (dto.sounds.volumePercent !in 0..100) {
            throw StoredStateException("volumePercent: must be between 0 and 100, was ${dto.sounds.volumePercent}.")
        }
        // Convert once up front so a bad enum or time is an import error, not a crash later on.
        dto.validate()
        return dto
    }

    /** Runs every DTO -> domain conversion for its errors, discarding the result. */
    private fun StoredStateDto.validate() {
        profileOrDefault()
        settings.toDomain()
        override?.toDomain()
    }
}
