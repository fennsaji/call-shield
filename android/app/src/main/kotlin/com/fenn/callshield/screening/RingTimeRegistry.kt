package com.fenn.callshield.screening

import javax.inject.Inject
import javax.inject.Singleton

private const val SHORT_RING_THRESHOLD_MS = 8_000L  // < 8 seconds = bait call (PRD §3.2)

/**
 * In-memory tracker for ring start times.
 *
 * - [onRingStart] is called by [CallShieldScreeningService] when a call arrives.
 * - [onCallEnded] is called by [CallStateMonitor] when the call state returns to IDLE.
 *
 * Thread-safe via @Volatile + synchronized writes. Only one active ring is tracked at
 * a time, which matches single-SIM behaviour on most Indian devices.
 */
@Singleton
class RingTimeRegistry @Inject constructor() {

    @Volatile private var activeHash: String? = null
    @Volatile private var ringStartMs: Long = 0L

    fun onRingStart(hash: String) {
        activeHash = hash
        ringStartMs = System.currentTimeMillis()
    }

    /**
     * Called when call state transitions to IDLE.
     * Returns the active hash and whether the ring was short, or null if no ring was tracked.
     */
    fun onCallEnded(): Pair<String, Boolean>? {
        val hash = activeHash ?: return null
        val start = ringStartMs
        activeHash = null
        val durationMs = System.currentTimeMillis() - start
        return hash to (durationMs < SHORT_RING_THRESHOLD_MS)
    }
}
