#!/usr/bin/env bash
# sim-burst-pattern.sh — Simulate a burst pattern: 5+ calls from same number
# within 15 minutes. Triggers CallFrequencyAnalyzer.isBurstPattern().
#
# Burst pattern is checked before frequency anomaly in ScreenCallUseCase —
# the 5th call should be flagged as BEHAVIORAL/burst_pattern at confidence 0.5.
#
# Usage:
#   ./sim-burst-pattern.sh                          # default number, 5 calls
#   ./sim-burst-pattern.sh +911234567890            # custom number
#   ./sim-burst-pattern.sh +911234567890 6          # custom number, 6 calls
#   ./sim-burst-pattern.sh +911234567890 6 emulator-5556

set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
NUMBER="${1:-+919876543210}"
COUNT="${2:-5}"
DEVICE="${3:-emulator-5554}"
RING_DURATION=4    # seconds to ring before cancelling
PAUSE_BETWEEN=3    # seconds between calls (keep total well under 15 min)

echo "💥 Simulating burst pattern: $COUNT rapid calls from $NUMBER on $DEVICE"
echo "   Threshold: 5+ calls in 15 min → flagged as BEHAVIORAL/burst_pattern"
echo "   Watch logcat: ./sim-logcat.sh $DEVICE"
echo ""

for i in $(seq 1 "$COUNT"); do
    echo "  [$i/$COUNT] Calling..."
    "$ADB" -s "$DEVICE" emu gsm call "$NUMBER"
    sleep "$RING_DURATION"
    "$ADB" -s "$DEVICE" emu gsm cancel "$NUMBER"
    echo "  [$i/$COUNT] Ended"
    if [ "$i" -lt "$COUNT" ]; then
        sleep "$PAUSE_BETWEEN"
    fi
done

echo ""
echo "✅ Done — check Activity tab for flagged entry on call #5+"
