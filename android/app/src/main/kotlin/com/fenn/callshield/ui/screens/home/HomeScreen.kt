package com.fenn.callshield.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.R
import com.fenn.callshield.domain.repository.CallStats
import com.fenn.callshield.ui.components.ScamDigestCard
import com.fenn.callshield.ui.theme.LocalDangerColor
import com.fenn.callshield.ui.theme.LocalSuccessColor
import com.fenn.callshield.ui.theme.LocalWarningColor

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToBlocklist: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToPrefixRules: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToDndManagement: () -> Unit = {},
    onNavigateToFamilyProtection: () -> Unit = {},
    onNavigateToReport: (hash: String, label: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val successColor = LocalSuccessColor.current

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

        // ── DND banner ────────────────────────────────────────────────────────
        item {
            DndBannerCard(onClick = onNavigateToDndManagement)
        }

        // ── Quick access ──────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Quick Access")
                QuickAccessRow(
                    onBlocklist = onNavigateToBlocklist,
                    onWhitelist = onNavigateToWhitelist,
                    onPrefixRules = onNavigateToPrefixRules,
                )
            }
        }

        // ── Features ──────────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Features")
                FeatureBannerCard(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Dashboard",
                    description = "Review what data is processed on your device",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToPrivacy,
                )
                FeatureBannerCard(
                    icon = Icons.Outlined.FamilyRestroom,
                    title = "Family Protection",
                    description = "Shield up to 2 family members with one plan",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToFamilyProtection,
                )
                FeatureBannerCard(
                    icon = Icons.Outlined.Star,
                    title = "Upgrade to Pro",
                    description = "Auto-block high-confidence spam before it rings",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToPaywall,
                )
            }
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
    }
}

@Composable
private fun DndBannerCard(onClick: () -> Unit) {
    val warningColor = LocalWarningColor.current
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            warningColor.copy(alpha = 0.15f),
                            warningColor.copy(alpha = 0.04f),
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(warningColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.DoNotDisturb,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = warningColor,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dnd_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.dnd_home_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }
        }
    }
}

@Composable
private fun FeatureBannerCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            color.copy(alpha = 0.12f),
                            color.copy(alpha = 0.03f),
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = color,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }
        }
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
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .scale(outerScale)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.07f))
                    )
                    // Inner ring
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(innerScale)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.13f))
                    )
                    // Shield
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
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    )
}

@Composable
private fun QuickAccessRow(
    onBlocklist: () -> Unit,
    onWhitelist: () -> Unit,
    onPrefixRules: () -> Unit,
) {
    val successColor = LocalSuccessColor.current
    val dangerColor = LocalDangerColor.current
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickAccessCard(
            icon = Icons.Outlined.Block,
            label = "Blocklist",
            iconTint = dangerColor,
            onClick = onBlocklist,
            modifier = Modifier.weight(1f),
        )
        QuickAccessCard(
            icon = Icons.Outlined.CheckCircle,
            label = "Whitelist",
            iconTint = successColor,
            onClick = onWhitelist,
            modifier = Modifier.weight(1f),
        )
        QuickAccessCard(
            icon = Icons.Outlined.FilterList,
            label = "Prefixes",
            iconTint = primary,
            onClick = onPrefixRules,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                    tint = iconTint,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
