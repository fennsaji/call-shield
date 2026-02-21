package com.fenn.callshield.ui

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.screening.PaywallTriggerManager
import com.fenn.callshield.ui.screens.blocklist.BlocklistScreen
import com.fenn.callshield.ui.screens.main.MainScreen
import com.fenn.callshield.ui.screens.onboarding.OnboardingScreen
import com.fenn.callshield.ui.screens.paywall.PaywallScreen
import com.fenn.callshield.ui.screens.permissions.PermissionsScreen
import com.fenn.callshield.ui.screens.prefix.PrefixRulesScreen
import com.fenn.callshield.ui.screens.privacy.PrivacyDashboardScreen
import com.fenn.callshield.ui.screens.report.ReportSpamScreen
import com.fenn.callshield.ui.screens.trai.TraiReportedNumbersScreen
import com.fenn.callshield.ui.screens.whitelist.WhitelistScreen

object Destinations {
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val PERMISSIONS_SETTINGS = "permissions_settings"
    const val HOME = "home"
    const val REPORT_SPAM = "report_spam/{numberHash}/{displayLabel}"
    const val BLOCKLIST = "blocklist"
    const val WHITELIST = "whitelist"
    const val PREFIX_RULES = "prefix_rules"
    const val PRIVACY_DASHBOARD = "privacy_dashboard"
    const val TRAI_REPORTED_NUMBERS = "trai_reported_numbers"
    const val PAYWALL = "paywall?trigger={trigger}"

    fun reportSpam(numberHash: String, displayLabel: String) =
        "report_spam/${Uri.encode(numberHash)}/${Uri.encode(displayLabel)}"

    /** Concrete paywall route with the trigger value substituted. */
    fun paywallRoute(trigger: Boolean = false) = "paywall?trigger=$trigger"
}

@Composable
fun CallShieldNavHost(
    prefs: ScreeningPreferences,
    paywallTrigger: PaywallTriggerManager,
    navController: NavHostController = rememberNavController(),
) {
    // null = not yet loaded (suppress NavHost until DataStore emits to avoid onboarding flash)
    val onboardingComplete by prefs.observeOnboardingComplete()
        .collectAsStateWithLifecycle(initialValue = null)

    if (onboardingComplete == null) return

    val startDestination = if (onboardingComplete == true) Destinations.HOME else Destinations.ONBOARDING

    // Observe paywall trigger from screening service
    LaunchedEffect(Unit) {
        paywallTrigger.paywallTrigger.collect {
            navController.navigate(Destinations.paywallRoute(trigger = true)) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
    ) {

        composable(
            Destinations.ONBOARDING,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            OnboardingScreen(onComplete = {
                navController.navigate(Destinations.PERMISSIONS) {
                    popUpTo(Destinations.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(
            Destinations.PERMISSIONS,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            PermissionsScreen(onAllGranted = {
                navController.navigate(Destinations.HOME) {
                    popUpTo(Destinations.PERMISSIONS) { inclusive = true }
                }
            })
        }

        composable(
            Destinations.HOME,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            MainScreen(
                onNavigateToBlocklist = { navController.navigate(Destinations.BLOCKLIST) },
                onNavigateToWhitelist = { navController.navigate(Destinations.WHITELIST) },
                onNavigateToPrefixRules = { navController.navigate(Destinations.PREFIX_RULES) },
                onNavigateToPrivacy = { navController.navigate(Destinations.PRIVACY_DASHBOARD) },
                onNavigateToTraiReported = { navController.navigate(Destinations.TRAI_REPORTED_NUMBERS) },
                onNavigateToPaywall = { navController.navigate(Destinations.paywallRoute()) },
                onNavigateToPermissions = { navController.navigate(Destinations.PERMISSIONS_SETTINGS) },
                onNavigateToReport = { hash, label ->
                    navController.navigate(Destinations.reportSpam(hash, label))
                },
            )
        }

        composable(
            route = Destinations.REPORT_SPAM,
            arguments = listOf(
                navArgument("numberHash") { type = NavType.StringType },
                navArgument("displayLabel") { type = NavType.StringType },
            ),
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
            popEnterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            popExitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
        ) { backStackEntry ->
            ReportSpamScreen(
                numberHash = backStackEntry.arguments?.getString("numberHash") ?: "",
                displayLabel = backStackEntry.arguments?.getString("displayLabel") ?: "",
                onDismiss = { navController.popBackStack() },
            )
        }

        composable(Destinations.PERMISSIONS_SETTINGS) {
            PermissionsScreen(
                onAllGranted = { navController.popBackStack() },
                showSkip = false,
            )
        }

        composable(Destinations.BLOCKLIST) {
            BlocklistScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.WHITELIST) {
            WhitelistScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.PREFIX_RULES) {
            PrefixRulesScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.PRIVACY_DASHBOARD) {
            PrivacyDashboardScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.TRAI_REPORTED_NUMBERS) {
            TraiReportedNumbersScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Destinations.PAYWALL,
            arguments = listOf(navArgument("trigger") {
                type = NavType.BoolType
                defaultValue = false
            }),
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
            popEnterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            popExitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
        ) { backStackEntry ->
            PaywallScreen(
                onDismiss = { navController.popBackStack() },
                fromTrigger = backStackEntry.arguments?.getBoolean("trigger") ?: false,
            )
        }
    }
}
