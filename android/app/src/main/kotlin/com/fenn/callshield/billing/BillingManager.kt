package com.fenn.callshield.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.fenn.callshield.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// Product IDs — must match Play Console configuration
const val PRODUCT_PRO_ANNUAL   = "callshield_pro_annual"
const val PRODUCT_PRO_MONTHLY  = "callshield_pro_monthly"
const val PRODUCT_PRO_LIFETIME = "callshield_pro_lifetime"     // Phase 4 — ₹599–799 one-time

/** Which plan the user is currently subscribed to / has purchased. */
enum class PlanType {
    NONE,
    PRO_MONTHLY,
    PRO_ANNUAL,
    PRO_LIFETIME,
    PROMO_PRO,
}

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val promoGrantManager: PromoGrantManager,
    private val deviceTokenManager: com.fenn.callshield.util.DeviceTokenManager,
) : PurchasesUpdatedListener {

    private val _isPro                    = MutableStateFlow(false)
    private val _hasPendingPurchase       = MutableStateFlow(false)
    private val _planType                 = MutableStateFlow(PlanType.NONE)
    private val _activeSubscriptionToken  = MutableStateFlow<String?>(null)
    private val _subscriptionRenewalDate      = MutableStateFlow<Long?>(null)
    private val _isSubscriptionCancelled      = MutableStateFlow(false)
    private val _hasConflictingSubscription   = MutableStateFlow(false)
    val isPro:                    StateFlow<Boolean>  = _isPro.asStateFlow()
    /** True when at least one purchase is in PENDING state (e.g. UPI / cash delayed processing). */
    val hasPendingPurchase:       StateFlow<Boolean>  = _hasPendingPurchase.asStateFlow()
    /** The user's current plan tier. */
    val planType:                 StateFlow<PlanType> = _planType.asStateFlow()
    /** Purchase token of the active subscription, needed for plan switching. Null for INAPP / PROMO. */
    val activeSubscriptionToken:  StateFlow<String?>  = _activeSubscriptionToken.asStateFlow()
    /** Epoch ms of next billing date. Null for lifetime / promo plans. */
    val subscriptionRenewalDate:  StateFlow<Long?>    = _subscriptionRenewalDate.asStateFlow()
    /** True when subscription is active but auto-renew has been turned off (user cancelled). */
    val isSubscriptionCancelled:  StateFlow<Boolean>  = _isSubscriptionCancelled.asStateFlow()
    /**
     * True when the user has conflicting active entitlements that require action:
     *  - Two active subscriptions simultaneously (switching without SubscriptionUpdateParams)
     *  - Lifetime INAPP purchased while a subscription is still active (subscription won't
     *    auto-cancel — user must cancel manually to avoid being charged again)
     */
    val hasConflictingSubscription: StateFlow<Boolean> = _hasConflictingSubscription.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private var productDetails: List<ProductDetails> = emptyList()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectMutex = Mutex()
    private val pendingAckPrefs: SharedPreferences =
        context.getSharedPreferences("pending_ack_tokens", Context.MODE_PRIVATE)

    /**
     * Connect to Play Billing and refresh subscription status.
     * Call from application onCreate or a ViewModel init block.
     *
     * Returns true immediately if already connected — calling startConnection()
     * while isReady is true may never fire the callback, causing an infinite hang.
     */
    suspend fun connect(): Boolean {
        if (billingClient.isReady) return true
        return connectMutex.withLock {
            if (billingClient.isReady) return@withLock true
            suspendCancellableCoroutine { cont ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (cont.isActive) {
                            cont.resume(false)
                        } else {
                            // Post-connection disconnect (OEM battery kill) — schedule reconnect
                            scheduleReconnect()
                        }
                    }
                })
            }
        }
    }

    private fun scheduleReconnect() {
        managerScope.launch {
            var delayMs = 1_000L
            repeat(3) {
                delay(delayMs)
                if (connect()) return@launch
                delayMs *= 2
            }
        }
    }

    suspend fun queryProducts(): List<ProductDetails> {
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_PRO_ANNUAL)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_PRO_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                )
            )
            .build()

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_PRO_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                )
            )
            .build()

        val subsResult = billingClient.queryProductDetails(subsParams)
        val inAppResult = billingClient.queryProductDetails(inAppParams)
        productDetails = (subsResult.productDetailsList ?: emptyList()) +
            (inAppResult.productDetailsList ?: emptyList())
        return productDetails
    }

    suspend fun refreshSubscriptionStatus() {
        // Retry any purchase tokens that failed acknowledgment in a previous session
        pendingAckPrefs.all.keys.toList().forEach { token -> retryAcknowledge(token) }

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val subsPurchases = billingClient.queryPurchasesAsync(subsParams).purchasesList
        val inAppPurchases = billingClient.queryPurchasesAsync(inAppParams).purchasesList

        val hasLifetime = inAppPurchases.any { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                p.products.contains(PRODUCT_PRO_LIFETIME)
        }
        val hasProAnnual = subsPurchases.any { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                p.products.contains(PRODUCT_PRO_ANNUAL)
        }
        val hasProMonthly = subsPurchases.any { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                p.products.contains(PRODUCT_PRO_MONTHLY)
        }
        val hasBillingPro = hasLifetime || hasProAnnual || hasProMonthly
        val promoGrant    = promoGrantManager.activeGrant(deviceTokenManager.deviceTokenHash)
        val hasPromoPro   = promoGrant != PromoGrant.NONE
        _isPro.value = hasBillingPro || hasPromoPro

        // Determine active plan type (highest tier wins)
        _planType.value = when {
            hasLifetime   -> PlanType.PRO_LIFETIME
            hasProAnnual  -> PlanType.PRO_ANNUAL
            hasProMonthly -> PlanType.PRO_MONTHLY
            hasPromoPro   -> PlanType.PROMO_PRO
            else          -> PlanType.NONE
        }

        // Cache the purchase token and compute renewal/cancellation state
        val activeSubs = subsPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val activeSub  = activeSubs.firstOrNull()
        _activeSubscriptionToken.value = activeSub?.purchaseToken
        _isSubscriptionCancelled.value = activeSub != null && !activeSub.isAutoRenewing
        _subscriptionRenewalDate.value = when (_planType.value) {
            PlanType.PRO_ANNUAL  -> activeSub?.purchaseTime?.plus(365L * 24 * 60 * 60 * 1000)
            PlanType.PRO_MONTHLY -> activeSub?.purchaseTime?.plus(30L  * 24 * 60 * 60 * 1000)
            else                 -> null
        }
        // Conflict: multiple active subscriptions OR lifetime + active subscription running together
        _hasConflictingSubscription.value = activeSubs.size > 1 || (hasLifetime && activeSubs.isNotEmpty())

        // Detect pending purchases (e.g. UPI / cash delayed processing)
        _hasPendingPurchase.value = (subsPurchases + inAppPurchases).any {
            it.purchaseState == Purchase.PurchaseState.PENDING
        }

        // Acknowledge any purchases that completed but were never acknowledged
        // (e.g. after reinstall, restore, or if onPurchasesUpdated never fired)
        (subsPurchases + inAppPurchases).forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
    }

    /**
     * Validates [code] against the configured promo hashes and grants the matching plan tier.
     * @return [PromoGrant.PRO] or [PromoGrant.NONE] if invalid/expired.
     */
    fun redeemPromoCode(code: String): PromoGrant {
        val grant = promoGrantManager.redeem(code, deviceTokenManager.deviceTokenHash)
        when (grant) {
            PromoGrant.PRO  -> { _isPro.value = true; _planType.value = PlanType.PROMO_PRO }
            PromoGrant.NONE -> Unit
        }
        return grant
    }

    /** Launch billing flow for subscription products (annual/monthly). */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .build()

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, flowParams)
    }

    /** Launch billing flow for one-time IN_APP products (lifetime plan). */
    fun launchInAppBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            // Update pending flag — if any purchase is still PENDING, keep flag set
            _hasPendingPurchase.value = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PENDING
            }

            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    !purchase.isAcknowledged
                ) {
                    acknowledgePurchase(purchase)
                }
            }
            val hasLifetime = purchases.any { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    p.products.contains(PRODUCT_PRO_LIFETIME)
            }
            val hasProAnnual = purchases.any { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    p.products.contains(PRODUCT_PRO_ANNUAL)
            }
            val hasProMonthly = purchases.any { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    p.products.contains(PRODUCT_PRO_MONTHLY)
            }
            val hasBillingPro = hasLifetime || hasProAnnual || hasProMonthly
            val promoGrant  = promoGrantManager.activeGrant(deviceTokenManager.deviceTokenHash)
            val hasPromoPro = promoGrant != PromoGrant.NONE
            _isPro.value = hasBillingPro || hasPromoPro

            // Update plan type and subscription token
            val newPlanType = when {
                hasLifetime   -> PlanType.PRO_LIFETIME
                hasProAnnual  -> PlanType.PRO_ANNUAL
                hasProMonthly -> PlanType.PRO_MONTHLY
                hasPromoPro   -> PlanType.PROMO_PRO
                else          -> _planType.value
            }
            if (newPlanType != PlanType.NONE) _planType.value = newPlanType

            val activeSubs = purchases.filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    (it.products.contains(PRODUCT_PRO_ANNUAL) ||
                        it.products.contains(PRODUCT_PRO_MONTHLY))
            }
            val activeSub = activeSubs.firstOrNull()
            activeSub?.purchaseToken?.let { _activeSubscriptionToken.value = it }
            _isSubscriptionCancelled.value = activeSub != null && !activeSub.isAutoRenewing
            _subscriptionRenewalDate.value = when (newPlanType) {
                PlanType.PRO_ANNUAL  -> activeSub?.purchaseTime?.plus(365L * 24 * 60 * 60 * 1000)
                PlanType.PRO_MONTHLY -> activeSub?.purchaseTime?.plus(30L  * 24 * 60 * 60 * 1000)
                else                 -> _subscriptionRenewalDate.value
            }
            _hasConflictingSubscription.value = activeSubs.size > 1 || (hasLifetime && activeSubs.isNotEmpty())
        }
    }

    /**
     * Switches an active subscription to a different plan (e.g. monthly ↔ annual).
     *
     * MUST provide [oldPurchaseToken] — omitting it creates a second parallel subscription
     * instead of replacing the existing one, resulting in double charges.
     *
     * Replacement modes:
     *  - Upgrade (monthly → annual): CHARGE_PRORATED_PRICE — immediate switch, prorated charge
     *  - Downgrade (annual → monthly): DEFERRED — switches at next renewal, no immediate charge
     */
    fun upgradeSubscription(
        activity: Activity,
        newProductDetails: ProductDetails,
        oldPurchaseToken: String,
        isUpgrade: Boolean,
    ): BillingResult {
        val offerToken = newProductDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .build()

        val replacementMode = if (isUpgrade) {
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE
        } else {
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
        }

        val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
            .setOldPurchaseToken(oldPurchaseToken)
            .setSubscriptionReplacementMode(replacementMode)
            .build()

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(newProductDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setSubscriptionUpdateParams(subscriptionUpdateParams)
            .build()

        return billingClient.launchBillingFlow(activity, flowParams)
    }

    /** Debug-only: simulate a specific plan without going through Play Billing. */
    fun debugSimulatePlan(plan: PlanType) {
        check(BuildConfig.DEBUG) { "debugSimulatePlan is only available in debug builds" }
        _planType.value = plan
        _isPro.value = plan != PlanType.NONE
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        managerScope.launch { retryAcknowledge(purchase.purchaseToken) }
    }

    private suspend fun retryAcknowledge(token: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()
        val result = billingClient.acknowledgePurchase(params)
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                pendingAckPrefs.edit().remove(token).apply()
            else ->
                pendingAckPrefs.edit().putBoolean(token, true).apply()
        }
    }
}
