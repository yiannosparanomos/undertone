package io.github.yiannosparanomos.undertone.spike

import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Probe 1: is call screening consulted for a caller who IS in contacts, and does setSilenceCall
 * actually silence the ringer?
 *
 * Behaviour is switched by a flag the adb receiver writes, so one install can test both the
 * silence path and the plain-allow path.
 */
class ProbeCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val handle: Uri? = callDetails.handle
        val number = handle?.schemeSpecificPart
        val inContacts = number?.let { lookupInContacts(it) }
        val silence = ProbeState.silenceCalls(this)

        // Never log the number itself; its presence and length are enough to read the result.
        Probe.log(
            "call-screening",
            "fired" to true,
            "sdk" to android.os.Build.VERSION.SDK_INT,
            "hasHandle" to (handle != null),
            "handleScheme" to (handle?.scheme ?: "none"),
            "inContacts" to inContacts,
            "direction" to callDetails.callDirection,
            "willSilence" to silence,
        )

        val response =
            CallResponse
                .Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .setSilenceCall(silence)
                .build()

        respondToCall(callDetails, response)
        Probe.log("call-screening", "responded" to true, "silenceCall" to silence)
    }

    private fun lookupInContacts(number: String): Boolean? =
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use {
                it.count > 0
            }
        } catch (e: SecurityException) {
            Probe.log("call-screening", "contactsLookup" to "denied", "error" to e.javaClass.simpleName)
            null
        }
}
