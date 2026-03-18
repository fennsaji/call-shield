package com.fenn.callshield

/**
 * Compile-time feature flags for Phase 2 capabilities.
 * Set to false to disable a feature without removing the code path.
 */
object Phase2Flags {
    /** On-device behavioral detection: frequency anomaly, burst pattern, short-ring. */
    const val BEHAVIORAL_DETECTION = false

    /** TRAI Quick Report button in call history and reason transparency panel. */
    const val TRAI_REPORT = false

    /** SMS scam detection — Phase 2 later addition, off by default. */
    const val SMS_DETECTION = false
}
