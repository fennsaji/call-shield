# Emulator Test Scripts

All scripts default to `+919876543210` and `emulator-5554`. Both are overridable via arguments.

## Basic Call Control

```bash
./sim-call.sh [number] [device]          # Start an incoming call
./sim-call-accept.sh [number] [device]   # Answer the ringing call
./sim-call-cancel.sh [number] [device]   # Hang up (caller disconnects)
```

## Behavioral Detection Scenarios

```bash
# Short-ring: rings briefly then hangs up — 2+ triggers short_ring flag
./sim-short-ring.sh [number] [count] [device]        # default: 2 rings

# Frequency anomaly: 3+ calls in 60 min
./sim-frequency-anomaly.sh [number] [count] [device] # default: 3 calls

# Burst pattern: 5+ calls in 15 min (highest priority flag)
./sim-burst-pattern.sh [number] [count] [device]     # default: 5 calls
```

Expected outcomes in the Activity tab:

| Script | Flagged on call # | Category |
|---|---|---|
| `sim-short-ring` | 2nd short ring | `short_ring` |
| `sim-frequency-anomaly` | 3rd call | `frequency_anomaly` |
| `sim-burst-pattern` | 5th call | `burst_pattern` |

## Logs

```bash
./sim-logcat.sh [device]   # Stream screening + behavioral logs
```
