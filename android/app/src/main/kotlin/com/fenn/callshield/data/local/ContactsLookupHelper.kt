package com.fenn.callshield.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * O(1) contact lookup using an in-memory HashSet populated at startup.
 *
 * Call [initialize] from [CallShieldApp.onCreate] and again from [PermissionsViewModel.refresh]
 * after READ_CONTACTS is granted. The set is rebuilt automatically whenever contacts change
 * via a [ContentObserver] (only registered once READ_CONTACTS is held).
 *
 * Returns `false` for all numbers if READ_CONTACTS is not granted — safe default.
 * Only reads — never writes, never uploads.
 */
@Singleton
class ContactsLookupHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val contactNumbers: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var observerRegistered = false

    /**
     * Attempt to populate the contact set and register a [ContentObserver].
     * Safe to call multiple times — the observer is registered only once.
     * No-op if READ_CONTACTS is not yet granted; call again after permission is obtained.
     */
    fun initialize() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        rebuildSet() // safe — catches SecurityException internally

        if (hasPermission && !observerRegistered) {
            try {
                context.contentResolver.registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI,
                    true,
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) = rebuildSet()
                    },
                )
                observerRegistered = true
            } catch (_: SecurityException) {
                // Permission check passed but registration still denied (edge case) — safe to ignore
            }
        }
    }

    private fun rebuildSet() {
        scope.launch {
            val fresh = mutableSetOf<String>()
            try {
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER),
                    null, null, null,
                )
                cursor?.use {
                    val col = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
                    if (col == -1) return@launch
                    while (it.moveToNext()) {
                        val number = it.getString(col)
                        if (!number.isNullOrBlank()) fresh.add(number)
                    }
                }
            } catch (_: SecurityException) {
                // READ_CONTACTS not granted — leave set empty
                return@launch
            }
            contactNumbers.clear()
            contactNumbers.addAll(fresh)
        }
    }

    fun isInContacts(e164Number: String): Boolean = contactNumbers.contains(e164Number)
}
