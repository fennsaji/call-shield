package com.fenn.callshield.ui.screens.currentplan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.billing.PlanType
import com.fenn.callshield.billing.PRODUCT_PRO_ANNUAL
import com.fenn.callshield.billing.PRODUCT_PRO_MONTHLY
import com.fenn.callshield.billing.PRODUCT_PRO_LIFETIME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

data class CurrentPlanState(
    val loading: Boolean = false,
    val planType: PlanType = PlanType.NONE,
    val annualProduct: ProductDetails? = null,
    val monthlyProduct: ProductDetails? = null,
    val lifetimeProduct: ProductDetails? = null,
    val switchSuccess: Boolean = false,
    val error: String? = null,
    val subscriptionRenewalDate: Long? = null,       // epoch ms of next billing date
    val isCancelled: Boolean = false,                // auto-renew off but still active
    val hasConflictingSubscription: Boolean = false, // multiple active / lifetime + subscription
    val hasPendingPurchase: Boolean = false,         // UPI / cash purchase awaiting completion
    val pendingPlanChange: PlanType? = null,         // deferred downgrade confirmed but not yet applied
)

@HiltViewModel
class CurrentPlanViewModel @Inject constructor(
    private val billingManager: BillingManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CurrentPlanState())
    val state: StateFlow<CurrentPlanState> = _state.asStateFlow()

    private var switchInProgress = false
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            billingManager.planType.collect { planType ->
                val current = _state.value
                // Clear pendingPlanChange once the deferred switch has actually applied
                val pendingPlanChange = if (planType == current.pendingPlanChange) null
                                        else current.pendingPlanChange
                _state.value = current.copy(planType = planType, pendingPlanChange = pendingPlanChange)
                if (switchInProgress && planType != PlanType.NONE) {
                    switchInProgress = false
                    _state.value = _state.value.copy(switchSuccess = true)
                }
            }
        }
        viewModelScope.launch {
            billingManager.subscriptionRenewalDate.collect { date ->
                _state.value = _state.value.copy(subscriptionRenewalDate = date)
            }
        }
        viewModelScope.launch {
            billingManager.isSubscriptionCancelled.collect { cancelled ->
                _state.value = _state.value.copy(isCancelled = cancelled)
            }
        }
        viewModelScope.launch {
            billingManager.hasConflictingSubscription.collect { conflict ->
                _state.value = _state.value.copy(hasConflictingSubscription = conflict)
            }
        }
        viewModelScope.launch {
            billingManager.hasPendingPurchase.collect { pending ->
                _state.value = _state.value.copy(hasPendingPurchase = pending)
            }
        }
    }

    fun loadProducts() {
        // Cancel any in-flight load before starting a new one (e.g. rapid back-navigate + return)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val connected = billingManager.connect()
                if (!connected) {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Could not connect to Play Billing",
                    )
                    return@launch
                }
                // Refresh purchases first so planType/cancellation/pending state is current
                // before we read them. Critical when returning from Google Play.
                billingManager.refreshSubscriptionStatus()
                val products = billingManager.queryProducts()
                _state.value = _state.value.copy(
                    loading = false,
                    planType = billingManager.planType.value,
                    annualProduct = products.firstOrNull { it.productId == PRODUCT_PRO_ANNUAL },
                    monthlyProduct = products.firstOrNull { it.productId == PRODUCT_PRO_MONTHLY },
                    lifetimeProduct = products.firstOrNull { it.productId == PRODUCT_PRO_LIFETIME },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun switchPlan(context: Context, product: ProductDetails) {
        val activity = context.findActivity() ?: run {
            _state.value = _state.value.copy(error = "Could not resolve Activity for billing")
            return
        }
        val isUpgrade: Boolean
        val result = when (product.productType) {
            BillingClient.ProductType.INAPP -> {
                isUpgrade = true
                billingManager.launchInAppBillingFlow(activity, product)
            }
            else -> {
                val existingToken = billingManager.activeSubscriptionToken.value
                if (existingToken != null) {
                    // Monthly → Annual = upgrade (immediate); Annual → Monthly = downgrade (deferred)
                    isUpgrade = _state.value.planType == PlanType.PRO_MONTHLY &&
                        product.productId == PRODUCT_PRO_ANNUAL
                    billingManager.upgradeSubscription(activity, product, existingToken, isUpgrade)
                } else {
                    isUpgrade = true
                    // No existing subscription — fresh purchase (e.g. PROMO_PRO switching to billing)
                    billingManager.launchBillingFlow(activity, product)
                }
            }
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(error = "Could not launch purchase (${result.debugMessage})")
            return
        }
        if (!isUpgrade) {
            // Deferred downgrade — Play confirmed but plan won't change until next renewal.
            // Track locally so the UI can show a "pending change" notice immediately.
            val targetPlan = when (product.productId) {
                PRODUCT_PRO_MONTHLY -> PlanType.PRO_MONTHLY
                PRODUCT_PRO_ANNUAL  -> PlanType.PRO_ANNUAL
                else                -> null
            }
            _state.value = _state.value.copy(pendingPlanChange = targetPlan)
        }
        switchInProgress = true
    }

    fun openManageSubscriptions(context: Context) {
        val packageName = context.packageName
        val url = if (_state.value.planType == PlanType.PRO_LIFETIME && !_state.value.hasConflictingSubscription) {
            // Clean Lifetime with no active subscription — show order history (one-time purchase)
            "https://play.google.com/store/account/orderhistory"
        } else {
            // Any subscription state, or Lifetime + conflicting subscription — open subscriptions manager
            "https://play.google.com/store/account/subscriptions?package=$packageName"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun debugSimulatePlan(plan: PlanType) {
        billingManager.debugSimulatePlan(plan)
        _state.value = _state.value.copy(switchSuccess = true)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
