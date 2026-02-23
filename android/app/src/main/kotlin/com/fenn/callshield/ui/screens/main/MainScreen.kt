package com.fenn.callshield.ui.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fenn.callshield.R
import com.fenn.callshield.ui.screens.activity.ActivityScreen
import com.fenn.callshield.ui.screens.home.HomeScreen
import com.fenn.callshield.ui.screens.protect.ProtectScreen
import com.fenn.callshield.ui.screens.settings.SettingsScreen

@Composable
fun MainScreen(
    onNavigateToBlocklist: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToPrefixRules: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTraiReported: () -> Unit,
    onNavigateToDndManagement: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToFamilyProtection: () -> Unit,
    onNavigateToAdvancedBlocking: () -> Unit,
    onNavigateToReport: (hash: String, label: String, screenedAt: Long) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.Security else Icons.Outlined.Security,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.nav_protect)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.nav_activity)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.nav_settings)) },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onNavigateToPrivacy = onNavigateToPrivacy,
                onNavigateToReport = onNavigateToReport,
                onNavigateToProtect = { selectedTab = 1 },
                onNavigateToActivity = { selectedTab = 2 },
            )
            1 -> ProtectScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onNavigateToAdvancedBlocking = onNavigateToAdvancedBlocking,
                onNavigateToBlocklist = onNavigateToBlocklist,
                onNavigateToWhitelist = onNavigateToWhitelist,
                onNavigateToPrefixRules = onNavigateToPrefixRules,
                onNavigateToDndManagement = onNavigateToDndManagement,
                onNavigateToFamilyProtection = onNavigateToFamilyProtection,
                onNavigateToPaywall = onNavigateToPaywall,
            )
            2 -> ActivityScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                snackbarHostState = snackbarHostState,
                onNavigateToReport = onNavigateToReport,
            )
            3 -> SettingsScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onNavigateToTraiReported = onNavigateToTraiReported,
                onNavigateToPermissions = onNavigateToPermissions,
                onNavigateToBackup = onNavigateToBackup,
                onNavigateToPaywall = onNavigateToPaywall,
            )
        }
    }
}
