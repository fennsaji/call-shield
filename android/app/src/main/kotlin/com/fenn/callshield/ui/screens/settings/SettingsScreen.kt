package com.fenn.callshield.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.BuildConfig
import com.fenn.callshield.R
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onNavigateToBlocklist: () -> Unit = {},
    onNavigateToWhitelist: () -> Unit = {},
    onNavigateToPrefixRules: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTraiReported: () -> Unit = {},
    onNavigateToDndManagement: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        // ── Protection ────────────────────────────────────────────────────────
        SectionHeader("Protection")
        SettingRow(
            icon = Icons.Outlined.Shield,
            title = stringResource(R.string.settings_auto_block),
            onClick = if (!isPro) onNavigateToPaywall else null,
            trailing = {
                if (isPro) {
                    Switch(
                        checked = state.autoBlock,
                        onCheckedChange = { scope.launch { viewModel.setAutoBlock(it) } },
                    )
                } else {
                    ProLockBadge()
                }
            },
        )
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
        SettingRow(
            icon = Icons.Outlined.VisibilityOff,
            title = "Block hidden numbers",
            onClick = if (!isPro) onNavigateToPaywall else null,
            trailing = {
                if (isPro) {
                    Switch(
                        checked = state.blockHidden,
                        onCheckedChange = { scope.launch { viewModel.setBlockHidden(it) } },
                    )
                } else {
                    ProLockBadge()
                }
            },
        )

        Spacer(Modifier.height(16.dp))

        // ── Notifications ─────────────────────────────────────────────────────
        SectionHeader("Notifications")
        SettingRow(
            icon = Icons.Outlined.Notifications,
            title = "Notify on block",
            trailing = {
                Switch(
                    checked = state.notifyOnBlock,
                    onCheckedChange = { scope.launch { viewModel.setNotifyOnBlock(it) } },
                )
            },
        )
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
        SettingRow(
            icon = Icons.Outlined.Shield,
            title = "Notify on flag",
            trailing = {
                Switch(
                    checked = state.notifyOnFlag,
                    onCheckedChange = { scope.launch { viewModel.setNotifyOnFlag(it) } },
                )
            },
        )

        Spacer(Modifier.height(16.dp))

        // ── Setup ─────────────────────────────────────────────────────────────
        SectionHeader("Setup")
        SettingRow(
            icon = Icons.Outlined.Security,
            title = "App Permissions",
            onClick = onNavigateToPermissions,
            trailing = { ChevronIcon() },
        )

        Spacer(Modifier.height(16.dp))

        // ── Privacy ───────────────────────────────────────────────────────────
        SectionHeader("Privacy")
        SettingRow(
            icon = Icons.Outlined.PrivacyTip,
            title = stringResource(R.string.privacy_title),
            onClick = onNavigateToPrivacy,
            trailing = { ChevronIcon() },
        )

        Spacer(Modifier.height(16.dp))

        // ── Reports ───────────────────────────────────────────────────────────
        SectionHeader("Reports")
        SettingRow(
            icon = Icons.Outlined.GppBad,
            title = stringResource(R.string.trai_reported_numbers_title),
            onClick = onNavigateToTraiReported,
            trailing = { ChevronIcon() },
        )
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
        SettingRow(
            icon = Icons.Outlined.DoNotDisturb,
            title = stringResource(R.string.dnd_title),
            onClick = onNavigateToDndManagement,
            trailing = { ChevronIcon() },
        )

        Spacer(Modifier.height(16.dp))

        // ── Account ───────────────────────────────────────────────────────────
        SectionHeader("Account")
        SettingRow(
            icon = Icons.Outlined.Star,
            title = "Upgrade to Pro",
            onClick = onNavigateToPaywall,
            trailing = { ChevronIcon() },
        )

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
    }
}

@Composable
private fun ChevronIcon() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
    )
}

@Composable
private fun ProLockBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Pro",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
