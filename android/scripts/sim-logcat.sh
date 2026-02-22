#!/usr/bin/env bash
# sim-logcat.sh — Stream CallGuard screening logs from the emulator.
# Filters to tags relevant for behavioral detection testing.
#
# Usage:
#   ./sim-logcat.sh                   # default device
#   ./sim-logcat.sh emulator-5556     # custom device

set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
DEVICE="${1:-emulator-5554}"

echo "📋 Streaming CallGuard logs from $DEVICE (Ctrl+C to stop)..."
echo "──────────────────────────────────────────────────────────────"

"$ADB" -s "$DEVICE" logcat -s \
    "CallShield.Screening" \
    "CallShield.StateMonitor" \
    "CallShield.Orchestrator" \
    "CallShield.FreqAnalyzer"
