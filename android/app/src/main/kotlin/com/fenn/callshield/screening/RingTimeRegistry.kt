package com.fenn.callshield.screening

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private const val SHORT_RING_THRESHOLD_MS = 8_000L  // < 8 seconds = bait call (PRD §3.2)

/**
 * In-memory tracker for ring start times.
 *
 * - [onRingStart] is called by [CallShieldScreeningService] when a call arrives.
 * - [onCallEnded] is called by [CallStateMonitor] when the call state returns to IDLE.
 *
 * Thread-safe via AtomicReference — both fields are written atomically so dual-SIM
 * concurrent calls cannot produce a torn state where activeHash belongs to SIM A but
 * ringStartMs belongs to SIM B.
 */
@Singleton
class RingTimeRegistry @Inject constructor() {

    private val activeRing = AtomicReference<Pair<String, Long>?>(null)

    fun onRingStart(hash: String) {
        activeRing.set(hash to System.currentTimeMillis())
    }

    /**
     * Called when call state transitions to IDLE.
     * Returns the active hash and whether the ring was short, or null if no ring was tracked.
     */
    fun onCallEnded(): Pair<String, Boolean>? {
        val ring = activeRing.getAndSet(null) ?: return null
        val durationMs = System.currentTimeMillis() - ring.second
        return ring.first to (durationMs < SHORT_RING_THRESHOLD_MS)
    }
}
