package com.fenn.callshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.screening.PaywallTriggerManager
import com.fenn.callshield.ui.CallShieldNavHost
import com.fenn.callshield.ui.theme.CallShieldTheme
import com.fenn.callshield.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var screeningPreferences: ScreeningPreferences
    @Inject lateinit var paywallTriggerManager: PaywallTriggerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by screeningPreferences.observeTheme()
                .collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK   -> true
                ThemeMode.LIGHT  -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CallShieldTheme(darkTheme = darkTheme) {
                CallShieldNavHost(
                    prefs = screeningPreferences,
                    paywallTrigger = paywallTriggerManager,
                )
            }
        }
    }
}
