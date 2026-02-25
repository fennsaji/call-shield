package com.fenn.callshield.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val autoBlock: Boolean = false,
    val blockHidden: Boolean = false,
    val notifyOnReject: Boolean = true,
    val notifyOnSilence: Boolean = true,
    val notifyOnFlag: Boolean = true,
    val notifyOnNightGuard: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: ScreeningPreferences,
    private val billingManager: BillingManager,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingManager.isPro

    val themeMode: StateFlow<ThemeMode> = prefs.observeTheme()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = SettingsState(
                autoBlock = prefs.autoBlockHighConfidence(),
                blockHidden = prefs.blockHiddenNumbers(),
                notifyOnReject = prefs.notifyOnReject(),
                notifyOnSilence = prefs.notifyOnSilence(),
                notifyOnFlag = prefs.notifyOnFlag(),
                notifyOnNightGuard = prefs.notifyOnNightGuard(),
            )
        }
    }

    suspend fun setAutoBlock(v: Boolean) { prefs.setAutoBlockHighConfidence(v); _state.value = _state.value.copy(autoBlock = v) }
    suspend fun setBlockHidden(v: Boolean) { prefs.setBlockHiddenNumbers(v); _state.value = _state.value.copy(blockHidden = v) }
    suspend fun setNotifyOnReject(v: Boolean) { prefs.setNotifyOnReject(v); _state.value = _state.value.copy(notifyOnReject = v) }
    suspend fun setNotifyOnSilence(v: Boolean) { prefs.setNotifyOnSilence(v); _state.value = _state.value.copy(notifyOnSilence = v) }
    suspend fun setNotifyOnFlag(v: Boolean) { prefs.setNotifyOnFlag(v); _state.value = _state.value.copy(notifyOnFlag = v) }
    suspend fun setNotifyOnNightGuard(v: Boolean) { prefs.setNotifyOnNightGuard(v); _state.value = _state.value.copy(notifyOnNightGuard = v) }
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { prefs.setTheme(mode) } }
}
