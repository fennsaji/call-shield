package com.fenn.callshield.data.repository

import com.fenn.callshield.data.local.dao.VipContactsDao
import com.fenn.callshield.data.local.entity.VipContactEntry
import com.fenn.callshield.domain.repository.VipContactsRepository
import com.fenn.callshield.util.PhoneNumberHasher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class VipContactsRepositoryImpl @Inject constructor(
    private val dao: VipContactsDao,
    private val hasher: PhoneNumberHasher,
) : VipContactsRepository {
    override fun observeAll(): Flow<List<VipContactEntry>> = dao.observeAll()

    override suspend fun add(e164: String, displayLabel: String) {
        val hash = hasher.hash(e164) ?: return
        dao.insert(VipContactEntry(numberHash = hash, displayLabel = displayLabel))
    }

    override suspend fun remove(numberHash: String) = dao.deleteByHash(numberHash)

    override suspend fun clear() = dao.deleteAll()
}
