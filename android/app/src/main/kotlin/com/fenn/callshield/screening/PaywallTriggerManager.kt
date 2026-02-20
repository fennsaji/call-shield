package com.fenn.callshield.screening

import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.data.preferences.ScreeningPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRD §3.11: "Show upgrade prompt the moment a spam call is detected and silenced
 * for a free user."
 *
 * Emits a trigger event into [paywallTrigger] the first time a spam call is
 * silenced/rejected for a non-Pro user. UI layers subscribe to this flow.
 */
@Singleton
class PaywallTriggerManager @Inject constructor(
    private val prefs: ScreeningPreferences,
    private val billingManager: BillingManager,
) {
    private val _paywallTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val paywallTrigger: SharedFlow<Unit> = _paywallTrigger.asSharedFlow()

    /** Call whenever a spam call is silenced or rejected. */
    suspend fun onSpamCallDetected() {
        if (billingManager.isPro.value) return
        if (prefs.isTrialTriggered()) return
        prefs.setTrialTriggered(true)
        _paywallTrigger.tryEmit(Unit)
    }
}
