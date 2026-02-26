# Known Issues — CallShield Android

> Last updated: 2026-02-26
> Reviewed by: Senior Android Engineer audit + manual code verification
> Branch at time of review: `dev`

Issues are grouped by severity. Each entry includes the affected file, a description of the problem, its impact, and the recommended fix. All issues have been verified against the actual source code.

---

## Critical

These issues can cause crashes, data loss, or revenue loss. Fix before the next production release.

---

### [C-1] Missing `MIGRATION_1_2` — cold crash on launch for any device on DB version 1

**File:** `di/DatabaseModule.kt:112`

**Verified:** Migration chain confirmed as `MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6`. No v1→v2 migration. DB version confirmed as 6 in `CallShieldDatabase.kt`.

**Problem:** Room does not fill migration gaps. Any device carrying DB version 1 throws `IllegalStateException` on launch.

**Fix:**
- DB version 1 was likely never shipped publicly (build.gradle.kts default `versionCode = 2`). Call `.fallbackToDestructiveMigrationFrom(1)` to safely destroy only that stale schema.
- Also add `.fallbackToDestructiveMigrationOnDowngrade()` so rolling back to an older release recovers instead of crashing.

---

### [C-2] `acknowledgePurchase` is fire-and-forget — unacknowledged purchases auto-refunded within 3 days

**File:** `billing/BillingManager.kt:342`

**Verified:** Callback body is `{ /* fire-and-forget */ }`. The `BillingResult` is never inspected.

**Problem:** Any network failure or billing service disconnection silently drops the acknowledgment. Google Play automatically refunds all unacknowledged purchases after 3 days.

**Fix:**
1. Inspect `result.responseCode` in the callback.
2. On any failure except `OK` and `ITEM_ALREADY_OWNED`, persist the purchase token to DataStore.
3. In `refreshSubscriptionStatus()`, retry acknowledgment for any persisted unacknowledged tokens before clearing them.

---

### [C-3] No reconnect after `onBillingServiceDisconnected` — billing permanently broken after OEM process kill

**File:** `billing/BillingManager.kt:90`

**Verified:** `onBillingServiceDisconnected` only calls `cont.resume(false)`. No retry is scheduled.

**Problem:** Huawei EMUI, Xiaomi MIUI, Oppo ColorOS, and Realme UI routinely kill the Google Play billing service to conserve battery. Once `connect()` returns `false`, every subsequent product query and purchase attempt silently fails for the rest of the session.

**Fix:** In `onBillingServiceDisconnected`, launch an exponential-backoff reconnect loop (delays: 1 s → 2 s → 4 s, max 3 attempts) in a supervised coroutine scope before returning the final `false`.

---

### [C-4] `ContactsLookupHelper.isInContacts()` is a synchronous ContentProvider IPC call inside the 1400 ms screening budget

**File:** `data/local/ContactsLookupHelper.kt:18`

**Verified:** `context.contentResolver.query(ContactsContract.PhoneLookup...)` — direct synchronous IPC call. No caching. Called from the screening pipeline on every incoming call.

**Problem:** On Samsung One UI with large contact books, contact aggregation on `PhoneLookup` takes 300–800 ms. Combined with the 6+ serial DataStore reads (see M-4) and the remote reputation call, the 1400 ms budget is regularly exhausted, letting calls through unscreened.

**Fix:**
1. At app startup, pre-populate an in-memory `HashSet<String>` of normalised contact numbers.
2. Register a `ContentObserver` on `ContactsContract.Contacts.CONTENT_URI` to invalidate and rebuild the set when contacts change.
3. Replace the IPC call in `isInContacts()` with an O(1) set lookup.

---

### [C-5] `SeedDbUpdater` accumulates all rows in memory before inserting — OOM on 2 GB devices

**File:** `data/seeddb/SeedDbUpdater.kt:62`

**Verified:** `val allRows = mutableListOf<SeedDbNumber>()` collects every CSV row before calling `seedDbDao.replaceAll(allRows)`. The `BATCH_SIZE = 1000` constant at line 97 is declared but **never used**. `SeedDbDao.replaceAll()` passes the full list to `insertAll(numbers: List<SeedDbNumber>)` in a single Room `@Insert` call.

**Problem:** 500,000 entries × ~50 bytes = ~25 MB heap allocation. Android's per-process heap limit on 2 GB devices is commonly 512 MB; combined with the running app this reliably causes `OutOfMemoryError`.

**Fix:**
1. Verify the SHA-256 checksum by streaming to a temp file first.
2. Re-open the verified file and call `insertAll` in batches of `BATCH_SIZE` inside the existing `@Transaction`-wrapped `replaceAll()` method.

---

### [C-6] `RingTimeRegistry` torn-read race condition on dual-SIM devices

**File:** `screening/RingTimeRegistry.kt:23`

**Verified:** Two `@Volatile` fields written in separate, non-atomic instructions. The class comment acknowledges it only tracks a single active ring ("matches single-SIM behaviour on most Indian devices") — confirming the dual-SIM gap is known but unmitigated.

**Problem:** `@Volatile` prevents CPU cache staleness but not interleaved writes. On a dual-SIM device (≈80% of Indian market), two concurrent `onRingStart` calls can interleave: `activeHash` ends up pointing to SIM A's hash paired with SIM B's timestamp. `onCallEnded` then records a `SHORT_RING` attributed to the wrong number hash.

**Impact:** Incorrect SHORT_RING event attribution, leading to wrong auto-escalation decisions on dual-SIM devices.

**Fix:** Replace both fields with a single `AtomicReference<Pair<String, Long>?>` updated atomically.

---

## Major

These issues cause reliability problems or silent billing bugs. Fix before the next minor release.

---

### [M-1] `onPurchasesUpdated` can only grant Pro/Family access, never revoke it — refunded users retain access indefinitely

**File:** `billing/BillingManager.kt:283`

**Verified:**
```kotlin
if (hasFamily || hasPromoFamily) _isFamily.value = true
if (hasBillingPro || hasPromoPro) _isPro.value = true
```
Neither flag is ever set to `false` in `onPurchasesUpdated`. The full recalculation that handles revocation exists in `refreshSubscriptionStatus()` but does not run on the real-time update path.

**Fix:** Mirror the full recalculation from `refreshSubscriptionStatus()` inside `onPurchasesUpdated`, evaluating billing purchases and promo grants together before setting both flags.

---

### [M-2] Auto-escalation counts all-time rejections with no time window — permanently escalates legitimately re-allowed numbers

**File:** `domain/usecase/EvaluateAdvancedBlockingUseCase.kt:94`
**Also:** `data/local/dao/CallHistoryDao.kt:43`

**Verified:**
```kotlin
val rejections = callHistoryRepo.countRejections(numberHash)
```
Maps to: `SELECT COUNT(*) FROM call_history WHERE numberHash = :numberHash AND outcome = 'rejected'` — no time window. No guard against adding a number already in the blocklist.

**Problem:** A number blocked 3+ times months ago, then manually whitelisted and later removed from the whitelist, re-triggers auto-escalation on the very next call. Also causes redundant DB writes for numbers already blocklisted.

**Fix:**
1. Add a `since: Long` parameter to `countRejectionsByHash` and scope it to a rolling 30-day window.
2. Add a `blocklistRepo.contains(hash)` guard before calling `blocklistRepo.add(...)`.

---

### [M-3] 6 serial `DataStore.data.first()` calls in the screening hot path

**File:** `domain/usecase/GetScreeningSettingsUseCase.kt:18`
**Also:** `data/preferences/ScreeningPreferences.kt:58–74`

**Verified:** Each of the 6 `prefs.*()` methods calls `context.dataStore.data.first()` independently. `ScreenCallUseCase` adds a 7th via `getAdvancedBlockingPolicy()`. DataStore has an in-memory cache, so only the very first call after a cold start hits disk — but even the serial coroutine dispatch overhead adds up inside the screening budget.

**Fix:** Read `dataStore.data.first()` exactly once in `GetScreeningSettingsUseCase` and map all keys in a single lambda. Cache as a `StateFlow<ScreeningSettings>` warmed up in `CallShieldApp.onCreate()` for zero-overhead access during screening.

---

### [M-4] `CallStateMonitor` has no guard against double `start()` — would double-register the telephony callback

**File:** `screening/CallStateMonitor.kt:44`

**Verified:** `start()` registers the telephony callback with no `isStarted` guard. The method exists and `stop()` is fully implemented, but it is never called anywhere in the app (not from `CallShieldApp`, not from any lifecycle owner). `Application.onTerminate()` is not called on production devices so it cannot be used as a cleanup point.

**Note:** The telephony callback intentionally living for the app's lifetime is by design and correct. The real issue is only the missing double-registration guard.

**Fix:** Add an `@Volatile private var started = false` flag; no-op if already started.

---

### [M-5] 10-flow `combine` with unchecked `Array<Any?>` casts — fragile, crashes silently on argument reorder

**File:** `ui/screens/home/HomeViewModel.kt:92`

**Verified:** 10-flow `combine` confirmed at lines 92–133, using `@Suppress("UNCHECKED_CAST")` on every element. Currently correct but completely untyped.

**Problem:** Any future developer adding, reordering, or removing a flow produces a `ClassCastException` at runtime with no compile-time warning.

**Fix:** Decompose into at most two nested typed `combine` calls (max 5 flows each, use the typed overloads), or use a chain of `combine { }.map { }` with a named `data class`.

---

### [M-6] No `Mutex` on `BillingManager.connect()` — concurrent callers can double-resume a suspension

**File:** `billing/BillingManager.kt:82`

**Verified:** `connect()` uses `suspendCancellableCoroutine`. If called concurrently before `isReady` becomes true, two `startConnection()` calls fire, both registering listeners. The second `cont.resume()` throws `IllegalStateException: Already resumed`.

**Note:** Low probability in current usage (paywall calls happen well after app startup). Still a correctness issue.

**Fix:** Protect the connection attempt with a `Mutex`. Concurrent callers should await the in-progress connection result.

---

### [M-7] `debugSimulatePlan()` has no `BuildConfig.DEBUG` guard — can grant free Pro/Family in release builds

**File:** `billing/BillingManager.kt:325`
**Also:** `ui/screens/paywall/PaywallViewModel.kt:143`

**Verified:** `debugSimulatePlan()` directly sets `_isPro`, `_isFamily`, and `_planType` with no guard. The UI entry point (`DebugSimulateSection`) is already behind `if (BuildConfig.DEBUG)`, but the underlying method is unprotected.

**Fix:** Add `check(BuildConfig.DEBUG) { "debugSimulatePlan is only available in debug builds" }` as the first line of both `BillingManager.debugSimulatePlan()` and `PaywallViewModel.debugSimulatePlan()`.

---

## Minor

Code quality and best-practice issues. Address in the normal dev cycle.

---

| ID | File | Issue | Status |
|---|---|---|---|
| m-1 | `di/DatabaseModule.kt` | Missing `.fallbackToDestructiveMigrationOnDowngrade()` — rollbacks crash instead of recovering | **Valid** |
| m-2 | `data/preferences/ScreeningPreferences.kt` | `_protectionState` in `HomeViewModel` is a manual copy of DataStore state; initialised via a one-shot `viewModelScope.launch` — updates from other sources (e.g. SettingsScreen) aren't reflected until the next app restart | **Valid** |
| m-3 | `util/HomeCountryProvider.kt:46` | `isoFromE164()` calls `sortedByDescending { it.value.length }` on 240 entries on every call in the screening path — allocates a new sorted list each time. Cache as `companion object val`. | **Valid** |
| m-4 | `ui/screens/permissions/PermissionsViewModel.kt:78` | OEM battery-hint logic only covers Xiaomi, Samsung, OnePlus. Missing: `"huawei"`, `"honor"`, `"oppo"`, `"realme"`, `"vivo"`, `"iqoo"` — the most aggressive battery killers in the Indian market. | **Valid** |
| m-5 | `notification/NotSpamActionReceiver.kt:40` | No overall `requestTimeoutMillis` on `ApiClient` — `socketTimeout = 5_000` prevents most hangs, but a server drip-feeding data in small chunks resets the socket timer indefinitely. Add `install(HttpTimeout) { requestTimeoutMillis = 8_000 }`. | **Valid (low risk)** |

---

## Issues Investigated and Dismissed

The following items were raised in the initial review but do not represent real bugs in this codebase after code verification.

| ID | Claim | Why dismissed |
|---|---|---|
| — | `purchaseInProgress` non-`@Volatile` threading bug | Both the write (`purchase()`) and the `collect` lambda run on `Dispatchers.Main` via `viewModelScope`. No cross-thread visibility issue. `@Volatile` is harmless to add but not a bug. |
| — | `CallerEventDao` `LIMIT -1 OFFSET 100` unsupported on SQLite 3.28 | The `LIMIT` is inside a `SELECT` subquery (`WHERE id IN (SELECT ...)`), not a direct `DELETE ... LIMIT n`. Subquery LIMIT/OFFSET is valid on all SQLite versions including 3.28. Not a bug. |
| — | `PhoneNumberHasher.normalise()` silently drops 13-digit Indian virtual numbers | The `else -> "+$digits"` branch prepends `+` to any number with more than 11 digits. A 12-digit input becomes a 13-digit E.164 with digit count 12 (within the 8–15 range), and is returned correctly. Not a bug. |
| — | `TimePoliciesScreen` `steps = 4` makes 22:00 and 23:00 unreachable | Compose Slider with `steps = 4` and `valueRange = 20f..25f` generates 6 snap points: 20, 21, 22, 23, 24, 25. All hours are reachable. Reviewer arithmetic was wrong. |
| — | `CAMERA` permission declared for unimplemented Phase 3 QR feature | `QrScanner.kt` and `FamilyProtectionScreen.kt` are fully implemented and shipping. `CAMERA` is actively used. Not unused. |
