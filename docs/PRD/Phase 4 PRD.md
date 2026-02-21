# CallShield – Phase 4 Detailed PRD

## 1. Phase 4 Objective

Scale CallShield from a growing privacy-first application into a mature telecom protection platform capable of supporting large user volumes, strategic partnerships, and long-term sustainability.

Phase 4 focuses on scale, partnerships, advanced intelligence refinement, and business expansion while maintaining strict privacy guarantees.

---

## 1a. Trigger Conditions

Phase 4 is initiated when the following criteria are met:

* 50,000+ active users
* 4.5+ app store rating
* Sustainable subscription revenue

## 2. Strategic Goals

* Support 100,000+ active users reliably.
* Strengthen market position as India’s trusted privacy-first spam shield.
* Evaluate carrier and regulatory partnership opportunities (BD effort, not engineering).
* Improve detection quality if a measurable gap exists that rule-based methods cannot close.
* Evaluate iOS companion app (offline seed DB + block rule sync) — this is a new product requiring a new codebase, not a Phase 4 feature of the Android app.

---

## 3. Advanced Intelligence Layer (Exploratory Research Direction Only)

On-device ML is a possible long-term direction, not a committed Phase 4 feature. It should be evaluated at Phase 4 kickoff based on whether the marginal detection improvement over Phase 2 behavioral rules justifies the full pipeline required:

- A labeled training dataset (who labels it, at what cost, with what quality controls?)
- A training and evaluation pipeline
- Model compression for on-device deployment (TFLite / ONNX)
- A model update delivery channel (models are significantly larger than DB deltas)
- An explainability layer for user-facing classification reasons

If Phase 2 behavioral detection + a well-maintained seed DB achieves acceptable accuracy at 50k users, on-device ML should not be built. Build it only if a specific, measurable accuracy gap exists that rule-based approaches cannot close.

**Privacy constraints if pursued:**
* Metadata-based features only — no call audio, no contact scraping.
* No personal user identifiers in training data.
* All training datasets must be anonymized and aggregated.
* Users must be able to see simplified reasoning for any ML-influenced classification.

---

## 4. Carrier & Regulatory Partnerships (Business Development, Not Engineering)

Carrier integrations (Airtel, Jio, Vi, TRAI) require business development relationships, regulatory approvals, and legal agreements that take years to establish and are outside the engineering team's control. This is not scoped as an engineering deliverable.

If carrier partnerships become commercially viable, they will be pursued as a business development effort with dedicated resources. Engineering work (e.g., an API integration) would follow only after a signed agreement and API specification are in hand.

**Phase 4 action:** evaluate carrier partnership opportunities; engage in exploratory conversations with regulatory bodies. Do not build integration code without a confirmed partner.

---

## 5. Enterprise Offering (Out of Scope — Evaluate as Separate Product)

An enterprise anti-scam product (bulk dashboard, centralized policy, SLAs) is a different product requiring a different sales motion, support model, infrastructure (multi-tenancy, SSO, audit logs), and pricing. It cannot be built as a Phase 4 extension of the consumer app without effectively starting a second product line.

If enterprise protection is strategically pursued after Phase 2 validation, it should be scoped, resourced, and evaluated as a standalone product — not as a feature of the consumer app.

---

## 6. Infrastructure Scaling

### 6.0 Supabase Scaling Ceiling

Supabase (managed Postgres + Edge Functions) is sufficient for Phases 1–3. At Phase 4 scale (100k+ active users, high-frequency reputation lookups during peak call hours), the following must be evaluated:

* **Read throughput:** `GET /reputation` is the hottest endpoint. A Redis caching layer in front of Postgres should be introduced before hitting Supabase connection limits.
* **Edge Function cold starts:** If lookup latency degrades under load, consider migrating hot-path Edge Functions to a dedicated low-latency service (e.g., Cloudflare Workers or a self-hosted Node service).
* **Write throughput:** `report_events` and `reporter_deduplication` inserts. Partition `report_events` by month at this scale.
* **Migration path:** The `report_events` append-only design (established in Phase 1) is the key enabler for migrating to a different storage backend without data loss — scores can be fully recomputed from events.

### 6.1 Backend Scaling

* Horizontal scaling for reputation database.
* Load balancing for lookup APIs.
* Caching layer for frequent hash lookups (Redis or Supabase Realtime cache).

### 6.2 Performance Targets

* Lookup latency under defined threshold.
* Zero blocking delays due to backend lag.
* System capable of handling traffic spikes.

### 6.3 Observability & Monitoring

* Real-time health dashboards.
* Reputation anomaly alerts.
* Abuse pattern detection alerts.

---

## 7. Governance & Compliance Hardening

* Formalize internal data access controls.
* Regular privacy audits.
* Annual policy review process.
* Transparent public privacy reports.

---

## 8. Brand & Market Expansion

### 8.1 Public Trust Campaign

* Publish transparency reports.
* Share detection statistics.
* Educate users on scam prevention.

### 8.2 App Store Optimization

* Improve store listing performance.
* Expand into additional regions (if compliant).

---

## 9. Performance & Stability Requirements

* Maintain sub-second detection decisions.
* Ensure model inference does not degrade performance.
* Maintain low battery usage at scale.

---

## 10. Data Governance (Phase 4 Additions)

* Maintain strict data minimization policy.
* Formalize retention limits for aggregated metadata.
* Ensure deletion mechanisms remain accessible.
* No expansion into contact harvesting or ad-based monetization.

---

## 11. Testing & Validation Plan

* Large-scale load testing.
* Model accuracy validation.
* Bias testing in detection logic.
* Security audit before major partnerships.

---

## 12. Phase 4 Success Criteria

* Stable performance at 100k+ active users.
* Sustained subscription growth.
* Increased brand trust indicators.
* Successful partnership pilots (if pursued).

---

## 13. Out of Scope (Phase 4)

* Selling user data.
* Ad-based monetization.
* Behavioral surveillance models.

Phase 4 positions CallShield as a mature privacy-first telecom protection platform capable of long-term growth and strategic collaboration without compromising its foundational principles.
