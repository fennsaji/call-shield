package com.fenn.callshield.ui.screens.dnd

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.R
import com.fenn.callshield.data.local.entity.DndCommandEntry
import com.fenn.callshield.ui.theme.LocalDangerColor
import com.fenn.callshield.ui.theme.LocalSuccessColor
import com.fenn.callshield.ui.theme.LocalWarningColor
import com.fenn.callshield.util.DndOperator
import com.fenn.callshield.util.TraiReportHelper

// ── Private helpers ────────────────────────────────────────────────────────────

private data class PendingAction(
    val title: String,
    val smsCommand: String,
    val explanation: String,
    val onConfirm: () -> Unit,
)

private data class CategoryItem(
    val code: Int,
    val shortLabelRes: Int,
    val icon: ImageVector,
    val color: Color,
)

private val TRAI_CATEGORIES = listOf(
    CategoryItem(1, R.string.dnd_cat_banking_short, Icons.Outlined.AccountBalance, Color(0xFF1E6FD9)),
    CategoryItem(2, R.string.dnd_cat_realestate_short, Icons.Outlined.Home,           Color(0xFF0D9488)),
    CategoryItem(3, R.string.dnd_cat_education_short, Icons.Outlined.School,          Color(0xFF7C3AED)),
    CategoryItem(4, R.string.dnd_cat_health_short, Icons.Outlined.HealthAndSafety,    Color(0xFF16A34A)),
    CategoryItem(5, R.string.dnd_cat_consumer_short, Icons.Outlined.ShoppingBag,      Color(0xFFEA580C)),
    CategoryItem(6, R.string.dnd_cat_telecom_short, Icons.Outlined.PhoneAndroid,      Color(0xFF0284C7)),
    CategoryItem(7, R.string.dnd_cat_tourism_short, Icons.Outlined.Flight,            Color(0xFF8B5CF6)),
    CategoryItem(8, R.string.dnd_cat_food_short, Icons.Outlined.Restaurant,           Color(0xFFDC2626)),
)

private data class ModeOption(
    val mode: DndMode,
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
)

private val MODE_OPTIONS = listOf(
    ModeOption(DndMode.FULL, Icons.Outlined.DoNotDisturb, R.string.dnd_mode_full_title, R.string.dnd_mode_full_desc),
    ModeOption(DndMode.PROMO, Icons.Outlined.NotificationsOff, R.string.dnd_mode_promo_title, R.string.dnd_mode_promo_desc),
    ModeOption(DndMode.CUSTOM, Icons.Outlined.Tune, R.string.dnd_mode_custom_title, R.string.dnd_mode_custom_desc),
    ModeOption(DndMode.NONE, Icons.Outlined.LockOpen, R.string.dnd_mode_none_title, R.string.dnd_mode_none_desc),
)

private fun launch(context: Context, intent: android.content.Intent) {
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {}
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndManagementScreen(
    onBack: () -> Unit,
    viewModel: DndManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    // Confirmation dialog
    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            icon = {
                Icon(
                    Icons.Outlined.DoNotDisturb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text(action.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = action.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "SMS to 1909:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Text(
                                text = action.smsCommand,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = "Your SMS app will open with this message pre-filled. Just tap Send — TRAI confirms within 24 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    action.onConfirm()
                    pendingAction = null
                }) {
                    Text("Open SMS App")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dnd_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Status ───────────────────────────────────────────────────────
            DndStatusCard(
                latest = state.latestCommand,
                onConfirm = { viewModel.confirmLatest() },
            )

            // ── Operator selector ────────────────────────────────────────────
            OperatorSection(
                selected = state.operator,
                onSelect = { viewModel.setOperator(it) },
            )

            // ── Protection level ──────────────────────────────────────────────
            ProtectionLevelPicker(
                selected = state.displayedMode,
                onSelect = { viewModel.setMode(it) },
            )

            // ── Category chips (Custom mode only) ───────────────────────────
            AnimatedVisibility(
                visible = state.displayedMode == DndMode.CUSTOM,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                CategoryChipsSection(
                    blockedCategories = state.displayedCategories,
                    onToggle = { viewModel.toggleCategory(it) },
                )
            }

            // ── Send CTA ─────────────────────────────────────────────────────
            val operatorSelected = state.operator != null
            val sendEnabled = operatorSelected &&
                state.displayedMode != null &&
                (state.displayedMode != DndMode.CUSTOM || state.displayedCategories.isNotEmpty())
            Button(
                onClick = {
                    val mode = state.displayedMode ?: return@Button
                    val cats = state.displayedCategories.sorted()
                    val op = state.operator ?: DndOperator.OTHER
                    pendingAction = when (mode) {
                        DndMode.FULL -> {
                            val cmd = op.fullBlockCommand()
                            PendingAction(
                                title = "Activate Full DND",
                                smsCommand = cmd,
                                explanation = "All promotional calls and SMS from registered telemarketers will be blocked.",
                                onConfirm = {
                                    launch(context, TraiReportHelper.createDndCommandIntent(cmd))
                                    viewModel.recordCommand("FULL", cmd, null)
                                },
                            )
                        }
                        DndMode.PROMO -> PendingAction(
                            title = "Block Promotional Only",
                            smsCommand = "BLOCK PROMO",
                            explanation = "Blocks promotional messages. OTPs, bank alerts, and service messages from brands you opted into will still be delivered.",
                            onConfirm = {
                                launch(context, TraiReportHelper.createDndPromoIntent())
                                viewModel.recordCommand("PROMO", "BLOCK PROMO", null)
                            },
                        )
                        DndMode.CUSTOM -> {
                            val cmd = op.partialBlockCommand(cats)
                            PendingAction(
                                title = "Block Selected Categories",
                                smsCommand = cmd,
                                explanation = "Blocks promotional messages only in your chosen categories. Everything else will still be delivered.",
                                onConfirm = {
                                    launch(context, TraiReportHelper.createDndCommandIntent(cmd))
                                    viewModel.recordCommand("PARTIAL", cmd, cats.joinToString(","))
                                },
                            )
                        }
                        DndMode.NONE -> {
                            val cmd = op.deactivateCommand()
                            PendingAction(
                                title = "Unblock All",
                                smsCommand = cmd,
                                explanation = "Removes all DND rules. Promotional calls and SMS may resume within 7 days.",
                                onConfirm = {
                                    launch(context, TraiReportHelper.createDndCommandIntent(cmd))
                                    viewModel.recordCommand("DEACTIVATE", cmd, null)
                                },
                            )
                        }
                    }
                },
                enabled = sendEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        !operatorSelected -> "Select your operator first"
                        sendEnabled -> stringResource(R.string.dnd_send_button)
                        else -> stringResource(R.string.dnd_up_to_date)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            // ── Info ─────────────────────────────────────────────────────────
            InfoNote(stringResource(R.string.dnd_info))

            // ── Secondary actions ─────────────────────────────────────────────
            val dangerColor = LocalDangerColor.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    enabled = operatorSelected,
                    onClick = {
                        val cmd = (state.operator ?: DndOperator.OTHER).deactivateCommand()
                        pendingAction = PendingAction(
                            title = "Unblock All",
                            smsCommand = cmd,
                            explanation = "Removes all DND rules. Promotional calls and SMS may resume within 7 days. You can re-apply rules at any time.",
                            onConfirm = {
                                launch(context, TraiReportHelper.createDndCommandIntent(cmd))
                                viewModel.recordCommand("DEACTIVATE", cmd, null)
                            },
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = dangerColor),
                ) {
                    Text(
                        text = stringResource(R.string.dnd_deactivate_short),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                OutlinedButton(
                    onClick = { launch(context, TraiReportHelper.createCallIntent()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Call 1909",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Text(
                text = stringResource(R.string.dnd_ivr_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Status card ────────────────────────────────────────────────────────────────

@Composable
private fun DndStatusCard(latest: DndCommandEntry?, onConfirm: () -> Unit) {
    val successColor = LocalSuccessColor.current
    val warningColor = LocalWarningColor.current
    val primary = MaterialTheme.colorScheme.primary

    data class StatusInfo(
        val accent: Color,
        val label: String,
        val sublabel: String,
        val pending: Boolean,
    )

    val info = when {
        latest == null -> StatusInfo(primary, "Not configured", "Choose a protection level below and tap Send", false)
        latest.command == "DEACTIVATE" && latest.confirmedByUser -> StatusInfo(MaterialTheme.colorScheme.outline, "DND inactive", "Promotional calls may reach you", false)
        latest.command == "FULL" && latest.confirmedByUser -> StatusInfo(successColor, "Full DND active", "All promotional calls & SMS blocked", false)
        latest.command == "PROMO" && latest.confirmedByUser -> StatusInfo(successColor, "Promo DND active", "Unsolicited promotional messages blocked", false)
        latest.command == "PARTIAL" && latest.confirmedByUser -> StatusInfo(successColor, "Category DND active", "Blocking: ${latest.categories?.replace(",", " · ")}", false)
        else -> StatusInfo(warningColor, "Awaiting confirmation", "Send the SMS, then tap 'Mark as Confirmed' once TRAI replies", true)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        info.accent.copy(alpha = 0.18f),
                        info.accent.copy(alpha = 0.06f),
                    )
                )
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colored icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(info.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.DoNotDisturb,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = info.accent,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = info.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = info.accent,
                )
                Text(
                    text = info.sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                if (info.pending) {
                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = warningColor.copy(alpha = 0.15f),
                            contentColor = warningColor,
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.dnd_mark_confirmed),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

// ── Operator section ───────────────────────────────────────────────────────────

@Composable
private fun OperatorSection(
    selected: DndOperator?,
    onSelect: (DndOperator) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.SimCard,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.dnd_operator_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = stringResource(R.string.dnd_operator_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DndOperator.entries.forEach { op ->
                    FilterChip(
                        selected = selected == op,
                        onClick = { onSelect(op) },
                        label = {
                            Text(
                                op.displayName,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

// ── Protection level picker ────────────────────────────────────────────────────

@Composable
private fun ProtectionLevelPicker(
    selected: DndMode?,
    onSelect: (DndMode) -> Unit,
) {
    val dangerColor = LocalDangerColor.current
    val warningColor = LocalWarningColor.current
    val primary = MaterialTheme.colorScheme.primary

    // Each mode has a distinct semantic color
    @Composable
    fun modeColor(mode: DndMode): Color = when (mode) {
        DndMode.FULL -> dangerColor
        DndMode.PROMO -> warningColor
        DndMode.CUSTOM -> primary
        DndMode.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            MODE_OPTIONS.forEachIndexed { index, option ->
                val isSelected = selected == option.mode
                val accent = modeColor(option.mode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option.mode) }
                        .background(
                            if (isSelected) accent.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Colored icon circle — always tinted, just brighter when selected
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = if (isSelected) 0.15f else 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = accent,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(option.titleRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) accent
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(option.descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = if (isSelected) Icons.Outlined.RadioButtonChecked
                        else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) accent
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    )
                }
                if (index < MODE_OPTIONS.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

// ── Category grid section ──────────────────────────────────────────────────────

@Composable
private fun CategoryChipsSection(
    blockedCategories: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    val warningColor = LocalWarningColor.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.dnd_section_categories).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
        // Disclaimer
        Surface(
            color = warningColor.copy(alpha = 0.08f),
            shape = RoundedCornerShape(10.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 1.dp),
                    tint = warningColor,
                )
                Text(
                    text = stringResource(R.string.dnd_category_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        // 2-column grid (manual, avoids nested scroll issues)
        TRAI_CATEGORIES.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { item ->
                    CategoryTile(
                        item = item,
                        isBlocked = item.code in blockedCategories,
                        onToggle = { onToggle(item.code) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Pad last row if odd number of items
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text(
            text = stringResource(R.string.dnd_categories_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun CategoryTile(
    item: CategoryItem,
    isBlocked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = item.color
    val containerColor = if (isBlocked)
        color.copy(alpha = 0.10f)
    else
        MaterialTheme.colorScheme.surfaceContainerLow

    Surface(
        onClick = onToggle,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (isBlocked) 0.18f else 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color,
                )
            }
            Text(
                text = stringResource(item.shortLabelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isBlocked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isBlocked) color else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Block,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = color,
                    )
                }
            }
        }
    }
}

// ── Info note ──────────────────────────────────────────────────────────────────

@Composable
private fun InfoNote(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}
