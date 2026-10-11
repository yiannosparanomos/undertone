package io.github.yiannosparanomos.undertone.core.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The passphrase for the encrypted database.
 *
 * A random 32-byte passphrase is generated once, then stored wrapped in AES-GCM by a key that
 * lives in the **Android Keystore** and never leaves it. The wrapped file is useless on its own:
 * copying it to another phone, or pulling it off this one, yields nothing without the hardware
 * key (ADR 0007).
 *
 * Nothing here is ever logged. If the Keystore key disappears — a factory reset of the secure
 * hardware, or a restore onto different hardware — the passphrase cannot be recovered and the
 * database is unreadable. That is the intended outcome: the data is seven days of local history,
 * and losing it is far better than it being readable off the device.
 */
internal object DatabaseKey {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "undertone.db"
    private const val WRAPPED_FILE = "undertone-db.key"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private const val PASSPHRASE_BYTES = 32

    /** The passphrase, creating and wrapping one on first use. */
    fun passphrase(context: Context): ByteArray {
        val file = File(context.filesDir, WRAPPED_FILE)
        return if (file.exists()) unwrap(file) else create(file)
    }

    private fun create(file: File): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val sealed = cipher.doFinal(passphrase)
        // iv || ciphertext
        file.writeBytes(cipher.iv + sealed)
        return passphrase
    }

    private fun unwrap(file: File): ByteArray {
        val bytes = file.readBytes()
        if (bytes.size <= IV_BYTES) {
            throw IllegalStateException("The database key file is truncated.")
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            keystoreKey(),
            GCMParameterSpec(GCM_TAG_BITS, bytes, 0, IV_BYTES),
        )
        return cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES)
    }

    private fun keystoreKey(): SecretKey {
        val keystore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keystore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // No user authentication requirement: the services must read the database while
                // the phone is locked, which is exactly when decisions are being made.
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }
}
