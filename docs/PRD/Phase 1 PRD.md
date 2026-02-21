# CallShield – Phase 1 Detailed PRD

## 1. Phase 1 Objective

Deliver a stable, privacy-compliant Android application that blocks obvious spam calls, earns user trust through radical transparency, and validates that users will pay for a privacy-respecting alternative to Truecaller.

Phase 1 focuses on reliability, clarity, and conversion — not advanced intelligence.

---

## 2. User Experience Flow

### Incoming Call Flow

1. Incoming call detected via Call Screening API.
2. Number checked in strict priority order (first definitive match wins):

   * Personal whitelist → Always Allow
   * Personal blocklist → Reject
   * Prefix rules → Reject or Silence per rule
   * Private/hidden number blocking (if enabled) → Reject
   * Local seed spam database → Known Spam → Silence or Reject
   * Backend reputation lookup (HMAC-SHA256 hashed, 1500ms timeout) → Likely Spam or auto-block
3. Decision made:

   * Allow
   * Silence
   * Reject
4. Decision written to on-device `call_decision_audit` log (number hash, decision, reason, timestamp).
5. User can view reason if flagged.

### Manual Report Flow

1. User taps "Report Spam".
2. Selects category from: Telemarketing / Promotional, Loan or Financial Scam, Investment Scam, Impersonation (bank / government), Phishing, Job or Work From Home Scam, Other.
3. Number normalized to E.164 and hashed locally using HMAC-SHA256 with app-bundled salt.
4. Hash + category + device token hash sent to backend.
5. Backend deduplicates against `reporter_deduplication` table before incrementing `unique_reporters`.
6. Reputation score recomputed and returned.

### "Not Spam" Correction Flow

1. User taps "Not Spam" on a blocked or flagged call.
2. Number is re-added to personal whitelist (overrides all future detections for this number).
3. A correction signal is sent to backend via `POST /correct` (hash + device token hash).
4. Backend increments `negative_signals` for that number and recomputes `confidence_score`.
5. If `negative_signals` reaches the dampening threshold (≥ 5), the number's confidence score is reduced proportionally.

---

## 3. Functional Requirements (Detailed)

### 3.1 Trust-Model Onboarding

* Shown once on first install, before requesting any permissions.
* Two screens only — must complete in under 60 seconds.
* Screen 1: "What we do" — "We block spam calls using a database of known scam numbers. No contact upload required."
* Screen 2: "What we never do" — "We never see your contacts, call logs, or who's calling. The number is hashed on your device before it ever reaches our servers."
* Must not be skippable. User must tap "Got it" to proceed.
* This is a conversion requirement, not just a UX nicety.

### 3.2 Call Screening

* Must register as call screening service.
* Must handle silent rejection.
* Must allow user override and unblock.
* Must work offline for local checks.
* Must support blocking private/hidden numbers (no caller ID) as a user-configurable option.

### 3.3 Missed Call / Callback Risk Indicator

* When a missed call arrives from an unknown number, the app shows a reputation indicator on the missed call entry in the recent calls screen.
* If the number is Known Spam or Likely Spam: display a warning banner — "This number has been reported as [category]. Proceed with caution before calling back."
* If Unknown: show neutral state. Do not alarm the user unnecessarily.
* The indicator must appear before the user dials back — not after.
* This feature directly prevents financial harm from premium-rate callback scams and impersonation calls.

### 3.4 Prefix Blocking

* User can define custom numeric prefixes.
* System supports common spam prefixes (e.g., promotional number series).
* Prefix rules applied before backend lookup.

### 3.5 Personal Whitelist

* Numbers on the whitelist always allow through — overrides all other checks including seed DB and backend reputation.
* User can add numbers manually or via "Not Spam" correction flow.
* Stored in a separate Room table from the blocklist — they must not share a table.
* Must function offline.

### 3.6 Local Blocklist

* Add number manually.
* Remove number.
* Stored securely on device.
* Must function without internet.

### 3.7 Seed Spam Database

* Preloaded compressed dataset.
* Fast lookup structure.
* Support delta updates.
* Must not expose raw source list publicly.

### 3.8 Anonymous Reputation Backend

* Accept hashed phone numbers only.
* Increment report counters.
* Return reputation score.
* Apply basic rate limiting.

### 3.9 Privacy Dashboard (Trust Statement)

* Single screen — must be readable in under 30 seconds.
* Plain-language statement: "We store only a hashed fingerprint of numbers you report. We never see your contacts, call logs, or who called you."
* Shows: what is collected (hashed reports only), what is never collected (contacts, call logs, SMS content, recordings), last sync timestamp.
* Provides one-tap option to delete all local data.
* Do not build a complex multi-section dashboard in Phase 1. A settings screen no one reads provides no trust value.

### 3.10 Lightweight Scam Digest

* Weekly in-app card shown on the home screen.
* Shows top 2–3 active scam types in India sourced from RBI/CERT public advisories.
* Example card: "Investment scams targeting UPI users are up this week. Scammers are posing as SEBI officials."
* Manual curation is acceptable in Phase 1 — no automation or ML required.
* Purpose: gives users a reason to open the app on days they receive no spam calls. Critical for retention.

### 3.11 Subscription

* Feature gating between Free and Pro.
* Trial support — 7-day free trial triggered by the first value moment (first spam call blocked or identified), not on install.
* Annual plan (₹399/year) presented as the default option at the paywall. Monthly (₹49/month) is secondary.
* Graceful handling of expired subscription — Pro features disable without losing configuration.

Free tier includes: manual blocking, whitelist, prefix blocking, seed DB detection, manual spam reporting.

Pro tier includes: auto-block high-confidence spam (calls blocked before ringing), advanced prefix rules, early access to seed DB delta updates.

Paywall trigger: Show upgrade prompt the moment a spam call is detected and silenced for a free user, with the message: "This call was from a high-confidence scam number. Pro would have blocked it before it rang." Do not show a generic feature list — show the value in the moment it's most relevant.

Family upsell intent capture: At the Pro paywall, show a secondary option: "Want to protect a family member too? Join the waitlist for Family Plan." Capture email intent only. Family Protection ships in Phase 3 — this builds the launch list now.

---

### 3.12 Call Classification States

Three states are used throughout the app and must be displayed consistently:

| State | Condition | UI Label |
|---|---|---|
| Known Spam | Matched in local seed DB | "Known Spam" |
| Likely Spam | confidence_score ≥ 0.6 from backend reputation | "Likely Spam" |
| Unknown | No match in any source | "Unknown Number" |

### 3.13 Auto-Block Logic (Pro Feature)

* System must auto-block numbers when confidence_score ≥ 0.8 (high-confidence threshold).
* Auto-block must be toggleable by user.
* User must receive a visible in-app notification and system notification when auto-block occurs, showing: the number blocked, the reason, and an "Undo / Mark as Not Spam" action.
* User must be able to mark as "Not Spam" to immediately unblock and suppress future auto-block for that number.

### 3.14 Reason Transparency Panel

* When a call is flagged, user can view:

  * Matched prefix rule (if applicable)
  * Presence in seed spam database
  * Reputation report count (if applicable)
* Reason display must be clear and non-technical.

### 3.15 Basic Abuse Protection (MVP Level)

* Prevent repeated spam reports for same number from same device within short time window.
* True uniqueness enforced via `reporter_deduplication` table — keyed on `(number_hash, device_token_hash)`. A device may only contribute once to `unique_reporters` for any given number, regardless of how many times they report it.
* Rate limit `POST /report` per `device_token_hash` at the Edge Function level (e.g., max 20 reports per device per hour).
* Do not escalate to high-confidence classification without a minimum of 3 unique reporters.
* Play Integrity API is deferred to Phase 2 — at MVP scale, rate limiting and `reporter_deduplication` provide sufficient protection without the implementation complexity and latency cost.

### 3.16 Delta Update Mechanism

* Seed spam database must support periodic lightweight delta updates.
* The app fetches `GET /seed-db/manifest` to check the current version and SHA-256 checksum before downloading a delta. If the downloaded file's checksum does not match the manifest, the delta is discarded and the existing DB is kept.
* Updates are served over HTTPS only from Supabase Storage — transport integrity is provided by TLS. The SHA-256 checksum guards against file corruption.
* Update checks must not impact battery life significantly.
* Ed25519 signing is deferred to Phase 2. It protects against a compromised Supabase account — a real threat at scale but not a Day 1 risk. Adding it in Phase 2 avoids the need for an offline signing station and CI key management pipeline in Phase 1.

### 3.13 OEM Stability Handling

* App must guide user to disable battery optimization if required.
* Provide in-app instructions for major OEMs (MIUI, Samsung, etc.).

### 3.14 Required Android Permissions

| Permission | Purpose |
|---|---|
| `android.app.role.CALL_SCREENING` | Role required to register as call screening service |
| `android.permission.BIND_SCREENING_SERVICE` | Service binding for Call Screening API |
| `android.permission.POST_NOTIFICATIONS` | Blocked call notifications (Android 13+) |
| `android.permission.INTERNET` | Backend reputation lookups and seed DB delta updates |

No `READ_CALL_LOG`, `READ_CONTACTS`, or `RECORD_AUDIO` permissions are requested. The Call Screening API provides the caller number directly without requiring call log access.

### 3.15 Metrics & Monitoring

* Track (anonymously):

  * Number of blocks triggered
  * Number of reports submitted
  * Auto-block usage rate
  * Subscription conversion events
* No personally identifiable data collected.

---

## 4. Performance Requirements

* Incoming call decision must complete within allowed Android screening time window.
* Backend lookup latency must not delay call handling.
* App must not significantly increase battery drain.

---

## 5. Compliance Requirements

* Must comply with Google Play call screening and permission policies.
* Must accurately complete Data Safety Form.
* Must provide public Privacy Policy URL.

---

## 6. Data Governance Rules

* No raw phone numbers stored on backend.
* No contact uploads.
* No SMS reading without explicit opt-in (not included in Phase 1).
* Reputation requires a minimum of 3 unique reporters before high-confidence classification (confidence_score ≥ 0.8).

### 6.1 Data Retention Policy

| Data | Location | Retention |
|---|---|---|
| Reputation metadata (hashed) | Supabase | Indefinite with confidence decay; auto-removed after 12 months of zero reports |
| Local blocklist and prefix rules | On-device | Until user deletes via Privacy Dashboard |
| Seed DB | On-device asset | Replaced on each delta update |
| App preferences | On-device | Until app uninstalled |

No user accounts or session identifiers are retained on the backend.

---

## 7. Testing Plan

* Test across top Android OEM devices in India.
* Validate prefix rule accuracy.
* Simulate high report volumes.
* Validate reputation threshold logic.
* Test subscription upgrade/downgrade flows.

---

## 8. Launch Readiness Checklist

* Core blocking validated.
* Reputation lookup stable.
* Privacy dashboard reviewed.
* Play Console submission prepared.
* Crash monitoring configured.

---

## 9. Phase 1 Success Criteria

* 7-day retention ≥ 40%.
* Conversion to Pro ≥ 3%.
* False positive rate below 1%.
* Active spam reporting by users (≥ 1 report/user/week).
* Seed DB miss rate below 50% at launch, trending toward 30% by week 8.
* Positive early reviews referencing privacy and user control (not just "it blocked a call").

## 10. Non-Goals for Phase 1

* TRAI Quick Report SMS helper — deferred to Phase 2 (builds on validated reporting behavior)
* SMS scam detection — Phase 2
* Behavioral anomaly detection — Phase 2
* Weighted trust scoring — Phase 2
* Family Protection (any form) — Phase 3
* Encrypted cloud sync — Phase 3
* Machine learning — Phase 4
