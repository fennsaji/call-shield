# CallShield – Product Roadmap

## Vision

You decide what gets through. We never see who's calling.

Build India's most trusted spam and scam protection app — powered by on-device intelligence, transparent reputation, and zero data harvesting.

---

# Phase 1 – MVP (8–10 Weeks)

## Goal

Launch a stable, policy-compliant spam blocker that delivers immediate, visible protection and validates that users will pay for a privacy-respecting alternative to Truecaller.

Primary objective:

* Stop obvious spam calls reliably
* Earn user trust through transparency and control
* Validate reporting behavior and subscription conversion
* Prove the product works well enough before building intelligence on top of it

This phase focuses on reliability, clarity, and compliance — not advanced intelligence.

---

## Detailed Scope

### 1. Trust-Model Onboarding

Purpose: Users don't know Truecaller harvests contacts. Without explaining CallShield's model, they cannot understand what they're paying for.

Two-screen onboarding (shown once on first install):

* Screen 1: What we do — "We block spam calls using a database of known scam numbers. No contact upload required."
* Screen 2: What we never do — "We never see your contacts, call logs, or who's calling. The number is hashed on your device before it ever reaches our servers."

This is a must-have for conversion, not a nice-to-have.

---

### 2. Call Screening Engine (Android)

Purpose: Deliver real, visible protection from day one.

Implementation Focus:

* Integrate Android Call Screening API properly (policy compliant).
* Ensure app can be set as default call screening service.
* Handle OEM edge cases (MIUI, Realme, Samsung battery restrictions).

User Capabilities:

* Auto-reject high-confidence spam.
* Silence suspected spam.
* Block private/hidden numbers.
* Block numbers by prefix (e.g., 140, +92, custom user-defined prefixes).
* Maintain personal blocklist (manual add/remove).
* Personal whitelist — always allow, overrides all detection.

UX Requirements:

* Clear toggle for auto-block.
* Visible indicator when a call was blocked.
* Easy "Unblock" and "Not Spam" flow accessible from the blocked call notification.
* Never block silently without user override.

---

### 3. Missed Call / Callback Risk Indicator

Purpose: Users frequently call back unknown missed calls — this is when many scams execute (premium-rate numbers, impersonation). Warn before they call back.

When a missed call arrives from an unknown number:

* Show a reputation indicator alongside the missed call in the app's recent calls screen.
* If the number has a Known Spam or Likely Spam classification, show a warning: "This number has been reported as [category]. Proceed with caution."
* If Unknown, show neutral state — do not alarm unnecessarily.

This is a high-value, low-complexity feature that directly prevents financial harm.

---

### 4. Local Spam Intelligence (Seed Layer)

Purpose: Avoid the "empty database" problem.

Implementation Focus:

* Bundle compressed seed spam dataset sourced from: TRAI NDNC registry, DoT spam number advisories, and RBI fraud alert publications.
* Store locally using optimized structure (fast lookup).
* Enable secure delta updates from backend (Ed25519-signed).

Detection Logic (MVP Level):

* If number exists in local seed DB → mark as "Known Spam".
* If hash reputation score exceeds threshold → mark as "Likely Spam".
* Otherwise → show as "Unknown".

Transparency:

* User can tap and see reason: "Matched known spam list".

---

### 5. Anonymous Reputation Backend (Supabase)

Purpose: Enable scalable reputation without storing personal data.

Data Stored (Server Side Only):

* number_hash (HMAC-SHA256)
* report_count
* unique_reporters_count
* confidence_score
* last_reported_at

Strict Privacy Constraints:

* No raw phone numbers.
* No contact uploads.
* No call logs.
* No device fingerprinting.
* No advertising ID storage.

MVP Logic:

* When report_count crosses threshold → increase confidence_score.
* Return only confidence metadata to client.

Abuse Protection (Basic MVP Level):

* Rate limit report submissions per device token.
* reporter_deduplication table enforces one unique_reporters increment per device per number — ever.
* Play Integrity API token required on every report submission.

---

### 6. Manual Reporting System

Purpose: Build distributed reputation slowly and ethically.

User Flow:

* User taps "Report Spam".
* Select category (Telemarketing, Loan Scam, Fraud, etc.).
* Number is hashed locally.
* Only hash + category sent to backend.

User Feedback:

* Confirmation message.
* Show updated reputation count (if threshold crossed).

Important:

* Never auto-block globally based on one report.
* Reputation must require multiple unique reports.

---

### 7. Privacy Dashboard (Trust Statement)

Purpose: Make the trust model visible — not a complex settings screen, but a single clear statement of what CallShield does and doesn't collect.

Screen shows:

* Plain-language statement: "We store only a hashed fingerprint of numbers you report. We never see your contacts, call logs, or who called you."
* What is never collected: contacts, call logs, SMS content, call recordings.
* Last sync timestamp.
* Option to delete all local data.

Keep this to one screen. The goal is that a user reads it in 30 seconds and feels reassured. A complex multi-section dashboard adds QA surface without proportional trust value.

---

### 8. Lightweight Scam Digest

Purpose: Give users a reason to open the app on days they receive no spam calls. Retention mechanism.

Weekly in-app card showing:

* Top 2–3 active scam types in India this week (sourced from RBI/CERT public advisories).
* Example: "UPI impersonation scams up 30% this week — scammers are posing as bank KYC agents."

This is a content feature, not an engineering feature. No personalization required. No ML. Manual curation initially is acceptable.

---

### 9. Subscription

Purpose: Validate willingness to pay.

Free Tier:

* Manual blocking and whitelist
* Prefix blocking
* Basic seed DB detection
* Manual spam reporting

Pro Tier:

* Auto-block high-confidence spam (calls blocked before ringing)
* Advanced prefix rules
* Early access to seed DB delta updates

Pricing Strategy:

* ₹399/year (default, prominently presented — equivalent to ₹33/month)
* ₹49/month (secondary option)
* 7-day free trial tied to first value moment (first spam call blocked or identified)

Paywall Trigger:

* Do not show paywall on install. Show it at the moment a spam call is detected and silenced for a free user: "This call was from a high-confidence scam number. Pro would have blocked it before it rang." This ties the upgrade ask to a concrete value demonstration.

Family Upsell Intent Capture:

* At the Pro paywall, show a secondary option: "Want to protect a family member too? Join the waitlist for Family Plan." Capture email intent — even though Family Protection ships in Phase 3, this validates demand and builds a launch list.

---

## Non-Goals for MVP (Deliberately Excluded)

* Behavioral anomaly detection — Phase 2
* Weighted trust scoring — Phase 2
* TRAI Quick Report SMS helper — Phase 2 (low-usage behavior; build after core reporting is validated)
* Advanced abuse detection — Phase 2
* SMS scam detection — Phase 2
* Encrypted cloud sync — Phase 3
* Family Protection (QR pairing, multi-device sync) — Phase 3
* Machine learning models — Phase 4
* iOS version — Phase 4

---

## Testing & Stability Requirements

Before launch:

* Test across top 5 Android OEMs in India.
* Validate no ANRs or background crashes.
* Verify Play Store policy compliance for permissions.
* Confirm no aggressive battery drain.
* Validate seed DB coverage against a representative sample of known spam numbers reported on Indian consumer forums.

---

## MVP Success Metrics

Primary Metrics:

* 7-day retention — target ≥ 40%
* % users enabling auto-block — target ≥ 30%
* Spam reports per active user per week — target ≥ 1
* False positive rate — must stay below 1%
* Conversion to Pro — target ≥ 3%
* Seed DB miss rate — % of spam calls users report that were not in the seed DB at time of call. Target: below 50% at launch, below 30% by week 8.

---

## Phase 1.5 — Validation Checkpoint (6 Weeks Post-Launch)

Before beginning Phase 2 development, run a mandatory checkpoint at 6 weeks post-launch:

**Green (proceed to Phase 2):**
- 7-day retention ≥ 35%
- Spam reports ≥ 0.5/user/week
- Seed DB miss rate trending down

**Yellow (proceed with caution):**
- Retention 25–35%: continue Phase 1 iteration, delay Phase 2 by 4 weeks
- Run user interviews: are users churning because of DB miss rate, false positives, or unclear value?

**Red (stop and diagnose):**
- Retention below 25%: pause Phase 2 planning entirely
- Run a 2-week research sprint: user interviews, session recordings, false positive audit
- Core product may need repositioning before intelligence layer adds any value

---

# Phase 2 – Intelligence Layer (Post Traction)

## Trigger Conditions

All three must be met:

* 1,000+ active users
* 7-day retention ≥ 35% (behavior signal — users are finding ongoing value)
* Spam reports ≥ 0.5/user/week average (engagement signal — users are actively contributing)
* Seed DB covers ≥ 70% of the top 1,000 most-reported Indian spam prefixes (DB quality gate — behavioral detection amplifies a good DB; it doesn't replace a thin one)
* Crash-free rate ≥ 99%

## Goal

Shift from rule-based blocking to behavioral detection and smarter reputation scoring.

## Features

### 1. Behavioral Detection Engine (On-Device)

* Call frequency anomaly detection
* Burst-call detection
* Short-duration ring detection
* Suspicious call pattern scoring

### 2. Weighted Reputation System

* Reputation decay over time
* Report weight based on trust score
* Spam confidence scoring model

### 3. Transparency Panel

* Show why call was flagged

  * Number of reports
  * Pattern detection signals
  * Prefix match

### 4. Scam Intelligence Feed (Full Version)

* RBI alerts
* CERT advisories
* Public scam warnings
* Trending scam categories (upgrade from Phase 1 manual digest to structured feed)

### 5. TRAI Quick Report Integration

Moved from Phase 1 — builds on validated reporting behavior:

* One-tap TRAI complaint SMS pre-fill
* Complaint reference stored locally
* Complaint history screen

### 6. Basic SMS Scam Pattern Detection

* Pattern-match incoming SMS against known UPI fraud templates (fake KYC, fake OTP, fake bank alerts).
* No ML required — rule-based regex matching against a maintained template list.
* Flag suspicious SMS with a non-intrusive indicator.
* This is opt-in and requires explicit user permission.

### 7. Report Velocity Quarantine and Category Voting

* Coordinated report bombing mitigation
* Category majority voting to prevent single-actor label flipping

---

# Phase 3 – Privacy Ecosystem Expansion

## Trigger Conditions

* 10,000+ active users
* Phase 2 behavioral detection validated (false positive rate still below 1%)
* Reputation system integrity confirmed (no abuse incidents at scale)

## Goal

Build scalable privacy infrastructure and launch the family protection feature properly.

## Features

### 1. Full Family Protection Mode

* QR-based pairing with 4-digit out-of-band confirmation
* Anonymous token-keyed rule sync via Supabase
* Guardian pushes prefix rules and block preferences to dependent devices
* No contact sharing, no surveillance

### 2. Encrypted Cloud Sync (Optional)

* Client-side encrypted blocklist
* Supabase encrypted storage
* No plaintext phone numbers

### 3. Reputation Abuse Protection

* Rate limit spam reports
* Detect malicious reporting patterns
* Trust-based report weighting

### 4. Performance Optimization

* DB compression
* Delta updates
* Battery optimization

---

# Phase 4 – Scale & Strategic Growth

## Trigger Condition

* 50,000+ active users
* 4.5+ rating
* Sustainable subscription revenue

## Expansion Opportunities

* Lightweight on-device ML — only if a measurable accuracy gap exists that Phase 2 behavioral rules cannot close; requires full training pipeline (evaluate at Phase 4 kickoff, not a committed feature)
* Carrier and regulatory partnerships — business development effort, not engineering; pursue exploratory conversations; build integration code only after signed agreement and API spec
* Enterprise offering — out of scope for consumer app; evaluate as a separate product line if strategically pursued
* iOS companion app — a new product requiring a new codebase; evaluate separately after Android reaches Phase 4 stability
* Lifetime purchase option (₹599–799) for Tier 2/3 city segment

---

# Architectural Principles

1. Privacy by default
2. On-device first, cloud second
3. Hash-based reputation
4. Transparent scoring
5. No ads, no contact upload
6. User control over all decisions

---

# Risk Management

Phase 1 Risks:

* **DB cold-start** — the seed DB will miss a significant portion of spam calls at launch. If the miss rate is above 50% at week 4, the product will feel broken to early adopters. Mitigation: validate DB coverage before launch against known spam samples; prioritize delta update cadence; monitor miss rate as a primary metric and communicate transparently to users why some calls slip through.
* Play Store permission rejection
* OEM background restrictions

Phase 2 Risks:

* False positives from behavioral detection
* Overblocking
* **RECEIVE_SMS Play Store review risk** — SMS scam detection requires `RECEIVE_SMS`, one of the most scrutinized permissions in Google Play. It triggers mandatory manual review and has caused delays or outright rejections for legitimate apps. Mitigation: evaluate notification-listener-based approach first; if `RECEIVE_SMS` is required, prepare detailed justification for Play Console and factor potential review delay into the Phase 2 timeline. Do not make Phase 2 launch dependent on SMS detection shipping.

Phase 3 Risks:

* Database abuse
* Scaling cost

Cross-Phase Risks:

* **Number portability** — spam numbers reassigned to legitimate users via MNP. Reputation decay mitigates this; decay window must be tight enough to protect reassigned numbers.
* **Carrier-level blocking makes the app redundant** — TRAI is directionally pushing carriers toward network-level spam filtering. If carrier blocking becomes standard, DB-based user-space blocking loses its value. Mitigation: position around user control and transparency, not just spam blocking. "Carriers block what they decide. You block what you decide." This positioning survives carrier-level filtering because it's about agency, not just outcomes.
* **False positives destroy trust faster than true positives build it** — a single high-profile false block of a legitimate business could generate press coverage undermining the brand. Mitigation: business dispute pathway, prominent "Not Spam" correction flow, conservative auto-block threshold.
* **Truecaller adds a privacy mode** — if regulation tightens (DPDP Act enforcement), Truecaller may add a "no contact upload" option. Mitigation: publish the technical architecture as a public commitment — the anonymous hash-based system is architecturally uncopiable by a data-harvesting model without contradicting their existing product.

Mitigation principles:

* Transparency at every step
* User override always available
* Conservative confidence thresholds
* Reputation decay

---

# Long-Term Vision

CallShield becomes India's user-controlled distributed reputation and scam detection layer.

Built on intelligence, not surveillance.
Built on user agency, not carrier decisions.
