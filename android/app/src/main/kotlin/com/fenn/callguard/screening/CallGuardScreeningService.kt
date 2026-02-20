package com.fenn.callguard.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.fenn.callguard.domain.model.CallDecision
import com.fenn.callguard.domain.repository.CallHistoryRepository
import com.fenn.callguard.domain.usecase.ScreenCallUseCase
import com.fenn.callguard.notification.CallNotificationManager
import com.fenn.callguard.util.PhoneNumberHasher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Android system entry point for call screening.
 * Must respond within ~1500ms or Android passes the call through.
 *
 * The screening decision pipeline is delegated to [ScreenCallUseCase].
 */
@AndroidEntryPoint
class CallGuardScreeningService : CallScreeningService() {

    @Inject lateinit var screenCallUseCase: ScreenCallUseCase
    @Inject lateinit var callHistoryRepo: CallHistoryRepository
    @Inject lateinit var notificationManager: CallNotificationManager
    @Inject lateinit var hasher: PhoneNumberHasher
    @Inject lateinit var paywallTrigger: PaywallTriggerManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart

        serviceScope.launch {
            try {
                // Stay within 1500ms budget with a 1400ms safety margin
                val decision = withTimeout(1400L) {
                    screenCallUseCase.execute(rawNumber)
                }

                val response = buildResponse(decision)
                respondToCall(callDetails, response)

                // Record history and post notifications (best-effort, outside response path)
                recordAndNotify(rawNumber, decision)
            } catch (e: Exception) {
                // On timeout or error: always allow (fail open)
                respondToCall(
                    callDetails,
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .build(),
                )
            }
        }
    }

    private fun buildResponse(decision: CallDecision): CallResponse =
        CallResponse.Builder().apply {
            when (decision) {
                is CallDecision.Reject -> {
                    // Disconnect the call — Pro auto-block and manual blocklist
                    setDisallowCall(true)
                    setRejectCall(true)
                    setSkipCallLog(true)
                }
                is CallDecision.Silence -> {
                    // Silence ring — goes to missed calls; free-tier default for spam
                    setDisallowCall(true)
                    setRejectCall(false)
                }
                is CallDecision.Allow -> {
                    setDisallowCall(false)
                    setRejectCall(false)
                }
                is CallDecision.Flag -> {
                    // Call rings normally; risk notification posted separately
                    setDisallowCall(false)
                    setRejectCall(false)
                }
            }
        }.build()

    private suspend fun recordAndNotify(rawNumber: String?, decision: CallDecision) {
        val hash = rawNumber?.let { hasher.hash(it) } ?: "unknown"
        val label = rawNumber?.takeLast(4)?.let { "****$it" } ?: "Hidden"

        val (score, category, source) = when (decision) {
            is CallDecision.Silence -> Triple(decision.confidenceScore, decision.category, decision.source)
            is CallDecision.Flag -> Triple(decision.confidenceScore, decision.category, decision.source)
            is CallDecision.Reject -> Triple(1.0, null, decision.source)
            is CallDecision.Allow -> Triple(0.0, null, com.fenn.callguard.domain.model.DecisionSource.DEFAULT)
        }

        callHistoryRepo.record(hash, label, decision, score, category, source.name)

        when (decision) {
            is CallDecision.Reject -> {
                notificationManager.showBlockedCallNotification(label, hash)
                paywallTrigger.onSpamCallDetected()
            }
            is CallDecision.Silence -> {
                notificationManager.showBlockedCallNotification(label, hash)
                paywallTrigger.onSpamCallDetected()
            }
            is CallDecision.Flag -> notificationManager.showFlaggedCallNotification(label, decision.confidenceScore, decision.category)
            is CallDecision.Allow -> Unit
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
