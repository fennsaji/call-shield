package com.fenn.callguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fenn.callguard.data.preferences.ScreeningPreferences
import com.fenn.callguard.screening.PaywallTriggerManager
import com.fenn.callguard.ui.CallGuardNavHost
import com.fenn.callguard.ui.theme.CallGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var screeningPreferences: ScreeningPreferences
    @Inject lateinit var paywallTriggerManager: PaywallTriggerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CallGuardTheme {
                CallGuardNavHost(
                    prefs = screeningPreferences,
                    paywallTrigger = paywallTriggerManager,
                )
            }
        }
    }
}
