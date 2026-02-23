# CallShield – Advanced Blocking

## Product Requirements Document (PRD)

---

# 1. Overview

**Feature Name:** Advanced Blocking
**Feature Category:** Power User Firewall Layer
**Release Target:** Phase 5 (Core) + Phase 5.5 (Pro Enhancements)

Advanced Blocking introduces a structured policy engine that allows users to define who is allowed to reach them. This feature transforms CallShield from a spam detection app into a deterministic call firewall.

The feature must remain powerful but not overwhelming. Presets are provided for simplicity. Custom mode unlocks granular control.

---

# 2. Goals

1. Provide proactive access control instead of reactive spam detection.
2. Enable privacy-first, offline-capable blocking logic.
3. Differentiate CallShield from caller-ID-based competitors.
4. Drive Pro conversions through granularity and automation.

---

# 3. Non-Goals

* No complex scripting language.
* No server-side rule processing.
* No dependency on remote database for rule evaluation.
* No overwhelming UI exposure for non-technical users.

---

# 4. User Personas

### 4.1 Normal User

Wants fewer spam calls. Prefers presets. Minimal configuration.

### 4.2 Privacy-Conscious User

Wants strict control over unknown callers and international numbers.

### 4.3 Power User

Wants configurable rules, time windows, country filters, escalation logic.

---

# 5. Free vs Pro Split

## Free Tier

* Contact Boundary Controls
* Block/Silence unknown numbers
* Block by prefix (max 5 rules)
* Fixed Night Guard (10PM–7AM, silence only)
* Block all international numbers
* Basic escalation (auto-block after 3 rejected attempts)

## Pro Tier

* Unlimited prefix rules
* Custom time windows & day-of-week rules
* Country whitelist & blacklist
* Multi-condition rule logic (AND/OR)
* Rule priority reordering
* Progressive escalation ladder
* Temporary access tokens
* Policy profiles (Work, Travel, Custom)
* Advanced analytics dashboard

---

# 6. Functional Requirements

## 6.1 Preset Modes

System provides preset configurations. Each preset maps to predefined policy combinations to avoid user confusion.

### Balanced (Default)

* Allow all contacts
* Silence unknown numbers with confidence ≥ 0.6
* Prefix rules active (existing user rules)
* Night Guard disabled
* International calls allowed
* Escalation: Auto-block after 3 rejected attempts (if enabled)

Goal: Reduce spam while minimizing false positives.

---

### Aggressive

* Allow contacts + personal whitelist only
* Silence all unknown numbers by default
* Auto-reject high-confidence spam (Pro only)
* Prefix rules enforced strictly
* Behavioral flags treated as silence
* International numbers silenced unless previously allowed

Goal: Maximum spam reduction with moderate risk of missed legitimate calls.

---

### Contacts Only

* Allow contacts + manual whitelist only
* Block or silence all numbers not in contacts
* Ignore remote reputation
* Prefix rules bypassed (not needed in this mode)
* International blocked unless in contacts

Goal: Default-deny firewall mode.

---

### Night Guard

* Active between 10:00 PM – 7:00 AM
* Allow contacts + whitelist
* Silence all unknown numbers during active window
* Outside window: revert to Balanced

Goal: Prevent disturbance during sleep hours.

---

### International Lock

* Allow domestic (India) numbers only
* Silence or reject all international numbers (Pro can choose action)
* Contacts with international numbers always allowed
* Prefix rules still apply within domestic numbers

Goal: Protect against cross-border scam calls.

---

Switching presets updates underlying policies immediately. Custom mode unlocks manual override of all preset mappings.

---

## 6.2 Contact Policies

Capabilities:

* Allow all contacts
* Block all numbers not in contacts
* Silence unknown numbers
* Allow recent outgoing contacts (last N days)
* Night exception toggle

Evaluation priority: High

---

## 6.3 Prefix Rules

Capabilities:

* Block or silence by prefix
* Wildcard support (*)
* Country-aware formatting
* Time-bound rule (Pro)
* Rule priority ordering (Pro)

Evaluation priority: After blocklist, before behavioral detection.

---

## 6.4 Time Policies

Free:

* Fixed Night Guard preset

Pro:

* Custom time ranges
* Day-of-week selection
* Multiple time rules
* Action per time window (Allow / Silence / Reject)

---

## 6.5 Region Policies

Free:

* Block all international calls
* Allow domestic only

Pro:

* Country whitelist mode
* Country blacklist mode
* Combine region with time rules

Country detection via phone number country code parsing.

---

## 6.6 Escalation Logic

Free:

* Auto-block after X rejected attempts (fixed threshold)

Pro:

* Configurable thresholds
* Progressive ladder (Silence → Reject → Block)

---

## 6.7 Decision Order Transparency

User can view evaluation order:

1. Whitelist
2. Personal Blocklist
3. Advanced Blocking Policies
4. Behavioral Detection
5. Seed Database
6. Remote Reputation
7. Allow

Tap any stage to view explanation.

---

# 7. Wireframes

---

## 7.1 Advanced Blocking – Main Screen

```
[ ← ] Advanced Blocking

Status: ● Balanced Mode
Change Mode >

--------------------------------
Blocking Presets

( ) Balanced
( ) Aggressive
( ) Contacts Only
( ) Night Guard
( ) International Lock
(•) Custom

--------------------------------
Custom Policies

> Number Rules        >
> Prefix Rules        >
> Contact Policies    >
> Time Policies       >
> Region Policies     >

--------------------------------
[ View Decision Order ]
```

---

## 7.2 Contact Policies Screen

```
[ ← ] Contact Policies

☑ Allow all contacts
☐ Block all numbers not in contacts
☐ Silence unknown numbers
☐ Allow recent outgoing calls (last 7 days)

--------------------------------
Night Exception
☐ Allow contacts only during night hours

Priority: High
```

---

## 7.3 Time Policies Screen

```
[ ← ] Time Policies

Night Guard
☑ Enable Night Blocking

Start: 10:00 PM
End:   07:00 AM

Action for unknown numbers:
( ) Allow
(•) Silence
( ) Reject (Pro)

--------------------------------
Weekend Guard
☐ Block unknown calls on Sundays

--------------------------------
[ + Add Time Rule ] (Pro)
```

---

## 7.4 Add Time Rule (Modal)

```
Rule Name: __________

Apply On:
☑ Mon ☑ Tue ☑ Wed ☑ Thu ☑ Fri ☐ Sat ☑ Sun

Time Range:
Start: __:__
End:   __:__

Condition:
( ) Unknown numbers
( ) Not in contacts
( ) Prefix match
( ) Region match

Action:
( ) Allow
( ) Silence
( ) Reject

[ Save Rule ]
```

---

## 7.5 Region Policies Screen

```
[ ← ] Region Policies

Country Mode:
( ) Allow All Countries
(•) Allow Selected Countries Only (Pro)
( ) Block Selected Countries (Pro)

--------------------------------
Allowed Countries:
☑ India
☐ United Arab Emirates
☐ United States
☐ Add Country +

--------------------------------
☑ Silence unknown international numbers
```

---

## 7.6 Prefix Rules Screen

```
[ ← ] Prefix Rules

Existing Rules:
1. 140* → Reject
2. +91 9876* → Silence (Night Only)

--------------------------------
[ + Add Prefix Rule ]
```

Add Prefix Rule (Modal):

```
Prefix Pattern: __________

Apply:
( ) Always
( ) During time range (Pro)

Action:
( ) Allow
( ) Silence
( ) Reject (Pro)

Priority:
[ Move Up ] [ Move Down ] (Pro)

[ Save ]
```

---

## 7.7 Number Rules Screen

```
[ ← ] Number Rules

Blocked Numbers (12)
> View List >

Allowed Overrides (4)
> View List >

--------------------------------
Auto Escalation
☑ Auto-block after 3 rejected attempts
☐ Progressive escalation (Pro)

--------------------------------
Temporary Access
> Manage Temporary Allows > (Pro)
```

---

# 8. Technical Constraints

* All rules evaluated within 1500ms screening window.
* Rules executed deterministically before behavioral detection.
* No raw numbers sent externally (HMAC-SHA256 hashing preserved).
* Must function fully offline except optional reputation lookup.

---

# 9. Success Metrics

* % of users enabling at least one Advanced Blocking policy
* Pro conversion rate triggered by rule limit/paywall
* Reduction in unknown calls reaching ring state
* False positive rate below defined threshold

---

# 10. Risks

* Over-complex UI leading to confusion
* Conflict between rules causing unintended blocks
* Scope creep into full scripting engine

Mitigation: Presets first, Custom optional, Clear decision order visualization.

---

# 11. Future Extensions

* Geo-fencing rules
* VoIP detection heuristics
* Business mode auto-response
* Rule import/export packs

---

End of PRD
