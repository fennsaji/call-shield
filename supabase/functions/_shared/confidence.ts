/**
 * Phase 1 confidence score formula (hardcoded — no config endpoint).
 *
 * Core formula (from spec):
 *   base_score       = min(unique_reporters / 10, 1.0)
 *   recency_decay    = max(0, 1 - days_since_last_report / 90)
 *   confidence_score = base_score * recency_decay
 *
 * unique_reporters of 10 = maximum base score (1.0)
 * Scores decay to 0 linearly over 90 days with no new reports.
 *
 * Negative-signal dampening (beyond spec, intentional):
 *   When negative_signals reaches NEGATIVE_DAMPENING_THRESHOLD (5), a
 *   dampening factor is applied to the raw score. This reflects "Not Spam"
 *   corrections submitted via /correct — a high number of corrections
 *   indicates the number may have been wrongly reported and its score should
 *   be suppressed proportionally. The dampening reaches 0 at 20 negative
 *   signals (NEGATIVE_DAMPENING_ZERO = 20).
 */

/** Minimum negative_signals count before dampening is applied. */
const NEGATIVE_DAMPENING_THRESHOLD = 5;
/** negative_signals count at which dampening factor reaches 0 (full suppression). */
const NEGATIVE_DAMPENING_ZERO = 20;

export function computeConfidenceScore(
  uniqueReporters: number,
  negativeSignals: number,
  lastReportedAt: Date | null,
): number {
  if (uniqueReporters === 0 || lastReportedAt === null) return 0;

  const baseScore = Math.min(uniqueReporters / 10, 1.0);

  const daysSinceLastReport =
    (Date.now() - lastReportedAt.getTime()) / (1000 * 60 * 60 * 24);
  const recencyDecay = Math.max(0, 1 - daysSinceLastReport / 90);

  const rawScore = baseScore * recencyDecay;

  // Apply negative-signal dampening when enough "Not Spam" corrections have
  // been received. See NEGATIVE_DAMPENING_THRESHOLD / NEGATIVE_DAMPENING_ZERO.
  if (negativeSignals >= NEGATIVE_DAMPENING_THRESHOLD) {
    const dampingFactor = Math.max(0, 1 - negativeSignals / NEGATIVE_DAMPENING_ZERO);
    return rawScore * dampingFactor;
  }

  return rawScore;
}

export const LIKELY_SPAM_THRESHOLD = 0.6;
export const HIGH_CONFIDENCE_THRESHOLD = 0.8;
export const MIN_UNIQUE_REPORTERS = 3;
