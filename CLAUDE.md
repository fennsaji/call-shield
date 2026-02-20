# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**CallGuard** — a privacy-first spam and scam call protection app for Android, targeting the Indian market. No contact uploads. No ads. No raw phone number storage. The repository is in pre-development; all content is documentation and planning.

Full specs: `docs/PRD/`, `docs/Tech Stack.md`, `docs/Developer Guidelines.md`, `docs/Wireframes.md`

---

## Tech Stack (Planned)

- **Android:** Kotlin, Jetpack Compose, Hilt, Room (SQLite), Ktor, DataStore
- **Backend:** Supabase — PostgreSQL + Edge Functions (TypeScript), RLS enabled
- **Key Android API:** Call Screening API — app must register as the device's call screening service
- **Monetization:** Google Play Billing Library — freemium, ₹399/year primary / ₹49/month secondary
- **Min SDK:** Android 10 (API 29)

---

## Architecture

**Pattern:** Clean Architecture + MVVM. Dependencies point inward — domain layer has zero Android/Supabase imports.

```
UI Layer (Compose + ViewModels)
  → Domain Layer (Use Cases + Entities — pure Kotlin)
    → Data Layer (Repositories + Room + Supabase Edge Functions)
```

**`CallScreeningService`** is a separate Android Service, outside the MVVM flow. It has a strict time budget:

```
CallScreeningService
  → ScreeningOrchestrator (injectable, unit-testable)
    → [parallel] LocalBlocklistCheck, SeedDatabaseCheck, PrefixRuleCheck
    → [async, 1500ms hard timeout] ReputationRemoteCheck (Supabase Edge Function)
  → CallDecision (Allow / Silence / Reject)
```

Remote lookup uses a three-state circuit breaker (Closed → Open → Half-open). When Open, all remote calls return `null` immediately — no waiting for timeout.

**Module layout:**
```
app/
├── ui/          # Compose screens, ViewModels
├── domain/      # Use cases, entities, repository interfaces
├── data/
│   ├── local/   # Room DAOs, seed DB access
│   ├── remote/  # Supabase client, Edge Function calls
│   └── repository/
├── screening/   # CallScreeningService + ScreeningOrchestrator
└── di/          # Hilt modules
```

---

## Call Decision Priority

`ScreeningOrchestrator` checks in this order — first definitive match wins:

| Priority | Check | Result |
|---|---|---|
| 1 | Personal whitelist | Always Allow (skips all further checks) |
| 2 | Personal blocklist | Reject |
| 3 | Prefix rules | Reject or Silence per rule |
| 4 | Private/hidden number (if enabled) | Reject |
| 5 | Seed DB | Known Spam → Silence or Reject |
| 6 | Backend reputation | ≥ 0.6 = Likely Spam; ≥ 0.8 = auto-block (Pro only) |
| 7 | Default | Allow (Unknown) |

---

## Hashing Convention

All phone numbers are hashed **on-device** using HMAC-SHA256 with a **static salt bundled in the app binary** before any network call. The salt is not fetched from the server — this ensures hashing works offline on first launch.

- Normalize to E.164 first: `+919876543210`
- Then: `HMAC-SHA256(e164_number, bundled_salt)`
- The hash is the **only** number-derived value ever sent to or stored on Supabase.

Plain SHA-256 is not used — Indian mobile numbers (~1 billion) are small enough to fully enumerate.

---

## Device Token

- Generated once as a cryptographically random UUID, stored in **Android Keystore**.
- Survives app **updates** but **not full uninstalls** (Keystore entry is tied to the app UID; uninstall deletes it).
- Only `HMAC-SHA256(uuid, bundled_salt)` is sent to the backend — the raw UUID never leaves the device.
- Used for rate limiting and reporter deduplication. Never linked to any account.

---

## What Goes Where

| Concern | Location |
|---|---|
| Blocking decision logic | On-device only |
| Number hashing (HMAC-SHA256) | On-device, before any network call |
| Device token | Android Keystore |
| Reputation counters & confidence scores | Supabase (hash-keyed) |
| Local blocklist, whitelist & prefix rules | Room (SQLite) |
| Seed spam DB | Bundled asset, delta-updated via HTTPS + SHA-256 checksum |
| call_decision_audit log | Room (SQLite), on-device only |
| Behavioral events buffer (Phase 2) | Room (SQLite), 24h hard TTL |
| Encrypted backup blobs (Phase 3) | Supabase Storage |

---

## Backend Schema (Phase 1)

```sql
reputation          -- number_hash, report_count, unique_reporters,
                    -- confidence_score, category TEXT, negative_signals,
                    -- last_reported_at, last_computed_at

report_events       -- append-only source of truth; enables score recomputation
                    -- number_hash, device_token_hash, category, reported_at, schema_version

reporter_deduplication  -- (number_hash, device_token_hash) PRIMARY KEY
                        -- enforces true unique_reporters count; one contribution per device per number ever

quarantine_queue    -- Phase 2 only; created on Phase 2 feature flag activation
number_categories   -- Phase 2 only; added alongside category voting
```

Edge Functions (TypeScript, Supabase):
- `POST /report` — rate-limit → dedup → insert event → recompute score
- `GET /reputation` — hash lookup, rate-limited 60/device/hr
- `POST /correct` — "Not Spam" signal → increment negative_signals → recompute score
- `GET /seed-db/manifest` — current version + SHA-256 checksum

Clients never access the DB directly. RLS is enabled on all tables.

---

## Confidence Score Formula (Phase 1, hardcoded in Edge Function)

```
base_score        = min(unique_reporters / 10, 1.0)
recency_decay     = max(0, 1 - days_since_last_report / 90)
confidence_score  = base_score * recency_decay
```

Records purged after 12 months of zero reports. Formula is hardcoded — no `GET /config` endpoint in Phase 1.

---

## Phase 1 Scope — What's Deliberately Excluded

These are **not** Phase 1. Do not partially build them:

| Feature | Phase |
|---|---|
| Play Integrity API on POST /report | 2 |
| Ed25519 seed DB signing | 2 |
| Behavioral detection (call frequency, burst, short-ring) | 2 |
| Report velocity quarantine + category voting | 2 |
| `number_categories` table | 2 |
| TRAI Quick Report | 2 |
| SMS scam detection | 2 |
| Encrypted cloud sync | 3 |
| Family Protection (QR pairing) | 3 |
| On-device ML | 4 (exploratory only) |

**Also removed from Phase 1:** server-managed HMAC secret (use static salt), `GET /config` endpoint (hardcode formula), timing normalization on GET /reputation, 4-digit QR confirmation.

---

## Subscription Tiers

- **Free:** manual blocking, whitelist, prefix blocking, seed DB detection, manual spam reporting.
- **Pro (₹399/year or ₹49/month):** auto-block high-confidence spam (before ringing), advanced prefix rules, early DB delta updates.
- **Family Plan (₹699/year, Phase 3):** two devices via QR pairing.
- **Lifetime (₹599–799, Phase 4):** Tier 2/3 city segment.

Paywall is triggered at the **value moment** (first spam call silenced for a free user) — not on install.

---

## Key Documents

| File | Purpose |
|---|---|
| `docs/Developer Guidelines.md` | Non-negotiable privacy rules, hashing, circuit breaker, abuse resistance, phase boundaries |
| `docs/Tech Stack.md` | Full schema DDL, module structure, seed DB pipeline, API diagram |
| `docs/PRD/Phase 1 PRD.md` | Complete Phase 1 functional requirements |
| `docs/Wireframes.md` | All 14 Phase 1 screens with annotations |
| `docs/Roadmap.md` | Phase trigger conditions, risk management |
