package com.fenn.callshield.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fenn.callshield.R
import com.fenn.callshield.ui.screens.activity.ActivityScreen
import com.fenn.callshield.ui.screens.home.HomeScreen
import com.fenn.callshield.ui.screens.protect.ProtectScreen
import com.fenn.callshield.ui.screens.settings.SettingsScreen

private data class NavTab(
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
)

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
    onNavigateToCurrentPlan: () -> Unit,
    onNavigateToReport: (hash: String, label: String, screenedAt: Long) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf(
        NavTab(stringResource(R.string.nav_home),     Icons.Filled.Home,     Icons.Outlined.Home),
        NavTab(stringResource(R.string.nav_protect),  Icons.Filled.Security, Icons.Outlined.Security),
        NavTab(stringResource(R.string.nav_activity), Icons.Filled.History,  Icons.Outlined.History),
        NavTab(stringResource(R.string.nav_settings), Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FloatingNavBar(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onNavigateToPrivacy = onNavigateToPrivacy,
                onNavigateToReport = onNavigateToReport,
                onNavigateToProtect = { selectedTab = 1 },
                onNavigateToActivity = { selectedTab = 2 },
                onNavigateToDndManagement = onNavigateToDndManagement,
            )
            1 -> ProtectScreen(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onNavigateToAdvancedBlocking = onNavigateToAdvancedBlocking,
                onNavigateToBlocklist = onNavigateToBlocklist,
                onNavigateToWhitelist = onNavigateToWhitelist,
                onNavigateToPrefixRules = onNavigateToPrefixRules,
                onNavigateToDndManagement = onNavigateToDndManagement,
                onNavigateToFamilyProtection = onNavigateToFamilyProtection,
                onNavigateToPaywall = onNavigateToPaywall,
            )
            2 -> ActivityScreen(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                snackbarHostState = snackbarHostState,
                onNavigateToReport = onNavigateToReport,
            )
            3 -> SettingsScreen(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onNavigateToTraiReported = onNavigateToTraiReported,
                onNavigateToPermissions = onNavigateToPermissions,
                onNavigateToBackup = onNavigateToBackup,
                onNavigateToPaywall = onNavigateToPaywall,
                onNavigateToCurrentPlan = onNavigateToCurrentPlan,
            )
        }
    }
}

@Composable
private fun FloatingNavBar(
    tabs: List<NavTab>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (selected) primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onTabSelected(index) }
                            .padding(
                                horizontal = if (selected) 16.dp else 14.dp,
                                vertical = 10.dp,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp),
                                tint = if (selected) primary else onSurface.copy(alpha = 0.45f),
                            )
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally(),
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
