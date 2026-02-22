#!/usr/bin/env bash
# sim-call-accept.sh — Accept a simulated incoming call (RINGING → OFFHOOK)
#
# Usage:
#   ./sim-call-accept.sh                          # accept default number
#   ./sim-call-accept.sh +911234567890            # accept specific number
#   ./sim-call-accept.sh +911234567890 emulator-5556

set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
NUMBER="${1:-+919876543210}"
DEVICE="${2:-emulator-5554}"

echo "✅ Accepting call from $NUMBER on $DEVICE..."
"$ADB" -s "$DEVICE" emu gsm accept "$NUMBER"
echo "  ✅ Call active (OFFHOOK)"
echo ""
echo "  To hang up: ./sim-call-cancel.sh $NUMBER $DEVICE"
