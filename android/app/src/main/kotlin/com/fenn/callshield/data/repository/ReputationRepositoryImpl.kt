package com.fenn.callshield.data.repository

import com.fenn.callshield.data.local.dao.SeedDbDao
import com.fenn.callshield.domain.model.ReputationResult
import com.fenn.callshield.domain.model.ReputationSource
import com.fenn.callshield.domain.repository.ReputationRepository
import com.fenn.callshield.network.ApiClient
import com.fenn.callshield.network.CircuitBreaker
import com.fenn.callshield.network.CircuitOpenException
import com.fenn.callshield.util.DeviceTokenManager
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReputationRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val seedDbDao: SeedDbDao,
    private val deviceTokenManager: DeviceTokenManager,
    private val circuitBreaker: CircuitBreaker,
) : ReputationRepository {

    override suspend fun lookup(numberHash: String): ReputationResult {
        // ── 1. Seed DB (synchronous, always available) ────────────────────────
        val seedEntry = seedDbDao.lookup(numberHash)
        if (seedEntry != null) {
            return ReputationResult(
                confidenceScore = seedEntry.confidenceScore,
                category = seedEntry.category,
                reportCount = 0,
                uniqueReporters = 0,
                source = ReputationSource.SEED_DB,
            )
        }

        // ── 2. Remote API with circuit breaker + 1200ms timeout ───────────────
        return try {
            withTimeout(1200L) {
                circuitBreaker.execute {
                    val response = apiClient.getReputation(
                        numberHash = numberHash,
                        deviceTokenHash = deviceTokenManager.deviceTokenHash,
                    )
                    ReputationResult(
                        confidenceScore = response.confidence_score,
                        category = response.category,
                        reportCount = response.report_count,
                        uniqueReporters = response.unique_reporters,
                        source = ReputationSource.REMOTE,
                    )
                }
            }
        } catch (_: CircuitOpenException) {
            notFound()
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            notFound()
        } catch (_: Exception) {
            notFound()
        }
    }

    override suspend fun submitReport(numberHash: String, category: String): Result<Unit> {
        return try {
            val ok = apiClient.postReport(
                numberHash = numberHash,
                deviceTokenHash = deviceTokenManager.deviceTokenHash,
                category = category,
            )
            if (ok) Result.success(Unit)
            else Result.failure(Exception("Server rejected the report"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitCorrection(numberHash: String): Result<Unit> {
        return try {
            val ok = apiClient.postCorrect(
                numberHash = numberHash,
                deviceTokenHash = deviceTokenManager.deviceTokenHash,
            )
            if (ok) Result.success(Unit)
            else Result.failure(Exception("Server rejected the correction"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun notFound() = ReputationResult(
        confidenceScore = 0.0,
        category = null,
        reportCount = 0,
        uniqueReporters = 0,
        source = ReputationSource.NOT_FOUND,
    )
}
