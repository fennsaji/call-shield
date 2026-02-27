# Google Play Billing Testing Setup

Guide for testing `callshield_pro_annual`, `callshield_pro_monthly`, and `callshield_pro_lifetime` purchases without real charges.

---

## Prerequisites Checklist

### Play Console (human steps — one-time setup)

- [ ] **1. Activate all 3 products** — Play Console > Monetize > Subscriptions / In-app products
  - `callshield_pro_annual` → status must be **Active** (not Draft)
  - `callshield_pro_monthly` → status must be **Active**
  - `callshield_pro_lifetime` → status must be **Active**
  - Draft products are invisible to BillingClient and will return empty `productDetailsList`

- [ ] **2. Add license testers** — Play Console > Settings > License testing
  - Add all tester Gmail addresses to an email list and Save
  - Your own publisher account is automatically a license tester
  - License testers get test payment instruments (no real charges)

- [ ] **3. Add testers to internal track** — Testing > Internal testing > Testers
  - Add the same Gmail addresses
  - Copy the **opt-in URL** and share with testers
  - Testers must open the URL on their Android device and click "Accept invitation"

- [ ] **4. Install Play Billing Lab** on test device(s)
  - App: [Play Billing Lab](https://play.google.com/store/apps/details?id=com.google.android.apps.play.billingtestcompanion)
  - Sign in with the license tester's Google account
  - Configurations expire after 2 hours

### Device Setup (per tester device)
- Physical Android device signed into the tester Gmail account
- App installed from Play Store via opt-in URL (or sideloaded debug APK for license testers)
- Only the first/primary Google account on the device is used for purchases

---

## How Test Payments Work

When a license tester initiates a purchase, the Google Play payment dialog shows test instruments instead of real payment methods. A notice is visible across the center of the dialog.

| Test Instrument | Behaviour |
|---|---|
| Test card, always approves | Purchase completes immediately |
| Test card, always declines | Purchase fails immediately |
| Slow card, approves after a few minutes | Enters PENDING state, then completes |
| Slow card, declines after a few minutes | Enters PENDING state, then fails |

No real money is charged. No taxes computed.

---

## Subscription Renewal Acceleration

For license testers, Google compresses billing periods:

| Production Period | Test Duration |
|---|---|
| 1 month | 5 minutes |
| 1 year | **30 minutes** |
| Free trial | 3 minutes |
| Grace period | 5 minutes |
| Account hold | 10 minutes |

Subscriptions auto-renew a **maximum of 6 times** then expire naturally.

**Example — testing `callshield_pro_annual` full lifecycle:**
| Time | Event |
|---|---|
| 0:00 | Subscribe |
| 0:30 | Renewal #1 |
| 1:00 | Renewal #2 |
| ... | ... |
| 3:00 | Renewal #6 (final) |
| 3:30 | Expires → user loses Pro access |

---

## Testing Checklist for CallShield

### Purchase flows
- [ ] Purchase `callshield_pro_annual` → Pro features unlock, `isPro = true`, `planType = PRO_ANNUAL`
- [ ] Purchase `callshield_pro_monthly` → Pro features unlock, `isPro = true`, `planType = PRO_MONTHLY`
- [ ] Purchase `callshield_pro_lifetime` (one-time INAPP) → Pro features unlock, `isPro = true`, `planType = PRO_LIFETIME`
- [ ] Purchase with "always declines" card → purchase dialog closes, no Pro access granted
- [ ] Purchase with "slow card, approves" → `hasPendingPurchase = true` during wait, then Pro unlocks

### Acknowledgment
- [ ] Purchase completes → acknowledgment fires within seconds (check no refund after 5 min test window)
- [ ] Kill app immediately after purchase → restart app → `refreshSubscriptionStatus()` re-acknowledges on next launch

### Subscription lifecycle
- [ ] Let annual subscription renew 2-3 times → access remains continuous
- [ ] Cancel subscription (Play Store > Subscriptions > Cancel) → access continues until next renewal, then `isPro = false`
- [ ] Let subscription expire after 6 renewals → `isPro = false`, paywall shows on next Pro feature access

### Grace period & account hold
- [ ] Subscribe → switch test card to "always declines" before next renewal → grace period (5 min) → `isPro` still true
- [ ] Let grace period expire → account hold (10 min) → `isPro = false`
- [ ] Switch back to "always approves" during account hold → recovery → `isPro = true` restored

### Restore & resume
- [ ] Uninstall and reinstall app → `restorePurchase()` → existing subscription restored
- [ ] Open paywall → background app → go to Play Store → redeem promo code → return to app → `ON_RESUME` detects it

### Intro offer
- [ ] If annual plan has an intro offer configured in Play Console → paywall shows intro price with strikethrough base price

### Promo codes
- [ ] Redeem a Play Console promo code for `callshield_pro_annual` → Pro unlocks after returning to app

---

## Play Billing Lab: Advanced Testing

Play Billing Lab enables these scenarios without waiting for the compressed timelines:

1. **Force subscription state transitions** — instantly move to grace period, account hold, or trigger renewal
2. **Repeat free trial** — test intro offer eligibility repeatedly with the same account
3. **Simulate BillingResult error codes** — configure specific APIs to return `BILLING_UNAVAILABLE`, `SERVICE_DISCONNECTED`, etc.
4. **Change Play country** — test regional pricing without VPN

**To force a state transition:**
1. Open Play Billing Lab
2. Tap "Subscription settings" > Manage
3. Select the active test subscription
4. Choose desired state

Note: Billing Lab overrides require the app to be built with `enableBillingOverridesTesting = true` in the manifest. This is already configured in `src/debug/AndroidManifest.xml` (requires billing-ktx 7.1.1+, currently set in `libs.versions.toml`). **These tags are never in release builds.**

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `queryProducts()` returns empty list | Products are in Draft state | Activate products in Play Console |
| No test card shown during purchase | Account is not a license tester | Add Gmail to Settings > License testing |
| "Item not available" error | Product ID mismatch or product not active | Check product IDs match exactly |
| Can't install from opt-in URL | Tester not added to internal track | Add to Testing > Internal testing > Testers |
| Purchase succeeds but `isPro` stays false | Acknowledgment pending or `refreshSubscriptionStatus()` not called | Check `onPurchasesUpdated` fires; call `connect()` first |
| BillingClient returns `SERVICE_UNAVAILABLE` | No internet or Play Services issue | Check connectivity; restart Play Services |

---

## Key Code Locations

| File | Purpose |
|---|---|
| `billing/BillingManager.kt` | Core billing logic, acknowledgment, `refreshSubscriptionStatus()` |
| `ui/screens/paywall/PaywallViewModel.kt` | Loads products, handles purchase, `refreshOnResume()` |
| `ui/screens/paywall/PaywallScreen.kt` | UI with dynamic pricing, intro offers, promo code redemption |
| `src/debug/AndroidManifest.xml` | Billing Lab override meta-data (debug only) |
| `gradle/libs.versions.toml` | `billing = "7.1.1"` |
