package com.fenn.callshield.domain.repository

import com.fenn.callshield.data.local.entity.WhitelistEntry
import kotlinx.coroutines.flow.Flow

interface WhitelistRepository {
    fun observeAll(): Flow<List<WhitelistEntry>>
    suspend fun contains(numberHash: String): Boolean
    suspend fun add(numberHash: String, displayLabel: String)
    suspend fun remove(numberHash: String)
}
