package com.fenn.callguard.domain.usecase

import com.fenn.callguard.data.preferences.ScreeningPreferences
import javax.inject.Inject

data class ScreeningSettings(
    val autoBlockHighConfidence: Boolean,
    val blockHiddenNumbers: Boolean,
    val notifyOnBlock: Boolean,
    val notifyOnFlag: Boolean,
)

class GetScreeningSettingsUseCase @Inject constructor(
    private val prefs: ScreeningPreferences,
) {
    suspend fun get(): ScreeningSettings = ScreeningSettings(
        autoBlockHighConfidence = prefs.autoBlockHighConfidence(),
        blockHiddenNumbers = prefs.blockHiddenNumbers(),
        notifyOnBlock = prefs.notifyOnBlock(),
        notifyOnFlag = prefs.notifyOnFlag(),
    )
}
