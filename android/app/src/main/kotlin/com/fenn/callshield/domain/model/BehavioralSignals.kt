package com.fenn.callshield.domain.model

/**
 * Aggregated on-device behavioral signals for a caller hash.
 * Produced by [CallFrequencyAnalyzer] and [RingTimeRegistry].
 *
 * Used by [ScreenCallUseCase] to escalate a Default-Allow call to Flag
 * when behavioral patterns suggest a scam bait-and-callback attempt.
 */
data class BehavioralSignals(
    /** 3+ calls from same hash within the last 60 minutes. */
    val frequencyAnomaly: Boolean,
    /** 5+ calls from same hash within the last 15 minutes. */
    val burstPattern: Boolean,
    /** Previous call from this hash ended within 3 seconds of ringing (bait call). */
    val shortRing: Boolean,
) {
    val hasAnySignal: Boolean get() = frequencyAnomaly || burstPattern || shortRing

    /** Human-readable description for the Reason Transparency Sheet. */
    fun describe(): String = buildList {
        if (burstPattern) add("Rapid repeated calls (burst pattern)")
        else if (frequencyAnomaly) add("Frequent calls in the last hour")
        if (shortRing) add("Previous call ended immediately (possible bait call)")
    }.joinToString(" · ")

    companion object {
        val NONE = BehavioralSignals(
            frequencyAnomaly = false,
            burstPattern = false,
            shortRing = false,
        )
    }
}
