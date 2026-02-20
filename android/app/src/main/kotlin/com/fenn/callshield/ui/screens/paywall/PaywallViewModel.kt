package com.fenn.callshield.ui.screens.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.billing.PRODUCT_PRO_ANNUAL
import com.fenn.callshield.billing.PRODUCT_PRO_MONTHLY
import com.fenn.callshield.data.preferences.ScreeningPreferences
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

data class PaywallState(
    val loading: Boolean = false,
    val annualProduct: ProductDetails? = null,
    val monthlyProduct: ProductDetails? = null,
    val purchaseSuccess: Boolean = false,
    val error: String? = null,
    val familyWaitlistEmail: String = "",
    val familyWaitlistJoined: Boolean = false,
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val prefs: ScreeningPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallState())
    val state: StateFlow<PaywallState> = _state.asStateFlow()

    private var purchaseInProgress = false

    init {
        // Collect isPro once — set purchaseSuccess only after an explicit purchase attempt
        viewModelScope.launch {
            billingManager.isPro.collect { isPro ->
                if (isPro && purchaseInProgress) {
                    purchaseInProgress = false
                    _state.value = _state.value.copy(purchaseSuccess = true)
                }
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val connected = billingManager.connect()
                if (!connected) {
                    _state.value = _state.value.copy(loading = false, error = "Could not connect to Play Billing")
                    return@launch
                }
                val products = billingManager.queryProducts()
                val annual = products.firstOrNull { it.productId == PRODUCT_PRO_ANNUAL }
                val monthly = products.firstOrNull { it.productId == PRODUCT_PRO_MONTHLY }
                _state.value = _state.value.copy(
                    loading = false,
                    annualProduct = annual,
                    monthlyProduct = monthly,
                    error = if (annual == null && monthly == null)
                        "Plans unavailable — app must be published on Google Play for purchases to work"
                    else null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun purchase(context: Context, product: ProductDetails) {
        val activity = context.findActivity() ?: run {
            _state.value = _state.value.copy(error = "Could not resolve Activity for billing")
            return
        }
        val result = billingManager.launchBillingFlow(activity, product)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(error = "Could not launch purchase (${result.debugMessage})")
            return
        }
        purchaseInProgress = true
    }

    fun restorePurchase() {
        viewModelScope.launch {
            val connected = billingManager.connect()
            if (!connected) {
                _state.value = _state.value.copy(error = "Could not connect to Play Billing")
                return@launch
            }
            billingManager.refreshSubscriptionStatus()
            if (billingManager.isPro.value) {
                _state.value = _state.value.copy(purchaseSuccess = true)
            } else {
                _state.value = _state.value.copy(error = "No active subscription found")
            }
        }
    }

    fun debugSimulatePurchase() {
        billingManager.debugSimulatePurchase()
        _state.value = _state.value.copy(purchaseSuccess = true)
    }

    fun onFamilyEmailChange(email: String) {
        _state.value = _state.value.copy(familyWaitlistEmail = email)
    }

    fun joinFamilyWaitlist() {
        viewModelScope.launch {
            val email = _state.value.familyWaitlistEmail
            if (email.contains("@")) {
                prefs.setFamilyWaitlistEmail(email)
                _state.value = _state.value.copy(familyWaitlistJoined = true)
            }
        }
    }
}
