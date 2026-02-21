package com.fenn.callshield.domain.usecase

import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.domain.model.CONFIDENCE_BLOCK_THRESHOLD
import com.fenn.callshield.domain.model.CONFIDENCE_FLAG_THRESHOLD
import com.fenn.callshield.domain.model.CallDecision
import com.fenn.callshield.domain.model.DecisionSource
import com.fenn.callshield.domain.model.MIN_REPORTERS_TO_ACT
import com.fenn.callshield.domain.model.ReputationSource
import com.fenn.callshield.domain.repository.BlocklistRepository
import com.fenn.callshield.domain.repository.PrefixRuleRepository
import com.fenn.callshield.domain.repository.ReputationRepository
import com.fenn.callshield.domain.repository.WhitelistRepository
import com.fenn.callshield.util.PhoneNumberHasher
import javax.inject.Inject

/**
 * Core screening logic. Implements the call decision priority:
 *   Whitelist → Blocklist → Prefix → Hidden → Seed DB → Remote → Allow
 *
 * Free tier:  spam calls → Silence (ring suppressed, shows in missed calls)
 * Pro tier:   high-confidence spam (≥0.8) → Reject if auto-block enabled
 *
 * Called from [CallShieldScreeningService] within the 1500ms Android budget.
 */
class ScreenCallUseCase @Inject constructor(
    private val hasher: PhoneNumberHasher,
    private val whitelistRepo: WhitelistRepository,
    private val blocklistRepo: BlocklistRepository,
    private val prefixRuleRepo: PrefixRuleRepository,
    private val reputationRepo: ReputationRepository,
    private val settingsUseCase: GetScreeningSettingsUseCase,
    private val billingManager: BillingManager,
) {

    suspend fun execute(rawNumber: String?): CallDecision {
        val isPro = billingManager.isPro.value
        val settings = settingsUseCase.get()

        // ── 1. Hidden number ─────────────────────────────────────────────────
        if (rawNumber.isNullOrBlank()) {
            return if (isPro && settings.blockHiddenNumbers) {
                CallDecision.Reject(DecisionSource.HIDDEN)
            } else {
                CallDecision.Silence(0.5, null, DecisionSource.HIDDEN)
            }
        }

        val e164 = hasher.normalise(rawNumber)
        val hash = if (e164 != null) hasher.hash(rawNumber) else null

        // ── 2. Whitelist ──────────────────────────────────────────────────────
        if (hash != null && whitelistRepo.contains(hash)) {
            return CallDecision.Allow
        }

        // ── 3. Blocklist ──────────────────────────────────────────────────────
        if (hash != null && blocklistRepo.contains(hash)) {
            return CallDecision.Reject(DecisionSource.BLOCKLIST)
        }

        // ── 4. Prefix rules ───────────────────────────────────────────────────
        if (e164 != null) {
            val prefixMatch = prefixRuleRepo.findMatch(e164)
            if (prefixMatch != null) {
                return when (prefixMatch.action) {
                    "block" -> CallDecision.Reject(DecisionSource.PREFIX)
                    "silence" -> CallDecision.Silence(1.0, null, DecisionSource.PREFIX)
                    "allow" -> CallDecision.Allow
                    else -> CallDecision.Allow
                }
            }
        }

        // ── 5 & 6. Seed DB then Remote reputation ─────────────────────────────
        if (hash != null) {
            val result = reputationRepo.lookup(hash)
            val score = result.confidenceScore
            val reporters = result.uniqueReporters

            val source = when (result.source) {
                ReputationSource.SEED_DB -> DecisionSource.SEED_DB
                ReputationSource.REMOTE -> DecisionSource.REMOTE
                ReputationSource.NOT_FOUND -> DecisionSource.DEFAULT
            }

            val hasEnoughSignal = reporters >= MIN_REPORTERS_TO_ACT ||
                result.source == ReputationSource.SEED_DB

            if (hasEnoughSignal) {
                // Pro auto-block: reject high-confidence spam before it rings
                if (isPro && settings.autoBlockHighConfidence && score >= CONFIDENCE_BLOCK_THRESHOLD) {
                    return CallDecision.Reject(source)
                }
                // Silence Known Spam (seed DB or high confidence)
                if (score >= CONFIDENCE_BLOCK_THRESHOLD || result.source == ReputationSource.SEED_DB) {
                    return CallDecision.Silence(score, result.category, source)
                }
                // Flag Likely Spam — call rings, risk notification posted
                if (score >= CONFIDENCE_FLAG_THRESHOLD) {
                    return CallDecision.Flag(score, result.category, source)
                }
            }
        }

        // ── 7. Default: allow ─────────────────────────────────────────────────
        return CallDecision.Allow
    }
}
