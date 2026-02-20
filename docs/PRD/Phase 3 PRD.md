# CallGuard – Phase 3 Detailed PRD

## 0. Trigger Conditions

Phase 3 is initiated when the following criteria are met:

* 10,000+ active users
* Phase 2 behavioral detection validated in production (false positive rate still below 1%)
* Reputation system integrity confirmed (no coordinated abuse incidents at scale)

---

## 1. Phase 3 Objective

Expand CallGuard from an intelligent spam blocker into a scalable privacy ecosystem while maintaining strict privacy-first principles.

Phase 3 focuses on secure cloud capabilities, family protection features, stronger abuse resistance, and infrastructure hardening for growth.

---

## 2. Strategic Goals

* Enable secure cross-device protection without compromising privacy.
* Strengthen reputation system integrity at scale.
* Improve operational resilience and performance.
* Prepare infrastructure for large user growth.

---

## 3. Encrypted Cloud Sync (Optional Feature)

### 3.1 Objective

Allow users to back up and restore personal blocklists securely without exposing raw phone numbers to the backend.

### 3.2 Functional Requirements

* Client-side encryption of blocklist before upload.
* Backend stores encrypted blob only — no raw numbers ever reach the server.
* Cloud sync must be optional and disabled by default.
* User can delete cloud backup at any time.

**Recommended approach — Google Account-backed key store:**
Derive the encryption key from a Google Account credential using Android's `AccountManager` + a server-side key derivation step. Users see "Sign in with Google to back up and restore your list." On a new device, they sign in with the same Google Account to restore. This delegates key management to a system users already understand. No raw encryption key is exposed to the user.

**Alternative — local file export/import:**
A simpler alternative is to skip cloud sync entirely in Phase 3 and provide a local backup file (AES-256 encrypted, user sets a PIN to encrypt/decrypt). The user exports the file manually (to Google Drive, WhatsApp, etc.) and imports on a new device. No backend storage required. This is lower development effort and eliminates key management complexity entirely.

**Do not implement:** an irrecoverable key model where the backup cannot be restored without a passphrase the user must remember and record. In practice, mainstream users will not save a passphrase, will fail to restore on a new phone, and will attribute the data loss to the app. The feature must be designed around how real users behave, not how engineers expect users to behave.

**Decision required before Phase 3 build:** choose between Google Account-backed sync or local file export/import. The chosen approach must be documented in the Phase 3 kickoff.

### 3.3 Privacy Constraints

* Backend must never store raw numbers.
* If Google Account approach is used: the server must store only the encrypted blob; the Google Account credential is never transmitted to Supabase.
* If local file approach is used: no backend storage is involved.

---

## 4. Family Protection Mode

### 4.1 Objective

Allow technically skilled users to protect family members (e.g., elderly parents) without sharing contacts or sensitive data.

### 4.2 Functional Requirements

* Guardian device generates a time-limited anonymous pairing token (UUID, no user identity attached). Token expires after 10 minutes if unused.
* Token is QR-encoded and scanned by the dependent device. Once scanned, pairing is complete.
* Both devices store the token locally. Rules are synced via Supabase keyed on the `HMAC-SHA256(token)` — no names, accounts, or phone numbers linked.
* Guardian device can push updated prefix rules and block preferences to paired devices.
* User can unpair at any time, which removes the sync token from both devices and Supabase. After unpairing, no further rule propagation occurs.
* Sync protection rules (not contacts).
* No call audio, call logs, or contact lists leave either device.

**Family sync backend schema (Supabase):**
```sql
family_sync_rules (
    token_hash       TEXT NOT NULL,        ← HMAC-SHA256 of pairing token
    rule_type        TEXT NOT NULL,        ← "prefix" | "preference"
    rule_payload     JSONB NOT NULL,       ← encrypted rule data
    updated_at       TIMESTAMP DEFAULT now(),
    PRIMARY KEY (token_hash, rule_type)
)
```
Token hashes are deleted when the guardian initiates unpair. The table stores no user identity — only the token hash and rule payload.

### 4.3 Privacy Constraints

* No contact sharing between family accounts.
* No call logs shared.
* No surveillance functionality.

---

## 5. Reputation System Hardening

### 5.1 Advanced Abuse Detection

* Detect abnormal reporting spikes.
* Identify coordinated malicious reporting patterns.
* Automatically dampen suspicious reputation inflation.

### 5.2 Trust Scoring Refinement

* Improve trust weight calculation.
* Detect low-quality reporters.
* Gradually reduce influence of abusive accounts.

### 5.3 Confidence Stabilization

* Implement dynamic threshold adjustment.
* Improve decay algorithms.
* Prevent rapid oscillation of classification.

---

## 6. Infrastructure Scaling & Optimization

### 6.1 Database Optimization

* Partition hash-based reputation storage.
* Optimize index performance.
* Implement caching for frequent lookups.

### 6.2 Delta Update Optimization

* Smarter differential updates for seed database.
* Reduce payload size.
* Schedule updates efficiently.

### 6.3 Monitoring & Observability

* Privacy-safe monitoring dashboards.
* Abuse detection alerts.
* Reputation anomaly monitoring.

---

## 7. Operational Governance

* Formalize spam classification thresholds.
* Document internal review guidelines.
* Create appeal and correction mechanism.
* Define moderation escalation process.

---

## 8. Performance Requirements

* Cloud sync must not affect call screening latency.
* Reputation lookup must remain within safe response window.
* Backend must scale to support 100k+ active users.

---

## 9. Data Governance (Phase 3 Additions)

* Encrypted data retention policy defined.
* Clear deletion mechanisms for cloud backups.
* Strict separation of reputation metadata and encrypted user data.
* No expansion of personal data collection.

---

## 10. Testing Plan

* Simulate high-volume reputation updates.
* Test encrypted backup/restore flow.
* Validate QR pairing security.
* Stress-test abuse detection mechanisms.
* Confirm no privacy regressions.

---

## 11. Phase 3 Success Criteria

* Stable encrypted sync functionality.
* Family mode adoption among active users.
* Reduced abuse incidents in reputation system.
* Backend stability at increased scale.

---

## 12. Out of Scope (Phase 3)

* Carrier-level API integrations.
* Telecom operator partnerships.
* Full enterprise product line.
* Large-scale machine learning training pipelines.

Phase 3 transforms CallGuard into a hardened, scalable privacy infrastructure while preserving the original mission: protection without surveillance.
