package com.fenn.callshield.domain.repository

import com.fenn.callshield.data.local.entity.VipContactEntry
import kotlinx.coroutines.flow.Flow

interface VipContactsRepository {
    fun observeAll(): Flow<List<VipContactEntry>>
    suspend fun add(e164: String, displayLabel: String)
    suspend fun remove(numberHash: String)
    suspend fun clear()
}
