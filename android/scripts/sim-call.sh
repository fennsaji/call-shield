#!/usr/bin/env bash
# sim-call.sh — Simulate an incoming call on the Android emulator
#
# Usage:
#   ./sim-call.sh                          # calls from +919876543210 (default)
#   ./sim-call.sh +911234567890            # calls from a custom number
#   ./sim-call.sh +911234567890 emulator-5556   # custom number + custom device

set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
NUMBER="${1:-+919876543210}"
DEVICE="${2:-emulator-5554}"

echo "📞 Simulating incoming call from $NUMBER on $DEVICE..."
"$ADB" -s "$DEVICE" emu gsm call "$NUMBER"
echo ""
echo "  ✅ Call ringing"
echo ""
echo "  To answer : ./sim-call-accept.sh $NUMBER $DEVICE"
echo "  To hang up: ./sim-call-cancel.sh $NUMBER $DEVICE"
