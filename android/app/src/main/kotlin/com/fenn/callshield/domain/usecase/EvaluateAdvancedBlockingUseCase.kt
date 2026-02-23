package com.fenn.callshield.domain.usecase

import com.fenn.callshield.domain.model.AdvancedBlockingPolicy
import com.fenn.callshield.domain.model.BlockingPreset
import com.fenn.callshield.domain.model.CallDecision
import com.fenn.callshield.domain.model.DecisionSource
import com.fenn.callshield.domain.model.UnknownCallAction
import com.fenn.callshield.domain.repository.BlocklistRepository
import com.fenn.callshield.domain.repository.CallHistoryRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * Evaluates the Advanced Blocking Policy for an incoming call.
 *
 * Returns null = no match (continue pipeline).
 * Returns non-null = policy decision (short-circuit).
 *
 * Evaluation order:
 *   1. Night Guard — silence/reject during configured hours
 *   2. Contacts Only — reject unknown callers
 *   3. Silence Unknown — silence non-contacts
 *   4. International Lock — silence/reject non-+91 numbers
 *   5. Auto-Escalate — auto-add to blocklist after N rejections
 */
class EvaluateAdvancedBlockingUseCase @Inject constructor(
    private val callHistoryRepo: CallHistoryRepository,
    private val blocklistRepo: BlocklistRepository,
) {

    suspend fun evaluate(
        e164Number: String?,
        numberHash: String?,
        isContact: Boolean,
        policy: AdvancedBlockingPolicy,
        isPro: Boolean,
    ): CallDecision? {
        // Balanced preset with no customisation: skip entirely to preserve existing behaviour
        if (policy.preset == BlockingPreset.BALANCED && !policy.isCustomized()) return null

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // 1. Night Guard
        if (policy.nightGuardEnabled) {
            if (isInNightWindow(currentHour, policy.nightGuardStartHour, policy.nightGuardEndHour) && !isContact) {
                return when {
                    isPro && policy.nightGuardAction == UnknownCallAction.REJECT ->
                        CallDecision.Reject(DecisionSource.ADVANCED_BLOCKING)
                    else ->
                        CallDecision.Silence(1.0, "night_guard", DecisionSource.ADVANCED_BLOCKING)
                }
            }
        }

        // 2. Contacts Only
        if (policy.allowContactsOnly && !isContact) {
            return CallDecision.Silence(1.0, "contacts_only", DecisionSource.ADVANCED_BLOCKING)
        }

        // 3. Silence Unknown
        if (policy.silenceUnknownNumbers && !isContact) {
            return CallDecision.Silence(0.5, "silence_unknown", DecisionSource.ADVANCED_BLOCKING)
        }

        // 4. International Lock — non-+91 numbers
        if (policy.blockInternational && e164Number != null && !e164Number.startsWith("+91")) {
            return CallDecision.Silence(1.0, "international_lock", DecisionSource.ADVANCED_BLOCKING)
        }

        // 5. Auto-Escalate
        if (policy.autoEscalateEnabled && numberHash != null) {
            val rejections = callHistoryRepo.countRejections(numberHash)
            if (rejections >= policy.autoEscalateThreshold) {
                blocklistRepo.add(numberHash, "Auto-blocked (${rejections} rejections)")
                return CallDecision.Reject(DecisionSource.ADVANCED_BLOCKING)
            }
        }

        return null
    }

    private fun isInNightWindow(hour: Int, start: Int, end: Int): Boolean =
        if (start > end) {
            // Crosses midnight, e.g. 22–7
            hour >= start || hour < end
        } else {
            hour in start until end
        }
}
