#!/usr/bin/env bash
# sim-short-ring.sh — Simulate a short-ring: call rings briefly then hangs up.
# Repeats N times to trigger short-ring behavioral detection (threshold: 2+ in 24h).
#
# Usage:
#   ./sim-short-ring.sh                             # 2 short rings from default number
#   ./sim-short-ring.sh +911234567890               # custom number, 2 rings
#   ./sim-short-ring.sh +911234567890 3             # custom number, 3 rings
#   ./sim-short-ring.sh +911234567890 3 emulator-5556

set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
NUMBER="${1:-+919876543210}"
COUNT="${2:-2}"
DEVICE="${3:-emulator-5554}"
RING_DURATION=3   # seconds to ring before hanging up (< 8s threshold)

echo "📳 Simulating $COUNT short-ring(s) from $NUMBER on $DEVICE"
echo "   Ring duration: ${RING_DURATION}s (threshold: 8s)"
echo ""

for i in $(seq 1 "$COUNT"); do
    echo "  [$i/$COUNT] Ringing..."
    "$ADB" -s "$DEVICE" emu gsm call "$NUMBER"
    sleep "$RING_DURATION"
    "$ADB" -s "$DEVICE" emu gsm cancel "$NUMBER"
    echo "  [$i/$COUNT] Hung up after ${RING_DURATION}s"
    if [ "$i" -lt "$COUNT" ]; then
        sleep 2   # brief pause between rings
    fi
done

echo ""
echo "  ✅ Done — check logcat for SHORT_RING events:"
echo "  adb -s $DEVICE logcat -s CallShield.StateMonitor"
