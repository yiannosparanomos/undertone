package io.github.yiannosparanomos.undertone.core.system.calls

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import io.github.yiannosparanomos.undertone.core.data.settings.AllowListDto

/**
 * Answers the two questions `CallEngine` cannot: is this number a saved contact, and is it on
 * the allow list.
 *
 * The M1 spike confirmed that call screening fires for callers who **are** in contacts
 * (ADR 0006), which is what makes the Contacts tier possible at all.
 *
 * Numbers are never logged. A lookup failure is reported as "not a contact", which is the
 * stricter answer — it can leave a call silent, never make a silenced call ring.
 */
class ContactLookup(
    private val context: Context,
) {
    /** What the engine needs to know about a caller. */
    data class Caller(
        val isContact: Boolean,
        val isStarred: Boolean,
        val lookupKey: String?,
    )

    fun lookup(number: String): Caller {
        if (number.isBlank()) return Caller(isContact = false, isStarred = false, lookupKey = null)
        val uri =
            Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            context.contentResolver
                .query(
                    uri,
                    arrayOf(
                        ContactsContract.PhoneLookup.STARRED,
                        ContactsContract.PhoneLookup.LOOKUP_KEY,
                    ),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) {
                        Caller(isContact = false, isStarred = false, lookupKey = null)
                    } else {
                        Caller(
                            isContact = true,
                            isStarred = cursor.getInt(0) != 0,
                            lookupKey = cursor.getString(1),
                        )
                    }
                } ?: Caller(isContact = false, isStarred = false, lookupKey = null)
        } catch (e: SecurityException) {
            // Contacts permission revoked: the Health check screen reports it. Treating the
            // caller as unknown keeps the quieter outcome.
            Log.w(TAG, "Contacts access denied; treating the caller as unknown")
            Caller(isContact = false, isStarred = false, lookupKey = null)
        } catch (e: Exception) {
            Log.w(TAG, "Contact lookup failed: ${e.javaClass.simpleName}")
            Caller(isContact = false, isStarred = false, lookupKey = null)
        }
    }

    /**
     * Whether [caller] is on the allow list.
     *
     * Starred contacts are always on it — that is the list Android's own DND uses. In
     * [AllowListDto.Mode.EXPANDED] the people and numbers added inside UnderTone count too.
     */
    fun isOnAllowList(
        caller: Caller,
        number: String,
        allowList: AllowListDto,
    ): Boolean {
        if (caller.isStarred) return true
        if (allowList.mode == AllowListDto.Mode.STARRED) return false
        if (caller.lookupKey != null && caller.lookupKey in allowList.contactLookupKeys) return true
        return allowList.containsNumber(number)
    }

    private companion object {
        const val TAG = "UndertoneContacts"
    }
}
