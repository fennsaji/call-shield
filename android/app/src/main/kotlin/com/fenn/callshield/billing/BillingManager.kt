package com.fenn.callshield.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// Product IDs — must match Play Console configuration
const val PRODUCT_PRO_ANNUAL    = "callshield_pro_annual"
const val PRODUCT_PRO_MONTHLY   = "callshield_pro_monthly"
const val PRODUCT_FAMILY_ANNUAL = "callshield_family_annual"  // Phase 3 — ₹699/year
const val PRODUCT_PRO_LIFETIME  = "callshield_pro_lifetime"   // Phase 4 — ₹599–799 one-time

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : PurchasesUpdatedListener {

    private val _isPro    = MutableStateFlow(false)
    private val _isFamily = MutableStateFlow(false)
    val isPro:    StateFlow<Boolean> = _isPro.asStateFlow()
    /** True when the user has an active Family Plan (superset of Pro). */
    val isFamily: StateFlow<Boolean> = _isFamily.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var productDetails: List<ProductDetails> = emptyList()

    /**
     * Connect to Play Billing and refresh subscription status.
     * Call from application onCreate or a ViewModel init block.
     *
     * Returns true immediately if already connected — calling startConnection()
     * while isReady is true may never fire the callback, causing an infinite hang.
     */
    suspend fun connect(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    // Will retry on next purchase attempt
                    if (cont.isActive) cont.resume(false)
                }
            })
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
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_FAMILY_ANNUAL)
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
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val subsPurchases = billingClient.queryPurchasesAsync(subsParams).purchasesList
        val inAppPurchases = billingClient.queryPurchasesAsync(inAppParams).purchasesList

        val hasFamily = subsPurchases.any { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                p.products.contains(PRODUCT_FAMILY_ANNUAL)
        }
        val hasLifetime = inAppPurchases.any { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                p.products.contains(PRODUCT_PRO_LIFETIME)
        }
        val hasPro = hasFamily || hasLifetime || subsPurchases.any { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                (p.products.contains(PRODUCT_PRO_ANNUAL) || p.products.contains(PRODUCT_PRO_MONTHLY))
        }
        _isFamily.value = hasFamily
        _isPro.value = hasPro
    }

    /** Launch billing flow for subscription products (annual/monthly/family). */
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
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    !purchase.isAcknowledged
                ) {
                    acknowledgePurchase(purchase)
                }
            }
            val hasFamily = purchases.any { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    p.products.contains(PRODUCT_FAMILY_ANNUAL)
            }
            val hasLifetime = purchases.any { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    p.products.contains(PRODUCT_PRO_LIFETIME)
            }
            val hasPro = hasFamily || hasLifetime || purchases.any { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    (p.products.contains(PRODUCT_PRO_ANNUAL) || p.products.contains(PRODUCT_PRO_MONTHLY))
            }
            if (hasFamily) _isFamily.value = true
            if (hasPro) _isPro.value = true
        }
    }

    /** Debug-only: bypass Play Billing and mark as Pro immediately. */
    fun debugSimulatePurchase() {
        _isPro.value = true
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { /* fire-and-forget */ }
    }
}
