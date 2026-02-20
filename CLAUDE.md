# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**CallGuard** is a privacy-first spam and scam call protection app targeting the Indian market. The core differentiator is on-device intelligence with an anonymous, hash-based reputation system — no contact uploads, no ads, no raw phone number storage.

The repository is currently in a planning/documentation phase. All code will be **Android-native (Kotlin)** with a **Supabase** backend.

## Planned Tech Stack

- **Platform:** Android (Kotlin), targeting Indian OEMs (MIUI, Samsung, Realme)
- **Backend:** Supabase (anonymous reputation storage, hash-based lookups)
- **Key Android API:** Call Screening API (app must register as default call screening service)
- **Monetization:** Google Play billing (freemium — ₹399/year primary, ₹49/month secondary Pro tier)

## Architecture Principles

1. **Privacy by default** — no raw phone numbers stored anywhere on the backend; all numbers are hashed client-side before any network call.
2. **On-device first, cloud second** — blocking decisions (local blocklist, prefix rules, seed DB lookup) must work fully offline; backend reputation is a supplementary signal.
3. **Hash-based reputation** — backend stores only `number_hash`, `report_count`, `unique_reporters_count`, `category`, `confidence_score`, `last_reported_at`.
4. **Transparent scoring** — every flagging decision must be explainable to the user (prefix match, seed DB hit, reputation count). No black-box decisions.
5. **Strict data minimization** — never collect contacts, call logs, SMS content, device fingerprint, or advertising IDs.

## Call Decision Flow

Incoming call → local blocklist check → prefix rule check → local seed DB lookup → backend reputation lookup (hashed) → decision: Allow / Silence / Reject → user can view reason.

## Phase-Based Development Scope

See `docs/PRD/` for detailed per-phase requirements. Summary:

| Phase | Focus | Key additions |
|-------|-------|--------------|
| 1 (MVP) | Rule-based blocking | Trust-model onboarding, Call Screening API, seed DB, prefix rules, anonymous reporting, missed-call callback risk indicator, lightweight scam digest, value-moment paywall |
| 2 | Intelligence layer | On-device behavioral detection (call frequency, burst calls, short-ring patterns), weighted/decaying reputation scoring, TRAI Quick Report, SMS scam pattern detection |
| 3 | Privacy ecosystem | Optional client-side-encrypted cloud sync, family protection (QR pairing), reputation abuse hardening, backend scaling |
| 4 | Scale & partnerships | Lightweight explainable ML (metadata-only, no audio), carrier/regulatory integrations (exploratory), enterprise offering |

## Key Constraints

- **Auto-block is a Pro feature only** and must always be toggleable with visible notification and easy "Not Spam" override.
- **Reputation requires multiple unique reporters** before high-confidence classification — never escalate on a single report.
- **Call screening decision must complete within Android's time window** — backend lookups must not block the decision path.
- **OEM compatibility** is a first-class concern: handle MIUI, Samsung, and Realme battery restrictions; provide in-app guidance for battery optimization settings.
- **Google Play compliance** for call screening permissions and Data Safety Form accuracy is required before launch.

## Subscription Tiers

**Free:** manual blocking, prefix blocking, basic seed DB detection, manual spam reporting.
**Pro (₹399/year or ₹49/month):** auto-block high-confidence spam (before ringing), advanced prefix rules, early DB delta updates.
**Family Plan (₹699/year, Phase 3):** covers two devices via QR pairing.
**Lifetime (₹599–799 one-time, Phase 4):** for Tier 2/3 city segment.
