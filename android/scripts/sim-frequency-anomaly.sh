#!/usr/bin/env bash
# sim-frequency-anomaly.sh — Simulate a frequency anomaly: 3+ calls from same number
# within 60 minutes. Triggers CallFrequencyAnalyzer.isFrequencyAnomaly().
#
# Each call rings briefly, hangs up, then the next one comes in.
# Expected result: 3rd call gets flagged with decisionSource=BEHAVIORAL, category=frequency_anomaly
#
# Usage:
#   ./sim-frequency-anomaly.sh                          # default number, 3 calls
#   ./sim-frequency-anomaly.sh +911234567890            # custom number
#   ./sim-frequency-anomaly.sh +911234567890 4          # custom number, 4 calls
#   ./sim-frequency-anomaly.sh +911234567890 4 emulator-5556

set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
NUMBER="${1:-+919876543210}"
COUNT="${2:-3}"
DEVICE="${3:-emulator-5554}"
RING_DURATION=6    # seconds to ring before cancelling
PAUSE_BETWEEN=5    # seconds between calls

echo "📊 Simulating frequency anomaly: $COUNT calls from $NUMBER on $DEVICE"
echo "   Threshold: 3+ calls in 60 min → flagged as BEHAVIORAL/frequency_anomaly"
echo "   Watch logcat: ./sim-logcat.sh $DEVICE"
echo ""

for i in $(seq 1 "$COUNT"); do
    echo "  [$i/$COUNT] Calling..."
    "$ADB" -s "$DEVICE" emu gsm call "$NUMBER"
    sleep "$RING_DURATION"
    "$ADB" -s "$DEVICE" emu gsm cancel "$NUMBER"
    echo "  [$i/$COUNT] Ended"
    if [ "$i" -lt "$COUNT" ]; then
        echo "  Waiting ${PAUSE_BETWEEN}s before next call..."
        sleep "$PAUSE_BETWEEN"
    fi
done

echo ""
echo "✅ Done — check Activity tab for flagged entry on call #3+"
