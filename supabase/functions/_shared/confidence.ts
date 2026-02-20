/**
 * Phase 1 confidence score formula (hardcoded — no config endpoint).
 *
 * base_score       = min(unique_reporters / 10, 1.0)
 * recency_decay    = max(0, 1 - days_since_last_report / 90)
 * confidence_score = base_score * recency_decay
 *
 * unique_reporters of 10 = maximum base score (1.0)
 * Scores decay to 0 linearly over 90 days with no new reports.
 */
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

  // Apply dampening if negative_signals >= 5
  if (negativeSignals >= 5) {
    const dampingFactor = Math.max(0, 1 - negativeSignals / 20);
    return rawScore * dampingFactor;
  }

  return rawScore;
}

export const LIKELY_SPAM_THRESHOLD = 0.6;
export const HIGH_CONFIDENCE_THRESHOLD = 0.8;
export const MIN_UNIQUE_REPORTERS = 3;
