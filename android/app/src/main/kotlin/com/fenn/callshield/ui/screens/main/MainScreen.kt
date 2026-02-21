package com.fenn.callshield.ui.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fenn.callshield.R
import com.fenn.callshield.ui.screens.activity.ActivityScreen
import com.fenn.callshield.ui.screens.home.HomeScreen
import com.fenn.callshield.ui.screens.settings.SettingsScreen

@Composable
fun MainScreen(
    onNavigateToBlocklist: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToPrefixRules: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToReport: (hash: String, label: String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
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
                            if (selectedTab == 1) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.nav_activity)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
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
                onNavigateToBlocklist = onNavigateToBlocklist,
                onNavigateToWhitelist = onNavigateToWhitelist,
                onNavigateToPrefixRules = onNavigateToPrefixRules,
                onNavigateToPrivacy = onNavigateToPrivacy,
                onNavigateToPaywall = onNavigateToPaywall,
                onNavigateToReport = onNavigateToReport,
            )
            1 -> ActivityScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onNavigateToReport = onNavigateToReport,
            )
            2 -> SettingsScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onNavigateToBlocklist = onNavigateToBlocklist,
                onNavigateToWhitelist = onNavigateToWhitelist,
                onNavigateToPrefixRules = onNavigateToPrefixRules,
                onNavigateToPrivacy = onNavigateToPrivacy,
                onNavigateToPaywall = onNavigateToPaywall,
                onNavigateToPermissions = onNavigateToPermissions,
            )
        }
    }
}
