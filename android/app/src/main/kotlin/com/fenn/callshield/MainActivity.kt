package com.fenn.callshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.screening.PaywallTriggerManager
import com.fenn.callshield.ui.CallShieldNavHost
import com.fenn.callshield.ui.theme.CallShieldTheme
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
            CallShieldTheme {
                CallShieldNavHost(
                    prefs = screeningPreferences,
                    paywallTrigger = paywallTriggerManager,
                )
            }
        }
    }
}
