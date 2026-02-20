package com.fenn.callguard.domain.usecase

import com.fenn.callguard.domain.repository.ReputationRepository
import com.fenn.callguard.domain.repository.WhitelistRepository
import javax.inject.Inject

/**
 * PRD §2 "Not Spam" Correction Flow:
 *   1. Number is re-added to personal whitelist (overrides all future detections).
 *   2. Correction signal sent to backend via POST /correct.
 *   3. Backend increments negative_signals and recomputes confidence_score.
 */
class MarkNotSpamUseCase @Inject constructor(
    private val whitelistRepo: WhitelistRepository,
    private val reputationRepo: ReputationRepository,
) {
    /**
     * @param numberHash  HMAC-SHA256 of the phone number
     * @param displayLabel Display-safe label (e.g. "****3210")
     */
    suspend fun execute(numberHash: String, displayLabel: String): Result<Unit> {
        // 1. Add to local whitelist — overrides all future detections for this number
        whitelistRepo.add(numberHash, displayLabel)

        // 2. Send correction signal to backend (best-effort — don't fail the whole flow)
        reputationRepo.submitCorrection(numberHash)

        return Result.success(Unit)
    }
}
