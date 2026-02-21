# CallShield – Investor Deck

## 1. Executive Summary

CallShield is a spam and scam call protection app built for the Indian market. You decide what gets through. We never see who’s calling.

Unlike Truecaller and carrier-level solutions, CallShield gives users full control — on-device intelligence, anonymous hash-based reputation, and zero contact upload. No ads. No surveillance.

Our mission: build India’s most trusted user-controlled protection layer against phone scams.

---

## 2. Problem

India faces a massive and growing spam and scam call problem:

* Billions of spam calls annually
* Rising digital payment fraud
* Increasing impersonation and loan scams

Current solutions have limitations:

* Heavy reliance on contact uploads
* Large centralized data harvesting
* Ad-driven monetization models
* Limited transparency in how numbers are classified
* Slow and fragmented regulatory reporting

There is no widely adopted privacy-first alternative focused on intelligent detection.

---

## 3. Solution

CallShield provides:

* On-device spam detection
* Prefix-based blocking (e.g., promotional number series)
* Anonymous, hash-based reputation system
* Transparent spam confidence scoring
* One-tap regulatory complaint assistance
* No contact upload
* No ads

The product works even with a small user base because detection does not depend solely on massive centralized databases.

---

## 4. Product Overview

### Core Features (Phase 1)

* Trust-model onboarding — explains exactly what we collect and never collect, before permissions are requested
* Android Call Screening integration — block, silence, or reject spam before it rings
* Missed call / callback risk indicator — warns users before they call back unknown numbers
* Local seed spam database (TRAI, DoT, RBI sourced)
* Manual blocking, whitelist, and prefix rules
* Anonymous spam reporting (hash-based, no raw numbers)
* Lightweight weekly scam digest — retention mechanism, surfaces active scam types
* Privacy trust statement — one screen, plain language, deletion option

### Future Enhancements

* Behavioral detection engine — Phase 2
* SMS scam pattern detection (UPI fraud) — Phase 2
* TRAI Quick Report integration — Phase 2
* Weighted reputation scoring — Phase 2
* Full Family Protection Mode (QR pairing, cross-device sync) — Phase 3
* Encrypted cloud sync — Phase 3
* Lightweight explainable ML — Phase 4
* Enterprise fraud protection extensions — Phase 4

---

## 5. Market Opportunity

India:

* Over 1 billion mobile users
* ~95% Android market share
* Rapid digital payments growth
* Rising awareness of data privacy

Primary target segment (Phase 1):

* Urban professionals (25–45) who receive high spam call volumes and have disposable income — fastest path to conversion
* Adult children managing elderly parents' digital safety — highest willingness to pay, strong referral dynamics, underserved by existing solutions

Secondary segments (Phase 3+):

* Privacy-conscious smartphone users as awareness of DPDP Act grows
* Tier 2/3 city users (lifetime plan model, Phase 4)

Even capturing 0.1% of Android users represents significant scale.

---

## 6. Business Model

Freemium subscription model.

Free Tier:

* Seed DB detection (see if a number is known spam)
* Manual blocking and whitelist
* Prefix blocking
* Manual spam reporting

Pro Tier:

* ₹399/year (default — equivalent to ₹33/month)
* ₹49/month (secondary option)
* Auto-block high-confidence spam — calls blocked before they ring
* Advanced prefix rules
* Early access to seed DB delta updates

Paywall trigger: shown at the moment value is demonstrated — when a free user's call is silenced because it matched a spam number. "Pro would have blocked this before it rang."

Phase 3 addition: Family Plan (₹699/year, covers two devices). Intent waitlist captured at Phase 1 paywall.
Phase 4 addition: Lifetime option (₹599–799 one-time) for Tier 2/3 city segment.

Revenue scales with user trust and retention.
No ad-based monetization. Ever.

---

## 7. Competitive Positioning

CallShield differentiates by:

* User control — you decide what gets through, not the carrier or an algorithm you can't inspect
* No contact harvesting — architecturally impossible in our model, not just a policy promise
* On-device intelligence — blocking decisions happen on the device, not in a data center
* Transparent scoring — every flagging decision is explainable to the user
* Minimal data storage — hashed reputation only, verified by published architecture

We compete on trust and user agency, not database size.

Against Truecaller: they need your contacts to work. We work without them — and we can prove it technically, not just claim it.

Against carrier-level blocking (Airtel AI, TRAI mandates): carriers block what they decide. We let users block what they decide. This positioning survives any regulatory shift.

---

## 8. Technology Architecture

* Android native (Kotlin)
* Supabase backend for anonymous reputation storage
* Hash-based number indexing
* On-device rule engine
* No centralized personal data collection

The architecture is capital-efficient and scalable.

---

## 9. Go-To-Market Strategy

* Privacy-focused digital communities
* Cyber safety awareness campaigns
* Targeted digital ads
* Influencer partnerships in tech and security space
* Word-of-mouth via family protection positioning

---

## 10. Roadmap

Phase 1: MVP launch and validation
Phase 2: Behavioral intelligence layer
Phase 3: Privacy ecosystem expansion
Phase 4: Scale and partnerships

---

## 11. Financial Overview (Early Stage Projection)

Annual plan as primary (₹399/year):

* 10,000 installs → 3% conversion → 300 paying users → ~₹120,000 ARR (~₹10,000 MRR equivalent)
* 50,000 installs → ~₹600,000 ARR (~₹50,000 MRR equivalent)

Monthly plan (₹49/month) as secondary contributes additional MRR on top.

Key assumption: annual plan as default paywall option improves conversion rate vs. monthly-first presentation, and significantly improves LTV and revenue predictability.

Infrastructure costs remain low due to minimal backend data storage — the hash-based architecture is capital-efficient by design.

---

## 12. Vision

CallShield aims to become a distributed reputation and scam detection layer for India’s telecom ecosystem.

Built on intelligence.
Built on transparency.
Built without surveillance.
