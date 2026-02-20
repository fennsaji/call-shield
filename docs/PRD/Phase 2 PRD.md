# CallGuard – Phase 2 Detailed PRD

## 0. Trigger Conditions

All of the following must be met before Phase 2 development begins:

* 1,000+ active users
* 7-day retention ≥ 35% (users are finding ongoing value)
* Spam reports ≥ 0.5/user/week average (users are actively contributing)
* Seed DB covers ≥ 70% of the top 1,000 most-reported Indian spam prefixes (behavioral detection amplifies a good DB — it doesn't rescue a thin one)
* Crash-free rate ≥ 99%

If the Phase 1.5 checkpoint (6 weeks post-launch) returns Yellow or Red, Phase 2 is paused until the underlying retention or DB coverage issue is resolved.

---

## 1. Phase 2 Objective

Enhance CallGuard from a rule-based spam blocker into an intelligence-driven detection system while preserving privacy-first architecture.

Phase 2 focuses on behavioral detection, smarter reputation scoring, and improved transparency without introducing invasive data collection.

---

## 2. Strategic Goals

* Improve spam detection accuracy without relying on massive databases.
* Reduce false positives through smarter scoring.
* Increase user trust through explainable detection.
* Strengthen abuse resistance in reputation system.

---

## 3. Behavioral Detection Engine (On-Device)

### 3.1 Call Frequency Anomaly Detection

* Detect repeated calls from same number: 3+ calls within 60 minutes triggers a frequency anomaly flag.
* Detect burst call patterns: 5+ calls within 15 minutes triggers a burst pattern flag.
* Assign behavior score locally based on flag count and severity.

### 3.2 Short Ring Pattern Detection

* Identify calls that disconnect in under 8 seconds without being answered (common robocall pattern).
* 2+ such events from the same number within 24 hours increases the suspicion score.

**Architecture note:** The Call Screening API fires `onScreenCall()` when an incoming call arrives — it does not report what happens after the call is allowed through (answered, rejected, or hung up). To detect short-ring events, the app needs a `CallDurationObserver` component that uses `TelephonyCallback` (API 31+) or `PhoneStateListener` (API 29–30) to observe `CALL_STATE_RINGING → CALL_STATE_IDLE` transitions without an intervening `CALL_STATE_OFFHOOK`. The `CallScreeningService` sets a short-lived entry in an in-memory map keyed on `number_hash` with a `ringStartTime`. The `CallDurationObserver` reads this map on state transition and computes duration.

`LISTEN_CALL_STATE` (basic ringing/idle/offhook states) does not require `READ_PHONE_STATE` on API 29–30 and does not receive the caller's phone number — the number correlation comes from the `CallScreeningService` entry already written. No additional permissions are required beyond those already declared for Phase 1. `READ_PHONE_STATE` must **not** be added.

This component must be scoped to Phase 2 and created only when the behavioral detection feature flag is active.

### 3.3 Suspicious Activity Scoring

* Combine prefix match, local DB match, and behavioral signals.
* Generate composite spam_score.
* Spam score must remain explainable (no black-box ML in Phase 2).

All behavioral logic must run entirely on-device.

### 3.4 On-Device Behavioral Buffer Schema

Behavioral detection requires storing recent call events locally. This storage must be strictly bounded to avoid becoming a surveillance log:

```
recent_caller_events (Room/SQLite)
    ├── number_hash        TEXT NOT NULL   ← HMAC-SHA256, not raw number
    ├── event_type         TEXT NOT NULL   ← "incoming", "missed", "short_ring"
    ├── event_at           INTEGER NOT NULL ← Unix timestamp
    └── duration_seconds   INTEGER         ← null if unanswered
```

- Hard TTL of **24 hours**: a scheduled job purges all rows where `event_at < now - 86400s`.
- Maximum 100 rows per `number_hash`. Oldest rows evicted when limit is reached.
- Raw phone numbers are **never stored** — only the HMAC-SHA256 hash.
- This table must be explicitly declared in the Phase 1 Data Safety Form as local-only behavioral data with automatic deletion. It must **not** appear before Phase 2 is active — the table is created on Phase 2 feature flag activation.

---

## 4. Advanced Reputation System

### 4.1 Weighted Report Scoring

* Assign weight to reports based on reporter trust score.
* Trust score increases with consistent, non-malicious reporting behavior.

### 4.2 Reputation Decay

* Confidence score gradually decreases if no recent reports.
* Prevent permanent labeling from outdated spam campaigns.

### 4.3 Unique Reporter Enforcement

* Reputation must require multiple unique reporters before high-confidence classification.
* Prevent rapid inflation from single source.

---

## 5. Transparency & Explainability Panel

When a number is flagged, the user must be able to view:

* Number of reports
* Behavioral flags triggered
* Prefix rule match (if any)
* Local DB match status
* Confidence level

Language must remain simple and understandable.

---

## 6. Scam Intelligence Feed (Full Version)

Upgrade the Phase 1 manual scam digest into a structured, regularly updated feed:

* RBI scam alerts
* CERT advisories
* Trending scam categories
* Educational content on avoiding fraud
* Update cadence: at minimum weekly, automated pull from public advisory sources where APIs are available

Content must be sourced from public and regulatory sources only. No user data is used to personalize the feed.

## 7. TRAI Quick Report Integration

Moved from Phase 1 — builds on validated reporting behavior established in Phase 1.

User Flow:

* One-tap "Report to TRAI" from any flagged call or manual blocklist entry.
* App auto-fills the TRAI complaint SMS format (1909 format).
* SMS app opened for user confirmation and manual send.
* App records the number in a local "Reported Numbers" list with status "Complaint prepared".
* "Reported Numbers" screen accessible in app settings.

Compliance rule: no automated filing without explicit user confirmation.

**Implementation constraint:** The app opens the SMS app via `ACTION_SENDTO` intent and returns immediately — it cannot detect whether the SMS was actually sent. Entries in the "Reported Numbers" list must be labeled **"Complaint prepared"**, not "Complaint sent." Do not imply the complaint reached TRAI; the user must send it manually from the SMS app.

## 8. Basic SMS Scam Pattern Detection

Purpose: Extend protection to the fastest-growing Indian scam vector — SMS-based UPI fraud.

* Pattern-match incoming SMS against a maintained list of known scam templates (fake KYC, fake OTP requests, fake bank suspension alerts, fake UPI payment requests).
* No ML required — rule-based regex matching in Phase 2.
* Flag suspicious SMS with a non-intrusive in-app indicator.
* This feature requires `RECEIVE_SMS` permission and must be strictly opt-in, with explicit user consent shown during setup.
* Template list sourced from RBI, CERT, and public scam alert databases.
* Raw SMS content is never stored or transmitted — only the match result (matched / not matched).

**Play Store risk:** `RECEIVE_SMS` is one of the most scrutinized permissions in Google Play review. It triggers mandatory manual review and has caused delays or rejections even for legitimate apps. This must be declared in the Data Safety Form with a full justification. Before implementation, evaluate whether notification-listener-based SMS reading (without `RECEIVE_SMS`) can achieve sufficient coverage; if it can, prefer that approach. If `RECEIVE_SMS` is required, ensure the Play Console submission includes detailed justification and a test account for reviewers. Factor potential review delay into the Phase 2 launch timeline.

---

## 9. Reputation Abuse Protection (Enhanced)

* Rate limiting per device
* Suspicious reporting pattern detection
* Automated dampening of low-trust reporters
* Monitoring abnormal report spikes
* **Play Integrity API** added to `POST /report` in Phase 2. All report submissions must include a Play Integrity token verified server-side. This prevents multi-reinstall Sybil attacks that become profitable at larger user scale. (Was deferred from Phase 1 due to implementation complexity and latency cost.)
* **Ed25519 seed DB signing** added to the delta update pipeline in Phase 2. The signing step protects against a compromised Supabase Storage account. The private key is held offline; the public key is bundled in the app. (Was deferred from Phase 1 to avoid the offline signing station and CI key management requirements at MVP stage.)

**Report velocity quarantine:** If more than 5 reports for the same `number_hash` arrive within 60 minutes, all incoming reports are held in a `quarantine_queue` table for 24 hours before being applied to reputation counters. This prevents coordinated bombing campaigns from taking immediate effect.

**Category voting:** When a number has reports across multiple categories, the displayed category is determined by majority vote. A category requires a lead of ≥ 3 reports over the next closest category before being promoted as the primary label. Single-actor category flipping is prevented.

System must maintain integrity without storing personal identity.

---

## 10. Performance & Stability Requirements

* Behavioral detection must execute within Android screening time limits.
* Composite scoring must not delay call handling.
* CPU and battery usage must remain minimal.

---

## 11. Data Governance (Phase 2 Additions)

* Trust scores stored anonymously.
* No user profiles created.
* Reputation metadata remains hash-based.
* No expansion of personal data collection.

---

## 12. Testing Plan

* Simulate burst-call scenarios.
* Validate composite scoring thresholds.
* Stress-test weighted reputation logic.
* Validate decay behavior over time.
* Monitor false positive and false negative rates.

---

## 13. Phase 2 Success Criteria

* Spam detection accuracy improved by ≥ 15% over Phase 1 baseline.
* False positive rate stays below 1% (same ceiling as Phase 1).
* Increased user trust — measurable via app store rating (target ≥ 4.3).
* Behavioral detection adds no perceptible latency to call screening decision.
* Stable performance with added intelligence layer.

---

## 14. Out of Scope (Phase 2)

* Machine learning neural network models.
* Carrier-level integrations.
* iOS expansion.
* Enterprise APIs.

Phase 2 strengthens intelligence while preserving privacy-first foundations.
