package com.fenn.callshield.data.local

import com.fenn.callshield.data.local.dao.VipContactsDao
import com.fenn.callshield.util.PhoneNumberHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * O(1) VIP contact lookup using an in-memory hash set populated from Room.
 *
 * Call [initialize] from [CallShieldApp.onCreate]. The set is updated automatically
 * whenever the vip_contacts table changes via a Room Flow collector.
 *
 * Stores HMAC-SHA256 hashes only — raw phone numbers are never held in memory.
 */
@Singleton
class VipContactsLookupHelper @Inject constructor(
    private val dao: VipContactsDao,
    private val hasher: PhoneNumberHasher,
) {
    @Volatile
    private var vipHashes: Set<String> = emptySet()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        scope.launch {
            dao.observeAll().collect { entries ->
                vipHashes = entries.map { it.numberHash }.toHashSet()
            }
        }
    }

    fun isVip(e164: String): Boolean {
        val hash = hasher.hash(e164) ?: return false
        return hash in vipHashes
    }
}
