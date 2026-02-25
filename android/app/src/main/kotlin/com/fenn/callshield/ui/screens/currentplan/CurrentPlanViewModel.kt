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
import com.fenn.callshield.billing.PRODUCT_FAMILY_ANNUAL
import com.fenn.callshield.billing.PRODUCT_FAMILY_LIFETIME
import com.fenn.callshield.billing.PRODUCT_PRO_ANNUAL
import com.fenn.callshield.billing.PRODUCT_PRO_MONTHLY
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val familyAnnualProduct: ProductDetails? = null,
    val familyLifetimeProduct: ProductDetails? = null,
    val switchSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CurrentPlanViewModel @Inject constructor(
    private val billingManager: BillingManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CurrentPlanState())
    val state: StateFlow<CurrentPlanState> = _state.asStateFlow()

    private var switchInProgress = false

    init {
        viewModelScope.launch {
            billingManager.planType.collect { planType ->
                _state.value = _state.value.copy(planType = planType)
                if (switchInProgress && planType != PlanType.NONE) {
                    switchInProgress = false
                    _state.value = _state.value.copy(switchSuccess = true)
                }
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
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
                val products = billingManager.queryProducts()
                _state.value = _state.value.copy(
                    loading = false,
                    planType = billingManager.planType.value,
                    annualProduct = products.firstOrNull { it.productId == PRODUCT_PRO_ANNUAL },
                    monthlyProduct = products.firstOrNull { it.productId == PRODUCT_PRO_MONTHLY },
                    familyAnnualProduct = products.firstOrNull { it.productId == PRODUCT_FAMILY_ANNUAL },
                    familyLifetimeProduct = products.firstOrNull { it.productId == PRODUCT_FAMILY_LIFETIME },
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
        val result = when (product.productType) {
            BillingClient.ProductType.INAPP -> billingManager.launchInAppBillingFlow(activity, product)
            // upgradeSubscription delegates to launchBillingFlow in BL 7.x;
            // Play handles replacement automatically when the user already has a subscription
            else -> billingManager.upgradeSubscription(activity, product)
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(error = "Could not launch purchase (${result.debugMessage})")
            return
        }
        switchInProgress = true
    }

    fun openManageSubscriptions(context: Context) {
        val packageName = context.packageName
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName"),
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
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
