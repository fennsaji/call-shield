package com.fenn.callguard.domain.repository

import com.fenn.callguard.data.local.entity.CallHistoryEntry
import com.fenn.callguard.domain.model.CallDecision
import kotlinx.coroutines.flow.Flow

interface CallHistoryRepository {
    fun observeRecent(): Flow<List<CallHistoryEntry>>
    suspend fun record(
        numberHash: String,
        displayLabel: String,
        decision: CallDecision,
        confidenceScore: Double,
        category: String?,
        decisionSource: String,
    )
    suspend fun stats(): CallStats
    fun observeStats(): Flow<CallStats>
}

data class CallStats(
    val totalScreened: Int,
    val totalBlocked: Int,
)
