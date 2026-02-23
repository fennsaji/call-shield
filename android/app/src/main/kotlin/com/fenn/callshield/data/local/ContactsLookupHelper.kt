package com.fenn.callshield.data.local

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks up whether a given E.164-normalized number exists in the device contacts.
 * Only reads — never writes, never uploads.
 * Returns false if READ_CONTACTS permission is not granted (fail-safe).
 */
@Singleton
class ContactsLookupHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isInContacts(e164Number: String): Boolean {
        return try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(e164Number)
                .build()
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null,
            )
            cursor?.use { it.count > 0 } ?: false
        } catch (_: SecurityException) {
            // READ_CONTACTS not granted — treat as not in contacts
            false
        }
    }
}
