package com.fenn.callguard.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callguard.billing.BillingManager
import com.fenn.callguard.data.preferences.ScreeningPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val autoBlock: Boolean = false,
    val blockHidden: Boolean = false,
    val notifyOnBlock: Boolean = true,
    val notifyOnFlag: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: ScreeningPreferences,
    private val billingManager: BillingManager,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = SettingsState(
                autoBlock = prefs.autoBlockHighConfidence(),
                blockHidden = prefs.blockHiddenNumbers(),
                notifyOnBlock = prefs.notifyOnBlock(),
                notifyOnFlag = prefs.notifyOnFlag(),
            )
        }
    }

    suspend fun setAutoBlock(v: Boolean) { prefs.setAutoBlockHighConfidence(v); _state.value = _state.value.copy(autoBlock = v) }
    suspend fun setBlockHidden(v: Boolean) { prefs.setBlockHiddenNumbers(v); _state.value = _state.value.copy(blockHidden = v) }
    suspend fun setNotifyOnBlock(v: Boolean) { prefs.setNotifyOnBlock(v); _state.value = _state.value.copy(notifyOnBlock = v) }
    suspend fun setNotifyOnFlag(v: Boolean) { prefs.setNotifyOnFlag(v); _state.value = _state.value.copy(notifyOnFlag = v) }
}
