package com.fenn.callshield.domain.repository

import com.fenn.callshield.data.local.entity.BlocklistEntry
import kotlinx.coroutines.flow.Flow

interface BlocklistRepository {
    fun observeAll(): Flow<List<BlocklistEntry>>
    suspend fun contains(numberHash: String): Boolean
    suspend fun add(numberHash: String, displayLabel: String)
    suspend fun remove(numberHash: String)
}
