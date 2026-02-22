package com.fenn.callshield.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.fenn.callshield.Phase2Flags
import com.fenn.callshield.data.local.dao.CallerEventDao
import com.fenn.callshield.data.local.entity.CallerEventEntry
import com.fenn.callshield.domain.model.CallDecision
import com.fenn.callshield.domain.repository.CallHistoryRepository
import com.fenn.callshield.domain.usecase.GetScreeningSettingsUseCase
import com.fenn.callshield.domain.usecase.ScreenCallUseCase
import com.fenn.callshield.notification.CallNotificationManager
import com.fenn.callshield.util.PhoneNumberHasher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Android system entry point for call screening.
 * Must respond within ~1500ms or Android passes the call through.
 *
 * The screening decision pipeline is delegated to [ScreenCallUseCase].
 */
@AndroidEntryPoint
class CallShieldScreeningService : CallScreeningService() {

    @Inject lateinit var screenCallUseCase: ScreenCallUseCase
    @Inject lateinit var callHistoryRepo: CallHistoryRepository
    @Inject lateinit var notificationManager: CallNotificationManager
    @Inject lateinit var hasher: PhoneNumberHasher
    @Inject lateinit var paywallTrigger: PaywallTriggerManager
    @Inject lateinit var getScreeningSettings: GetScreeningSettingsUseCase
    @Inject lateinit var ringTimeRegistry: RingTimeRegistry
    @Inject lateinit var callerEventDao: CallerEventDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart
        Log.d(TAG, "onScreenCall: number=$rawNumber direction=${callDetails.callDirection}")

        // Notify RingTimeRegistry so short-ring detection works on next IDLE transition
        if (Phase2Flags.BEHAVIORAL_DETECTION && rawNumber != null) {
            hasher.hash(rawNumber)?.let { ringTimeRegistry.onRingStart(it) }
        }

        serviceScope.launch {
            var responded = false
            try {
                // Stay within 1500ms budget with a 1400ms safety margin
                val decision = withTimeout(1400L) {
                    screenCallUseCase.execute(rawNumber)
                }

                Log.d(TAG, "Decision: $decision for $rawNumber")
                respondToCall(callDetails, buildResponse(decision))
                responded = true

                // Record history and post notifications — NonCancellable ensures this
                // completes even if the service is destroyed (onDestroy cancels serviceScope)
                withContext(NonCancellable) {
                    recordAndNotify(rawNumber, decision)
                }
            } catch (e: Exception) {
                if (responded) return@launch  // already responded — do not override with allow
                Log.e(TAG, "Screening failed for $rawNumber — allowing (fail open)", e)
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
        val label = rawNumber ?: "Hidden"

        // Record behavioral event for frequency / burst detection
        if (Phase2Flags.BEHAVIORAL_DETECTION && hash != "unknown") {
            callerEventDao.insert(CallerEventEntry(numberHash = hash, eventType = "INCOMING_CALL"))
            callerEventDao.enforceCapForHash(hash)
        }

        val (score, category, source) = when (decision) {
            is CallDecision.Silence -> Triple(decision.confidenceScore, decision.category, decision.source)
            is CallDecision.Flag -> Triple(decision.confidenceScore, decision.category, decision.source)
            is CallDecision.Reject -> Triple(1.0, null, decision.source)
            is CallDecision.Allow -> Triple(0.0, null, com.fenn.callshield.domain.model.DecisionSource.DEFAULT)
        }

        callHistoryRepo.record(hash, label, decision, score, category, source.name)

        val settings = getScreeningSettings.get()
        Log.d(TAG, "recordAndNotify: notifyOnBlock=${settings.notifyOnBlock} notifyOnFlag=${settings.notifyOnFlag}")

        when (decision) {
            is CallDecision.Reject -> {
                if (settings.notifyOnBlock) notificationManager.showBlockedCallNotification(label, hash, decision.source.displayLabel)
                else Log.d(TAG, "Blocked notification suppressed — notifyOnBlock=false")
                paywallTrigger.onSpamCallDetected()
            }
            is CallDecision.Silence -> {
                // PRD §3.3: silenced calls go to missed calls — warn user about callback risk
                if (settings.notifyOnBlock) notificationManager.showMissedCallWarningNotification(label, hash, decision.category)
                else Log.d(TAG, "Missed call warning suppressed — notifyOnBlock=false")
                paywallTrigger.onSpamCallDetected()
            }
            is CallDecision.Flag -> {
                if (settings.notifyOnFlag) notificationManager.showFlaggedCallNotification(label, decision.confidenceScore, decision.category)
                else Log.d(TAG, "Flagged notification suppressed — notifyOnFlag=false")
            }
            is CallDecision.Allow -> Unit
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "CallShield.Screening"
    }
}
