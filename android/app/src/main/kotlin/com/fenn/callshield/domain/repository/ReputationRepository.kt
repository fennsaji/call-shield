package com.fenn.callshield.domain.repository

import com.fenn.callshield.domain.model.ReputationResult

interface ReputationRepository {
    /**
     * Looks up reputation for [numberHash]:
     *   1. Checks seed DB first (synchronous, always available).
     *   2. Falls through to remote API with circuit breaker.
     *   3. Returns NOT_FOUND if both miss or remote is unavailable.
     *
     * Must complete within the 1500ms screening window budget.
     */
    suspend fun lookup(numberHash: String): ReputationResult

    suspend fun submitReport(numberHash: String, category: String): Result<Unit>
    suspend fun submitCorrection(numberHash: String): Result<Unit>
}
