package com.fenn.callguard.ui.screens.paywall

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.fenn.callguard.billing.BillingManager
import com.fenn.callguard.billing.PRODUCT_PRO_ANNUAL
import com.fenn.callguard.billing.PRODUCT_PRO_MONTHLY
import com.fenn.callguard.data.preferences.ScreeningPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                _state.value = _state.value.copy(
                    loading = false,
                    annualProduct = products.firstOrNull { it.productId == PRODUCT_PRO_ANNUAL },
                    monthlyProduct = products.firstOrNull { it.productId == PRODUCT_PRO_MONTHLY },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun purchase(context: Context, product: ProductDetails) {
        val activity = context as? android.app.Activity ?: return
        billingManager.launchBillingFlow(activity, product)
        viewModelScope.launch {
            billingManager.isPro.collect { isPro ->
                if (isPro) _state.value = _state.value.copy(purchaseSuccess = true)
            }
        }
    }

    fun restorePurchase() {
        viewModelScope.launch {
            billingManager.refreshSubscriptionStatus()
            if (billingManager.isPro.value) {
                _state.value = _state.value.copy(purchaseSuccess = true)
            } else {
                _state.value = _state.value.copy(error = "No active subscription found")
            }
        }
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
