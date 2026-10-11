package io.github.yiannosparanomos.undertone.core.data.db

import android.content.Context

/** Reaches the internal key handling from instrumented tests in the same module. */
internal object DatabaseKeyTestAccess {
    fun passphrase(context: Context): ByteArray = DatabaseKey.passphrase(context)
}
