package io.github.yiannosparanomos.undertone.core.system

import androidx.datastore.core.Serializer
import io.github.yiannosparanomos.undertone.core.data.settings.SettingsCodec
import io.github.yiannosparanomos.undertone.core.data.settings.StoredStateDto
import java.io.InputStream
import java.io.OutputStream

/**
 * A serializer for tests in this module.
 *
 * `core:data`'s own serializer is internal to that module on purpose — nothing outside it should
 * be constructing settings stores in production. Tests build one on the same codec, so the bytes
 * are identical to what the app writes.
 */
object StoredStateSerializerTestAccess {
    val serializer: Serializer<StoredStateDto> =
        object : Serializer<StoredStateDto> {
            override val defaultValue = StoredStateDto()

            override suspend fun readFrom(input: InputStream): StoredStateDto {
                val text = input.readBytes().decodeToString()
                return if (text.isBlank()) defaultValue else SettingsCodec.decode(text)
            }

            override suspend fun writeTo(
                t: StoredStateDto,
                output: OutputStream,
            ) = output.write(SettingsCodec.encode(t).encodeToByteArray())
        }
}
