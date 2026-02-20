package com.fenn.callguard.data.repository

import com.fenn.callguard.data.local.dao.CallHistoryDao
import com.fenn.callguard.data.local.entity.CallHistoryEntry
import com.fenn.callguard.domain.model.CallDecision
import com.fenn.callguard.domain.repository.CallHistoryRepository
import com.fenn.callguard.domain.repository.CallStats
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CallHistoryRepositoryImpl @Inject constructor(
    private val dao: CallHistoryDao,
) : CallHistoryRepository {

    override fun observeRecent(): Flow<List<CallHistoryEntry>> = dao.observeRecent()

    override suspend fun record(
        numberHash: String,
        displayLabel: String,
        decision: CallDecision,
        confidenceScore: Double,
        category: String?,
        decisionSource: String,
    ) {
        val outcome = when (decision) {
            is CallDecision.Reject -> "rejected"
            is CallDecision.Silence -> "silenced"
            is CallDecision.Allow -> "allowed"
            is CallDecision.Flag -> "flagged"
        }
        dao.insert(
            CallHistoryEntry(
                numberHash = numberHash,
                displayLabel = displayLabel,
                outcome = outcome,
                confidenceScore = confidenceScore,
                category = category,
                decisionSource = decisionSource,
            )
        )
        dao.pruneOldEntries()
    }

    override suspend fun stats(): CallStats = CallStats(
        totalScreened = dao.totalCount(),
        totalBlocked = dao.blockedCount(),
    )
}
