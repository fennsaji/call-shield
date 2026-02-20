package com.fenn.callguard.data.repository

import com.fenn.callguard.data.local.dao.WhitelistDao
import com.fenn.callguard.data.local.entity.WhitelistEntry
import com.fenn.callguard.domain.repository.WhitelistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WhitelistRepositoryImpl @Inject constructor(
    private val dao: WhitelistDao,
) : WhitelistRepository {
    override fun observeAll(): Flow<List<WhitelistEntry>> = dao.observeAll()
    override suspend fun contains(numberHash: String): Boolean = dao.contains(numberHash)
    override suspend fun add(numberHash: String, displayLabel: String) =
        dao.insert(WhitelistEntry(numberHash = numberHash, displayLabel = displayLabel))
    override suspend fun remove(numberHash: String) = dao.deleteByHash(numberHash)
}
