package com.fenn.callshield.screening

import com.fenn.callshield.data.local.dao.CallerEventDao
import javax.inject.Inject

private const val FREQUENCY_WINDOW_MS = 60 * 60 * 1000L   // 60 minutes
private const val FREQUENCY_THRESHOLD = 3                  // ≥ 3 = anomaly

private const val BURST_WINDOW_MS = 15 * 60 * 1000L       // 15 minutes
private const val BURST_THRESHOLD = 5                      // ≥ 5 = burst

private const val SHORT_RING_WINDOW_MS = 24 * 60 * 60 * 1000L  // 24 hours
private const val SHORT_RING_MIN_COUNT = 2                      // ≥ 2 events required (PRD §3.2)

/**
 * Checks call frequency and behavioral patterns for a given number hash.
 * Queries the local [recent_caller_events] table — no network required.
 */
class CallFrequencyAnalyzer @Inject constructor(
    private val callerEventDao: CallerEventDao,
) {

    /** Returns true if ≥3 INCOMING_CALLs from [hash] in the last 60 minutes. */
    suspend fun isFrequencyAnomaly(hash: String): Boolean {
        val since = System.currentTimeMillis() - FREQUENCY_WINDOW_MS
        return callerEventDao.countSince(hash, since) >= FREQUENCY_THRESHOLD
    }

    /** Returns true if ≥5 INCOMING_CALLs from [hash] in the last 15 minutes. */
    suspend fun isBurstPattern(hash: String): Boolean {
        val since = System.currentTimeMillis() - BURST_WINDOW_MS
        return callerEventDao.countSince(hash, since) >= BURST_THRESHOLD
    }

    /** Returns true if [hash] had ≥2 SHORT_RING events in the last 24 hours (PRD §3.2). */
    suspend fun hadRecentShortRing(hash: String): Boolean {
        val since = System.currentTimeMillis() - SHORT_RING_WINDOW_MS
        return callerEventDao.countShortRingsSince(hash, since) >= SHORT_RING_MIN_COUNT
    }
}
