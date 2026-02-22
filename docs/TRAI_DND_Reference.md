# TRAI DND Reference

Source: Telecom Commercial Communications Customer Preference (Second Amendment) Regulations, 2025 (1 of 2025)
Published: 12 February 2025 | Comes into force: 30 days after Gazette publication

---

## Operator-Specific Commands

Commands differ by operator (confirmed from user testing on Jio + Airtel; research for Vi/BSNL):

| Action | Jio | Airtel | Vi | BSNL |
|---|---|---|---|---|
| Full DND | `FULLY BLOCK` | `START 0` | `FULLY BLOCK` | `START 0` |
| Block categories | `BLOCK 1,4` | `START 1,4` | `START 1,4` | `START 1,4` |
| Block promo only | `BLOCK PROMO` | `BLOCK PROMO` | `BLOCK PROMO` | `BLOCK PROMO` |
| Deactivate all | `UNBLOCK ALL` | `UNBLOCK ALL` | `UNBLOCK ALL` | `STOP DND` |

> `BLOCK PROMO` is a 2025 TRAI regulation standard command — should work across operators.

---

## SMS Commands to 1909

### Activation

| Command | Effect | Operator support |
|---|---|---|
| `START 0` | Full DND — block all promotional | Universal (Jio, Airtel, Vi, BSNL) |
| `FULLY BLOCK` | Full DND — same as START 0 | Vi/Vodafone Idea confirmed; may vary |
| `BLOCK PROMO` | Promotional only — explicit consent service messages still delivered | Listed in 2025 regulation; may vary by operator |
| `START 1` – `START 7` | Block individual category | Universal |
| `BLOCK 1` – `BLOCK 8` | Block individual category (2025 amendment syntax) | Listed in 2025 regulation |
| `START 1,4` | Block multiple categories | Universal |

USSD alternatives: `*1909*0#` (full block), `*1909*50#` (promo only), `*1909*1#` – `*1909*8#` (by category)

### Deactivation (confirmed from actual 1909 reply)

| Command | Effect |
|---|---|
| `UNBLOCK ALL` | Remove from DND completely ✅ confirmed |
| `UNBLOCK SERVICE` | Unblock service category |
| `UNBLOCK 91,92,93` | Unblock specific categories (9x codes — see below) |
| `STOP 91,92,93` | Same as UNBLOCK 91,92,93 |

**Unblock category codes** (different from block codes):

| Unblock code | Category |
|---|---|
| 91 | Banking / Insurance / Financial |
| 92 | Real Estate |
| 93 | Education |
| 94 | Health |
| 95 | Consumer Goods / Automobiles |
| 96 | Communication / Broadcasting / Entertainment / IT |
| 97 | Tourism / Leisure |
| 98 | Food and Beverages |

> `STOP` alone and `STOP 0` are **not confirmed** — use `UNBLOCK ALL` for full deactivation.

Deactivation takes effect within **7 days** (TCCCPR 2018).

### Status Check

**`STATUS` via SMS is not mentioned in the official 1909 reply — not a supported command.**

Reliable methods:
1. **Call 1909** (toll-free IVR)
2. **TRAI DND 2.0 app** — Google Play Store
3. **Online:** ndnc.net

---

## FULLY BLOCK vs BLOCK PROMO — Precise Difference

Both block promotional calls and SMS. The difference is what happens to **service messages where you gave explicit consent**.

**Explicit consent** = you actively agreed (ticked a checkbox, replied YES to a consent SMS) to receive updates from a specific brand.

**Inferred consent** = you are an existing customer and the sender can reasonably assume you need operational updates (e.g. your bank sending balance alerts, your e-commerce order tracking).

| Message type | FULLY BLOCK | BLOCK PROMO |
|---|---|---|
| Promotional (any) | ❌ Blocked | ❌ Blocked |
| Service — explicit consent (newsletter you signed up for, loyalty program) | ❌ Blocked | ✅ Delivered |
| Service — inferred consent (bank balance, delivery alert) | ✅ Delivered | ✅ Delivered |
| Transactional (OTP, transaction confirmation) | ✅ Delivered | ✅ Delivered |
| Government messages | ✅ Delivered | ✅ Delivered |

> **Regulation source — Note 3 (Schedule II):**
> "FULLY BLOCK option shall put the Customer in Fully Blocked state and block service types of Commercial Communications requiring Explicit Consent **as well as** promotional types of Commercial Communications for all categories."
>
> **Note 4:**
> "BLOCK PROMO option shall block **only** promotional types of Commercial Communications for all categories… except service and transaction type of Commercial Communications and Government Communication."

**For most users:** FULLY BLOCK is the right choice. Transactional messages (OTPs, bank alerts) bypass both options entirely — they are never affected by DND.

---

## DND Category Codes (Schedule II, 2025)

| Code | Category |
|---|---|
| 1 | Banking / Insurance / Financial Products / Credit Cards |
| 2 | Real Estate |
| 3 | Education |
| 4 | Health |
| 5 | Consumer Goods / Automobiles |
| 6 | Communication / Broadcasting / Entertainment / IT |
| 7 | Tourism / Leisure |
| 8 | Food and Beverages *(added in 2025 amendment)* |

---

## Complaint Format

To file a UCC complaint via SMS to 1909:

```
<Brief description of call/SMS>, <sender's phone number>, <dd/mm/yy>
```

Example:
```
Received unsolicited spam call, 9876543210, 22/02/26
```

**Valid complaint requirements (Regulation 6, sub-regulation 5a):**
- Sender's mobile number or header
- Date of the UCC (within **7 days** of receiving it)
- Brief description of the communication

The name of the business and purpose are optional (not mandatory for complaint registration).

---

## Regulation 34A — Compliance Note for CallGuard

> "No Call Management Application or similar services shall tag, block, filter, or restrict incoming calls or messages originating from the **designated number series assigned for commercial communications** as well as communication sent by the Government."

**Implication:** CallGuard must not blanket-block the 140xxxxxxxx telemarketer number series.

**Safe harbour (proviso):** "consumers shall have the right to **individually** manage their own call preferences through such Call Management Applications."

CallGuard's per-number spam blocking (user-initiated or community-flagged) is compliant. Blanket prefix blocking of `140*` would not be.

---

## Key Definitions

**Unsolicited Commercial Communication (UCC):** Any commercial communication that is neither as per the consent nor the registered preferences of the recipient. Excludes transactional, service, and government messages.

**Transactional message:** Sent in response to a customer-initiated transaction within 30 minutes (OTPs, transaction confirmations, refund info). Never requires consent. Cannot be blocked by DND.

**Service message (inferred consent):** Sent to existing customers for product/service information (warranty, delivery, balance alerts). Bypasses DND.

**Service message (explicit consent):** Sent after customer actively gave permission. Blocked by FULLY BLOCK, not by BLOCK PROMO.

**Promotional message:** Any commercial communication containing promotional material or advertisement. Blocked by all DND options. Must include an opt-out mechanism.
