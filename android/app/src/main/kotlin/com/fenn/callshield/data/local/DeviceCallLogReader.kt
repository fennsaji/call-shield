package com.fenn.callshield.data.local

import android.content.Context
import android.provider.CallLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceCallLogEntry(
    /** Raw phone number as stored in the call log. May be empty for hidden numbers. */
    val number: String,
    /** Contact name if available, otherwise null. */
    val name: String?,
    /** One of [CallLog.Calls.INCOMING_TYPE], [CallLog.Calls.OUTGOING_TYPE], [CallLog.Calls.MISSED_TYPE], etc. */
    val type: Int,
    /** Call duration in seconds. */
    val duration: Long,
    /** Epoch millis of the call. */
    val timestamp: Long,
)

@Singleton
class DeviceCallLogReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Reads up to [limit] most-recent entries from the device call log.
     * Returns an empty list if READ_CALL_LOG permission has not been granted.
     */
    fun read(limit: Int = 200): List<DeviceCallLogEntry> {
        val entries = mutableListOf<DeviceCallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE,
        )
        runCatching {
            // Note: do NOT append "LIMIT n" to the sortOrder — CallLog ContentProvider on
            // some OEMs rejects or ignores the clause, returning 0 rows. Cap in Kotlin instead.
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC",
            )?.use { cursor ->
                val numberIdx   = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx     = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIdx     = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val dateIdx     = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                while (cursor.moveToNext() && entries.size < limit) {
                    entries += DeviceCallLogEntry(
                        number    = cursor.getString(numberIdx) ?: "",
                        name      = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() },
                        type      = cursor.getInt(typeIdx),
                        duration  = cursor.getLong(durationIdx),
                        timestamp = cursor.getLong(dateIdx),
                    )
                }
            }
        }
        return entries
    }
}
