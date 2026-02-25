package com.fenn.callshield.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.BuildConfig
import com.fenn.callshield.R
import com.fenn.callshield.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onNavigateToTraiReported: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToCurrentPlan: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
        )

        // ── Plan card ─────────────────────────────────────────────────────────
        if (isPro) {
            YourPlanCard(onClick = onNavigateToCurrentPlan)
        } else {
            UpgradeToProCard(onClick = onNavigateToPaywall)
        }

        // ── Appearance ────────────────────────────────────────────────────────
        SettingsSection("Appearance") {
            ThemeSegmentedRow(current = themeMode, onSelect = { viewModel.setTheme(it) })
        }

        // ── Notifications ─────────────────────────────────────────────────────
        SettingsSection("Notifications") {
            SettingRow(
                icon = Icons.Outlined.Notifications,
                iconTint = MaterialTheme.colorScheme.error,
                title = "Spam blocked",
                subtitle = "Calls rejected by your blocklist or policies",
                trailing = {
                    Switch(
                        checked = state.notifyOnReject,
                        onCheckedChange = { scope.launch { viewModel.setNotifyOnReject(it) } },
                    )
                },
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.VolumeOff,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Call silenced",
                subtitle = "Unknown callers silenced before ringing",
                trailing = {
                    Switch(
                        checked = state.notifyOnSilence,
                        onCheckedChange = { scope.launch { viewModel.setNotifyOnSilence(it) } },
                    )
                },
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.Shield,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = "Possible spam",
                subtitle = "Calls that rang but looked suspicious",
                trailing = {
                    Switch(
                        checked = state.notifyOnFlag,
                        onCheckedChange = { scope.launch { viewModel.setNotifyOnFlag(it) } },
                    )
                },
            )
        }

        // ── Preset overrides ──────────────────────────────────────────────────
        SettingsSection("Preset overrides") {
            SettingRow(
                icon = Icons.Outlined.DarkMode,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "Night Guard",
                subtitle = "Notify when calls are silenced during sleep hours",
                trailing = {
                    Switch(
                        checked = state.notifyOnNightGuard,
                        onCheckedChange = { scope.launch { viewModel.setNotifyOnNightGuard(it) } },
                    )
                },
            )
        }

        // ── Setup ─────────────────────────────────────────────────────────────
        SettingsSection("Setup") {
            SettingRow(
                icon = Icons.Outlined.Security,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "App Permissions",
                onClick = onNavigateToPermissions,
                trailing = { ChevronIcon() },
            )
        }

        // ── Reports ───────────────────────────────────────────────────────────
        SettingsSection("Reports") {
            SettingRow(
                icon = Icons.Outlined.GppBad,
                iconTint = MaterialTheme.colorScheme.error,
                title = stringResource(R.string.trai_reported_numbers_title),
                onClick = onNavigateToTraiReported,
                trailing = { ChevronIcon() },
            )
        }

        // ── Data ──────────────────────────────────────────────────────────────
        SettingsSection("Data") {
            SettingRow(
                icon = Icons.Outlined.SaveAlt,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "Backup & Restore",
                onClick = onNavigateToBackup,
                trailing = { ChevronIcon() },
            )
        }

        Text(
            stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 16.dp),
        )
    }

}

// ── Section card wrapper ──────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) { content() }
        }
    }
}

// ── Upgrade to Pro card ───────────────────────────────────────────────────────

@Composable
private fun UpgradeToProCard(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.09f), primary.copy(alpha = 0.04f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = primary,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "Upgrade to Pro",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                    )
                    Text(
                        "Auto-block spam, advanced blocking & more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
                ChevronIcon()
            }
        }
    }
}

// ── Your Plan card ────────────────────────────────────────────────────────────

@Composable
private fun YourPlanCard(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.09f), primary.copy(alpha = 0.04f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.ManageAccounts,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = primary,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "Your Plan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                    )
                    Text(
                        "Manage, switch, or upgrade your subscription",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
                ChevronIcon()
            }
        }
    }
}

// ── Row ───────────────────────────────────────────────────────────────────────

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp, end = 0.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

// ── Theme picker ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSegmentedRow(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "Default", Icons.Outlined.PhoneAndroid),
        Triple(ThemeMode.LIGHT,  "Light",   Icons.Outlined.LightMode),
        Triple(ThemeMode.DARK,   "Dark",    Icons.Outlined.DarkMode),
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        options.forEachIndexed { index, (mode, label, icon) ->
            SegmentedButton(
                selected = current == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    activeContentColor = MaterialTheme.colorScheme.primary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                ),
                icon = {
                    SegmentedButtonDefaults.ActiveIcon()
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                },
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Shared ────────────────────────────────────────────────────────────────────

@Composable
private fun ChevronIcon() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
    )
}
