package com.fenn.callshield.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.R
import com.fenn.callshield.data.local.entity.CallHistoryEntry
import com.fenn.callshield.domain.model.BlockingPreset
import com.fenn.callshield.domain.repository.CallStats
import com.fenn.callshield.ui.components.ScamDigestCard
import com.fenn.callshield.ui.screens.advancedblocker.AdvancedBlockingViewModel
import com.fenn.callshield.ui.theme.LocalDangerColor
import com.fenn.callshield.ui.theme.LocalSuccessColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToReport: (hash: String, label: String, screenedAt: Long) -> Unit,
    onNavigateToProtect: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    advancedBlockingViewModel: AdvancedBlockingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val policy by advancedBlockingViewModel.policy.collectAsStateWithLifecycle()
    val successColor = LocalSuccessColor.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        viewModel.refreshScreeningRole(context)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshScreeningRole(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isScreeningActive) successColor
                                    else MaterialTheme.colorScheme.error
                                )
                        )
                        Text(
                            text = if (state.isScreeningActive) "Active protection"
                            else "Protection inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isScreeningActive) successColor
                            else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // ── Hero status card ──────────────────────────────────────────────────
        item {
            StatusHeroCard(isActive = state.isScreeningActive, stats = state.stats)
        }

        // ── Protection shortcut ───────────────────────────────────────────────
        item {
            ProtectionShortcutCard(
                presetName = policy.preset.displayName(),
                presetDescription = policy.preset.shortDescription(),
                presetIcon = policy.preset.icon(),
                onClick = onNavigateToProtect,
            )
        }

        // ── Scam digest ───────────────────────────────────────────────────────
        state.scamDigest?.let { digest ->
            item {
                ScamDigestCard(
                    entry = digest,
                    onDismiss = { viewModel.dismissScamDigest(digest.id) },
                )
            }
        }

        // ── Recent blocked calls preview ──────────────────────────────────────
        val recentBlocked = state.recentCalls.filter { it.outcome in listOf("rejected", "silenced") }.take(3)
        if (recentBlocked.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "RECENT BLOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onNavigateToActivity),
                        )
                    }
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    ) {
                        Column {
                            recentBlocked.forEachIndexed { index, call ->
                                RecentCallRow(
                                    entry = call,
                                    onClick = { onNavigateToReport(call.numberHash, call.displayLabel, call.screenedAt) },
                                )
                                if (index < recentBlocked.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 72.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCallRow(
    entry: CallHistoryEntry,
    onClick: () -> Unit,
) {
    val dangerColor = LocalDangerColor.current
    val warningColor = com.fenn.callshield.ui.theme.LocalWarningColor.current

    val (icon, tint) = when (entry.outcome) {
        "rejected" -> Icons.Filled.Block to dangerColor
        "silenced" -> Icons.AutoMirrored.Filled.VolumeOff to warningColor
        "flagged" -> Icons.Filled.Warning to warningColor
        "allowed" -> Icons.Filled.Check to MaterialTheme.colorScheme.primary
        else -> Icons.Filled.Phone to MaterialTheme.colorScheme.onSurface
    }

    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.screenedAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.displayLabel.ifBlank { "Unknown" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                entry.outcome.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = tint.copy(alpha = 0.8f),
            )
        }
        Text(
            timeStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun StatusHeroCard(isActive: Boolean, stats: CallStats) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val error = MaterialTheme.colorScheme.error
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val activeColor = if (isActive) primary else error

    val transition = rememberInfiniteTransition(label = "pulse")
    val outerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.20f else 1f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "outer",
    )
    val innerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.10f else 1f,
        animationSpec = infiniteRepeatable(
            tween(1700, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "inner",
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isActive) primaryContainer else errorContainer,
                            if (isActive) primary.copy(alpha = 0.2f)
                            else error.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface,
                        )
                    )
                )
        ) {
            Column {

                // Shield zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .scale(outerScale)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.07f))
                    )
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(innerScale)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.13f))
                    )
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(76.dp),
                        tint = activeColor,
                    )
                }

                // Status label
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = if (isActive) stringResource(R.string.home_status_active)
                        else stringResource(R.string.home_status_inactive),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = if (isActive) "Monitoring all incoming calls"
                        else "Open Settings → App Permissions to activate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }

                // Inline stats
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InlineStat(
                        value = stats.totalScreened.toString(),
                        label = stringResource(R.string.home_calls_screened),
                        color = primary,
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )
                    InlineStat(
                        value = stats.totalBlocked.toString(),
                        label = stringResource(R.string.home_calls_blocked),
                        color = error,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineStat(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ProtectionShortcutCard(
    presetName: String,
    presetDescription: String,
    presetIcon: ImageVector,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.14f), primary.copy(alpha = 0.03f))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── Top row: section label + configure link ────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "PROTECTION MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "Configure",
                            style = MaterialTheme.typography.labelSmall,
                            color = primary,
                        )
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = primary,
                        )
                    }
                }
                // ── Bottom row: icon + preset name + description ───────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            presetIcon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = primary,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            presetName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                        )
                        Text(
                            presetDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
    }
}

private fun BlockingPreset.displayName(): String = when (this) {
    BlockingPreset.BALANCED -> "Balanced"
    BlockingPreset.AGGRESSIVE -> "Aggressive"
    BlockingPreset.CONTACTS_ONLY -> "Contacts Only"
    BlockingPreset.NIGHT_GUARD -> "Night Guard"
    BlockingPreset.INTERNATIONAL_LOCK -> "International Lock"
    BlockingPreset.CUSTOM -> "Custom"
}

private fun BlockingPreset.shortDescription(): String = when (this) {
    BlockingPreset.BALANCED -> "Spam detection only, no extra rules"
    BlockingPreset.AGGRESSIVE -> "Silencing unknowns + auto-escalating repeat callers"
    BlockingPreset.CONTACTS_ONLY -> "Only saved contacts can ring through"
    BlockingPreset.NIGHT_GUARD -> "Unknown calls silenced 10 PM – 7 AM"
    BlockingPreset.INTERNATIONAL_LOCK -> "Non-+91 numbers are silenced"
    BlockingPreset.CUSTOM -> "Custom rules active"
}

private fun BlockingPreset.icon(): ImageVector = when (this) {
    BlockingPreset.BALANCED -> Icons.Filled.Balance
    BlockingPreset.AGGRESSIVE -> Icons.Filled.Shield
    BlockingPreset.CONTACTS_ONLY -> Icons.Filled.ContactPhone
    BlockingPreset.NIGHT_GUARD -> Icons.Filled.DarkMode
    BlockingPreset.INTERNATIONAL_LOCK -> Icons.Filled.Language
    BlockingPreset.CUSTOM -> Icons.Filled.Tune
}
