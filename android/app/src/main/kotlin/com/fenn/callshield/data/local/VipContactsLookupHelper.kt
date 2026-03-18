package com.fenn.callshield.data.local

import com.fenn.callshield.data.local.dao.VipContactsDao
import com.fenn.callshield.util.PhoneNumberHasher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * O(1) VIP contact lookup using an in-memory hash set populated from Room.
 *
 * Call [initialize] from [CallShieldApp.onCreate]. The set is updated automatically
 * whenever the vip_contacts table changes via a Room Flow collector.
 *
 * Stores HMAC-SHA256 hashes only — raw phone numbers are never held in memory.
 *
 * Startup race: [isVip] may be called by [CallShieldScreeningService] before the
 * Room Flow emits its first value. [firstLoadReady] is completed after the first
 * emission; [isVip] blocks (up to 500 ms) waiting for it so that VIP contacts are
 * never incorrectly treated as non-VIP during the startup window.
 */
@Singleton
class VipContactsLookupHelper @Inject constructor(
    private val dao: VipContactsDao,
    private val hasher: PhoneNumberHasher,
) {
    @Volatile
    private var vipHashes: Set<String> = emptySet()

    /** Completed (with Unit) after the first Room emission. */
    private val firstLoadReady = CompletableDeferred<Unit>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        scope.launch {
            dao.observeAll().collect { entries ->
                vipHashes = entries.map { it.numberHash }.toHashSet()
                // Signal that the initial DB load is complete (idempotent on subsequent calls).
                firstLoadReady.complete(Unit)
            }
        }
    }

    fun isVip(e164: String): Boolean {
        val hash = hasher.hash(e164) ?: return false
        // Block for up to 500 ms to ensure the first DB load has completed before
        // making a decision. If the timeout elapses we fall through using whatever
        // is in vipHashes (worst case: empty set on a very slow device at cold start).
        if (!firstLoadReady.isCompleted) {
            runBlocking {
                withTimeoutOrNull(500L) { firstLoadReady.await() }
            }
        }
        return hash in vipHashes
    }
}
