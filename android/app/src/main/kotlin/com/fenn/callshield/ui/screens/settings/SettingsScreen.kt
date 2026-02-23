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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showPromoDialog by remember { mutableStateOf(false) }

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

        // ── Upgrade to Pro ────────────────────────────────────────────────────
        if (!isPro) {
            UpgradeToProCard(onClick = onNavigateToPaywall)
            Spacer(Modifier.height(4.dp))
        }

        // ── Account ───────────────────────────────────────────────────────────
        if (!isPro) {
            SectionHeader("Account")
            SettingRow(
                icon = Icons.Outlined.CardGiftcard,
                title = "Redeem Promo Code",
                onClick = { showPromoDialog = true },
                trailing = { ChevronIcon() },
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Appearance ────────────────────────────────────────────────────────
        SectionHeader("Appearance")
        ThemeSegmentedRow(
            current = themeMode,
            onSelect = { viewModel.setTheme(it) },
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

        // ── Reports ───────────────────────────────────────────────────────────
        SectionHeader("Reports")
        SettingRow(
            icon = Icons.Outlined.GppBad,
            title = stringResource(R.string.trai_reported_numbers_title),
            onClick = onNavigateToTraiReported,
            trailing = { ChevronIcon() },
        )

        Spacer(Modifier.height(16.dp))

        // ── Data (Phase 3) ────────────────────────────────────────────────────
        SectionHeader("Data")
        SettingRow(
            icon = Icons.Outlined.SaveAlt,
            title = "Backup & Restore",
            onClick = onNavigateToBackup,
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

    if (showPromoDialog) {
        PromoCodeDialog(
            onDismiss = { showPromoDialog = false },
            onRedeem = { code ->
                val success = viewModel.redeemPromoCode(code)
                if (success) showPromoDialog = false
                success
            },
        )
    }
}

@Composable
private fun PromoCodeDialog(
    onDismiss: () -> Unit,
    onRedeem: (String) -> Boolean,
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redeem Promo Code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter your tester promo code to unlock Pro features.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = false },
                    placeholder = { Text("Promo code") },
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) {
                        { Text("Invalid promo code", color = MaterialTheme.colorScheme.error) }
                    } else null,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = onRedeem(code)
                    if (!success) error = true
                },
                enabled = code.isNotBlank(),
            ) {
                Text("Redeem")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun UpgradeToProCard(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.18f), primary.copy(alpha = 0.05f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = primary,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = primary,
                )
            }
        }
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
                .height(56.dp)
                .padding(horizontal = 20.dp),
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
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            options.forEachIndexed { index, (mode, label, icon) ->
                SegmentedButton(
                    selected = current == mode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
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

