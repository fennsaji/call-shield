# CallShield – Tech Stack

## Mobile (Android)

- **Language:** Kotlin
- **Min SDK:** Android 10 (API 29) — minimum required for Call Screening API
- **Key APIs:** Android Call Screening API, Android Billing Library (subscriptions), Play Integrity API (added in Phase 2 for abuse resistance)
- **UI:** Jetpack Compose
- **Async:** Kotlin Coroutines + Flow
- **Dependency Injection:** Hilt
- **Navigation:** Jetpack Navigation Compose
- **Secure storage:** Android Keystore (device token)

## Backend

- **Platform:** Supabase
  - PostgreSQL for reputation storage (hash-based, no raw numbers)
  - Edge Functions for reputation lookup and report ingestion
  - Row-level security to enforce privacy constraints
  - Storage for encrypted cloud backup blobs (Phase 3)

## On-Device Storage

- **Local blocklist & prefix rules:** Room (SQLite)
- **Seed spam database:** Compressed SQLite with hash index
- **Preferences & feature flags:** DataStore (Proto)

## Networking

- **HTTP client:** Ktor (Kotlin-native, lightweight)
- **Supabase SDK:** official Kotlin client (`io.github.jan-tennert.supabase`)

## Monetization

- **Subscriptions:** Google Play Billing Library

## Analytics & Monitoring

- **Crash reporting:** TBD (Firebase Crashlytics or Sentry)
- **Anonymous metrics:** TBD (no PII, aggregate only)

---

## Android Architecture

### Pattern: Clean Architecture + MVVM

The app is structured in three layers. Dependencies point inward — the domain layer has no knowledge of Android or Supabase.

```
UI Layer  →  Domain Layer  →  Data Layer
(Compose +    (Use Cases +     (Repositories +
 ViewModels)   Entities)        Data Sources)
```

**UI Layer**
- Jetpack Compose screens observe `StateFlow` from ViewModels.
- ViewModels hold no business logic — they call use cases and expose UI state.

**Domain Layer**
- Pure Kotlin. No Android imports, no Supabase imports.
- One use case per user action (e.g., `ReportSpamUseCase`, `CheckReputationUseCase`).
- Entities: `CallDecision`, `ReputationResult`, `BlocklistEntry`, `PrefixRule`.

**Data Layer**
- Repositories implement domain interfaces and coordinate between data sources.
- Data sources: `LocalBlocklistDataSource` (Room), `SeedDatabaseDataSource` (SQLite asset), `ReputationRemoteDataSource` (Supabase Edge Functions).
- The repository decides whether to use cached/local data or make a network call.

---

### Call Screening Service

The `CallScreeningService` is a separate Android Service component, not part of the MVVM flow. It has a strict time budget.

```
CallScreeningService
    ↓
ScreeningOrchestrator   ← coordinates local checks (no coroutine overhead)
    ↓ (parallel, with timeout)
LocalBlocklistCheck     SeedDatabaseCheck     PrefixRuleCheck
    ↓ (if no local match, async with fallback)
ReputationRemoteCheck   ← Supabase Edge Function (hashed lookup)
    ↓
CallDecision (Allow / Silence / Reject)
```

- Local checks run synchronously (fast, in-memory or direct DB query).
- Remote reputation lookup runs async with a **1500ms hard timeout** (`withTimeout(1500)`). If it doesn't respond in time, the local result is used and the coroutine is cancelled.
- `ReputationRemoteDataSource` wraps the remote call with a three-state circuit breaker (Closed → Open → Half-open). When Open, all remote calls return `null` immediately without waiting for the timeout.
- `ScreeningOrchestrator` is injectable and unit-testable independently of the Android Service.

---

### Module Structure

```
app/
├── ui/                  # Compose screens, ViewModels
│   ├── home/
│   ├── blocklist/
│   ├── reporting/
│   ├── privacy/         # Privacy dashboard
│   └── subscription/
├── domain/              # Use cases, entities, repository interfaces
├── data/
│   ├── local/           # Room DAOs, seed DB access
│   ├── remote/          # Supabase client, Edge Function calls
│   └── repository/      # Repository implementations
├── screening/           # CallScreeningService + ScreeningOrchestrator
└── di/                  # Hilt modules
```

---

## Supabase Backend Architecture

```
Android Client
    ↓  (HTTPS, HMAC-SHA256 hashed numbers + device_token_hash)
Edge Functions
    ├── POST /report              ← rate-limit → dedup → insert event → recompute score
    │                               (Phase 2+: Play Integrity check added here)
    ├── GET  /reputation          ← hash lookup, rate-limited 60/device/hr
    ├── POST /correct             ← "Not Spam" signal → increment negative_signals → recompute score
    └── GET  /seed-db/manifest    ← current seed DB version + SHA-256 checksum
    ↓
PostgreSQL
```

### PostgreSQL Schema

```sql
-- Derived reputation state (read-optimised, recomputed from report_events)
reputation (
    number_hash          TEXT PRIMARY KEY,
    report_count         INT,
    unique_reporters     INT,
    confidence_score     FLOAT,
    category             TEXT,             -- single category column in Phase 1
    negative_signals     INT DEFAULT 0,
    last_reported_at     TIMESTAMP,
    last_computed_at     TIMESTAMP
)

-- Append-only source of truth for all reports (enables score recomputation)
report_events (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number_hash          TEXT NOT NULL,
    device_token_hash    TEXT NOT NULL,
    category             TEXT NOT NULL,
    reported_at          TIMESTAMP DEFAULT now(),
    schema_version       INT NOT NULL
)

-- Enforces true uniqueness of reporters per number (prevents unique_reporters inflation)
reporter_deduplication (
    number_hash          TEXT NOT NULL,
    device_token_hash    TEXT NOT NULL,
    first_reported_at    TIMESTAMP DEFAULT now(),
    PRIMARY KEY (number_hash, device_token_hash)
)

-- Quarantine for velocity-triggered holds (Phase 2)
-- Table created on Phase 2 activation, not in Phase 1
quarantine_queue (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number_hash          TEXT NOT NULL,
    device_token_hash    TEXT NOT NULL,
    category             TEXT NOT NULL,
    queued_at            TIMESTAMP DEFAULT now(),
    release_at           TIMESTAMP NOT NULL
)

-- Per-category breakdown — added in Phase 2 alongside category voting
-- report_events already stores category per event; this table is derived from it
number_categories (
    number_hash          TEXT NOT NULL,
    category             TEXT NOT NULL,
    report_count         INT DEFAULT 1,
    PRIMARY KEY (number_hash, category)
)
```

---

## Seed DB Provenance Pipeline

The seed spam database originates from public regulatory sources and follows this pipeline before distribution:

```
Sources: TRAI NDNC registry, DoT advisories, RBI fraud publications
    ↓
Normalization: deduplicate, normalize to E.164, strip non-number fields
    ↓
Build: compile into compressed SQLite with hash index
    ↓
Checksum: compute SHA-256 of the compiled file
    ↓
Upload: push to Supabase Storage with version + checksum metadata
    ↓
Manifest: GET /seed-db/manifest returns { version, sha256 }
    ↓
Client: fetches manifest → compares version → downloads delta if newer
       → verifies SHA-256 checksum of downloaded file
       → applies delta only if checksum matches
```

- Updates are served over HTTPS — TLS provides transport integrity.
- The SHA-256 checksum guards against file corruption or truncated downloads.
- **Phase 2:** Ed25519 signing will be added to protect against a compromised Supabase Storage account. This requires an offline signing station and CI key management, deferred from Phase 1 to reduce initial complexity.
- If checksum verification fails, the client discards the payload and retains the current DB version.

---

- Edge Functions are the only entry point — clients never touch the DB directly.
- RLS is enabled as a second line of defence, even though direct access is not expected.
- Rate limiting is enforced inside Edge Functions per `device_token_hash` before any DB write.
- `reporter_deduplication` is checked on every `POST /report` — if the `(number_hash, device_token_hash)` pair already exists, the report is rejected before incrementing `unique_reporters`. This is the primary gate against single-source reputation bombing.
