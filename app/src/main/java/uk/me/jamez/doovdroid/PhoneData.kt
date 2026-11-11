package uk.me.jamez.doovdroid

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Telephony
import android.util.Log

/** Reads from the phone's own contacts, call log and SMS stores. Call off the main thread. */
object PhoneData {
    private const val TAG = "Doovdroid"

    /**
     * The number to call for a contact name. The same person is often stored several times
     * (SIM, phone, WhatsApp); prefer the default number, then a mobile, then any.
     */
    fun numberFor(ctx: Context, name: String): String? {
        val projection = arrayOf(Phone.NUMBER, Phone.IS_SUPER_PRIMARY, Phone.IS_PRIMARY, Phone.TYPE)
        return try {
            ctx.contentResolver.query(Phone.CONTENT_URI, projection, "${Phone.DISPLAY_NAME} = ?", arrayOf(name), null)?.use { c ->
                var best: String? = null
                var bestScore = -1
                while (c.moveToNext()) {
                    val number = c.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val score = (if (c.getInt(1) != 0) 4 else 0) +
                        (if (c.getInt(2) != 0) 2 else 0) +
                        (if (c.getInt(3) == Phone.TYPE_MOBILE) 1 else 0)
                    if (score > bestScore) {
                        best = number
                        bestScore = score
                    }
                }
                best
            }
        } catch (e: Exception) {
            Log.w(TAG, "Contact lookup failed", e)
            null
        }
    }

    fun starredNames(ctx: Context): List<String> = try {
        ctx.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            "${ContactsContract.Contacts.STARRED} = 1", null,
            "${ContactsContract.Contacts.DISPLAY_NAME} COLLATE LOCALIZED ASC",
        )?.use { c -> buildList { while (c.moveToNext()) c.getString(0)?.let(::add) } }.orEmpty()
            .distinct().take(Config.MAX_SPEED_DIAL)
    } catch (e: Exception) {
        Log.w(TAG, "Starred lookup failed", e)
        emptyList()
    }

    /** Missed calls nobody has looked at yet (the dialler clears NEW when the call log is opened). */
    fun missedCalls(ctx: Context): Int =
        count(ctx, CallLog.Calls.CONTENT_URI, "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} AND ${CallLog.Calls.NEW} = 1")

    fun unreadTexts(ctx: Context): Int =
        count(ctx, Telephony.Sms.Inbox.CONTENT_URI, "${Telephony.Sms.READ} = 0") +
            count(ctx, Telephony.Mms.Inbox.CONTENT_URI, "${Telephony.Mms.READ} = 0")

    private fun count(ctx: Context, uri: Uri, selection: String): Int = try {
        ctx.contentResolver.query(uri, arrayOf(BaseColumns._ID), selection, null, null)?.use { it.count } ?: 0
    } catch (e: Exception) {
        Log.w(TAG, "Count failed for $uri", e)
        0
    }
}
