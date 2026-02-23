package com.fenn.callshield.ui.screens.advancedblocker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.domain.model.AdvancedBlockingPolicy
import com.fenn.callshield.domain.model.BlockingPreset
import com.fenn.callshield.domain.model.toDefaultPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedBlockingViewModel @Inject constructor(
    private val prefs: ScreeningPreferences,
    private val billingManager: BillingManager,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingManager.isPro

    val policy: StateFlow<AdvancedBlockingPolicy> = prefs.observeAdvancedBlockingPolicy()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdvancedBlockingPolicy(),
        )

    fun setPreset(preset: BlockingPreset) {
        viewModelScope.launch {
            prefs.setAdvancedBlockingPolicy(preset.toDefaultPolicy())
        }
    }

    fun updatePolicy(policy: AdvancedBlockingPolicy) {
        viewModelScope.launch {
            prefs.setAdvancedBlockingPolicy(policy)
        }
    }
}
