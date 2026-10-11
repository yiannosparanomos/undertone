package io.github.yiannosparanomos.undertone.core.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream

/**
 * Stores the user's rules as the same JSON that settings export produces.
 *
 * JSON rather than Protobuf because this file is the export format too: a user who exports
 * their settings should get something they can read, check and keep (spec §9). It holds rules
 * only, never notification content, so it does not need the encryption the Room database has
 * (ADR 0007).
 */
internal object StoredStateSerializer : Serializer<StoredStateDto> {
    override val defaultValue = StoredStateDto()

    override suspend fun readFrom(input: InputStream): StoredStateDto {
        val text = input.readBytes().decodeToString()
        if (text.isBlank()) return defaultValue
        return try {
            SettingsCodec.decode(text)
        } catch (e: StoredStateException) {
            // DataStore turns this into the corruption handler's business (see UndertoneSettingsStore).
            throw CorruptionException(e.message ?: "The settings file could not be read.", e)
        }
    }

    override suspend fun writeTo(
        t: StoredStateDto,
        output: OutputStream,
    ) {
        output.write(SettingsCodec.encode(t).encodeToByteArray())
    }
}
