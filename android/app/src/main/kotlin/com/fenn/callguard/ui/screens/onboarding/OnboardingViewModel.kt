package com.fenn.callguard.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import android.content.Context
import com.fenn.callguard.data.preferences.ScreeningPreferences
import com.fenn.callguard.data.seeddb.ScamDigestSeeder
import com.fenn.callguard.data.seeddb.SeedDbUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: ScreeningPreferences,
    private val scamDigestSeeder: ScamDigestSeeder,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    suspend fun markOnboardingComplete() {
        prefs.setOnboardingComplete(true)
        scamDigestSeeder.seedIfNeeded()
        SeedDbUpdateWorker.schedule(context)
    }
}
