package com.fenn.callshield.domain.usecase

import com.fenn.callshield.data.preferences.ScreeningPreferences
import javax.inject.Inject

data class ScreeningSettings(
    val autoBlockHighConfidence: Boolean,
    val blockHiddenNumbers: Boolean,
    val notifyOnReject: Boolean,
    val notifyOnSilence: Boolean,
    val notifyOnFlag: Boolean,
    val notifyOnNightGuard: Boolean,
)

class GetScreeningSettingsUseCase @Inject constructor(
    private val prefs: ScreeningPreferences,
) {
    suspend fun get(): ScreeningSettings = ScreeningSettings(
        autoBlockHighConfidence = prefs.autoBlockHighConfidence(),
        blockHiddenNumbers = prefs.blockHiddenNumbers(),
        notifyOnReject = prefs.notifyOnReject(),
        notifyOnSilence = prefs.notifyOnSilence(),
        notifyOnFlag = prefs.notifyOnFlag(),
        notifyOnNightGuard = prefs.notifyOnNightGuard(),
    )
}
