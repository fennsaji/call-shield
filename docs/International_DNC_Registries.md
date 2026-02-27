# International Do-Not-Call / DND Registries

Research note for potential future expansion of DND Management beyond India.

---

## India (current)
- **Regulator:** TRAI
- **Registry:** DND / TCCCPR
- **How it works:** SMS to 1909; category-based opt-out (8 TRAI categories)
- **Enforcement:** 7-day complaint window; TSPs can be penalised
- **App integration:** SMS intent to 1909 or launch `trai.gov.in.dnd` package
- **Reference:** `docs/TRAI_DND_Reference.md`, `docs/TRAI_Regulation.pdf`

---

## United States
- **Regulator:** FTC
- **Registry:** National Do Not Call Registry
- **Website:** donotcall.gov
- **How it works:** Phone number registration; telemarketers must scrub lists every 31 days
- **Enforcement:** FTC civil penalties up to $51,744 per violation
- **App integration:** No public API; web registration only

## Canada
- **Regulator:** CRTC
- **Registry:** National Do Not Call List (DNCL)
- **Website:** lnnte-dncl.gc.ca
- **How it works:** Similar to US; 3-year registration validity
- **Enforcement:** CRTC fines
- **App integration:** No public API

## United Kingdom
- **Regulator:** Ofcom / ICO
- **Registry:** Telephone Preference Service (TPS)
- **Website:** tpsonline.org.uk
- **How it works:** Landline and mobile registration; GDPR-backed enforcement
- **Enforcement:** ICO fines
- **App integration:** No public API

## Australia
- **Regulator:** ACMA
- **Registry:** Do Not Call Register
- **Website:** donotcall.gov.au
- **How it works:** 8-year registration; covers voice + fax
- **Enforcement:** ACMA infringement notices
- **App integration:** No public API

## Singapore
- **Regulator:** PDPC
- **Registry:** Do Not Call (DNC) Registry
- **Website:** dnc.pdpc.gov.sg
- **How it works:** Separate registrations for voice, SMS, fax
- **Enforcement:** PDPA fines up to SGD 1M
- **App integration:** No public API

## France
- **Regulator:** DGCCRF
- **Registry:** Bloctel
- **Website:** bloctel.gouv.fr
- **How it works:** Free registration; valid indefinitely
- **Enforcement:** €375,000 fine per violation
- **App integration:** No public API

## Germany
- **Regulator:** BNetzA / BfDI
- **Registry:** No central registry
- **How it works:** GDPR opt-in model — cold calls require prior consent by default
- **Enforcement:** GDPR fines (up to 4% global revenue)
- **App integration:** N/A

## Brazil
- **Regulator:** Procon (state-level) / SENACON (federal)
- **Registry:** "Não Perturbe" — varies by state (São Paulo most mature)
- **Website:** naopertube.procon.sp.gov.br (SP)
- **How it works:** State-level; inconsistent national coverage
- **Enforcement:** Variable
- **App integration:** No public API

---

## Implementation Notes for Future Expansion

| Country | Integration Approach |
|---------|---------------------|
| India | SMS to 1909 or launch DND app — already implemented |
| US / Canada / UK / AU | Deep-link to registration website (WebView or browser intent) |
| Singapore | Deep-link to website |
| Germany | Not applicable — GDPR opt-in model, no registry to register with |

Most countries outside India have **no programmatic API** — the best an app can do is:
1. Detect the user's country (via `HomeCountryProvider.isoCode`)
2. Offer a "Register on [Country] DNC" button that opens the official website in browser

This is simpler than India's SMS integration but still useful as a guided action.

---

## TODO
- [ ] Research if any countries have opened programmatic APIs (check again before implementing)
- [ ] Decide on scope: India-only forever, or expand to US/UK/AU in a future phase
- [ ] If expanding: add country-specific registry URLs to a config file
