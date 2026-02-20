package com.fenn.callshield.data.repository

import com.fenn.callshield.data.local.dao.BlocklistDao
import com.fenn.callshield.data.local.entity.BlocklistEntry
import com.fenn.callshield.domain.repository.BlocklistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BlocklistRepositoryImpl @Inject constructor(
    private val dao: BlocklistDao,
) : BlocklistRepository {
    override fun observeAll(): Flow<List<BlocklistEntry>> = dao.observeAll()
    override suspend fun contains(numberHash: String): Boolean = dao.contains(numberHash)
    override suspend fun add(numberHash: String, displayLabel: String) =
        dao.insert(BlocklistEntry(numberHash = numberHash, displayLabel = displayLabel))
    override suspend fun remove(numberHash: String) = dao.deleteByHash(numberHash)
}
