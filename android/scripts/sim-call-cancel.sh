#!/usr/bin/env bash
# sim-call-cancel.sh — Hang up / cancel a simulated call (RINGING → IDLE)
# Useful for testing short-ring detection — call rings briefly then caller hangs up.
#
# Usage:
#   ./sim-call-cancel.sh                          # cancel default number
#   ./sim-call-cancel.sh +911234567890            # cancel specific number
#   ./sim-call-cancel.sh +911234567890 emulator-5556

set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
NUMBER="${1:-+919876543210}"
DEVICE="${2:-emulator-5554}"

echo "📵 Cancelling call from $NUMBER on $DEVICE..."
"$ADB" -s "$DEVICE" emu gsm cancel "$NUMBER"
echo "  ✅ Call ended (IDLE)"
