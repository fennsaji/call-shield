package com.fenn.callshield.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.R
import com.fenn.callshield.data.local.entity.CallHistoryEntry
import com.fenn.callshield.domain.model.DecisionSource
import com.fenn.callshield.ui.screens.home.HomeViewModel
import com.fenn.callshield.ui.screens.reason.ReasonTransparencySheet
import com.fenn.callshield.ui.theme.LocalDangerColor
import com.fenn.callshield.ui.theme.LocalSuccessColor
import com.fenn.callshield.ui.theme.LocalWarningColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class ActivityFilter(val label: String) {
    ALL("All"),
    BLOCKED("Blocked"),
    SILENCED("Silenced"),
    ALLOWED("Allowed"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToReport: (hash: String, label: String, screenedAt: Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeFilter by rememberSaveable { mutableStateOf(ActivityFilter.ALL) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val filteredCalls = remember(state.recentCalls, activeFilter) {
        when (activeFilter) {
            ActivityFilter.ALL -> state.recentCalls
            ActivityFilter.BLOCKED -> state.recentCalls.filter { it.outcome == "blocked" || it.outcome == "rejected" }
            ActivityFilter.SILENCED -> state.recentCalls.filter { it.outcome == "silenced" }
            ActivityFilter.ALLOWED -> state.recentCalls.filter { it.outcome == "allowed" }
        }
    }

    Column(modifier = modifier) {
        // Header
        Text(
            text = stringResource(R.string.nav_activity),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ActivityFilter.entries) { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = { Text(filter.label) },
                    leadingIcon = if (activeFilter == filter) ({
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }) else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (filteredCalls.isEmpty()) {
            EmptyActivityState(
                modifier = Modifier.fillMaxSize(),
                isFiltered = activeFilter != ActivityFilter.ALL,
            )
        } else {
            val grouped = filteredCalls.groupBy { entry ->
                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(entry.screenedAt))
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                grouped.forEach { (dateLabel, entries) ->
                    item(key = "header_$dateLabel") {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(entries, key = { it.id }) { entry ->
                        var showReasonSheet by remember { mutableStateOf(false) }
                        ActivityCallRow(
                            entry = entry,
                            onTap = { showReasonSheet = true },
                            onReport = {
                                onNavigateToReport(entry.numberHash, entry.displayLabel, entry.screenedAt)
                            },
                            onMarkNotSpam = {
                                viewModel.markNotSpam(entry.numberHash, entry.displayLabel)
                            },
                        )
                        if (showReasonSheet) {
                            ReasonTransparencySheet(
                                entry = entry,
                                onDismiss = { showReasonSheet = false },
                                onMarkNotSpam = {
                                    showReasonSheet = false
                                    viewModel.markNotSpam(entry.numberHash, entry.displayLabel)
                                },
                                onReport = {
                                    showReasonSheet = false
                                    onNavigateToReport(entry.numberHash, entry.displayLabel, entry.screenedAt)
                                },
                                onTraiReported = {
                                    viewModel.recordTraiReport(entry.numberHash, entry.displayLabel)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityCallRow(
    entry: CallHistoryEntry,
    onTap: () -> Unit,
    onReport: () -> Unit,
    onMarkNotSpam: () -> Unit,
) {
    val dangerColor = LocalDangerColor.current
    val warningColor = LocalWarningColor.current
    val successColor = LocalSuccessColor.current

    val style = when (entry.outcome) {
        "blocked", "rejected" -> OutcomeStyle(
            accent = dangerColor,
            label = stringResource(R.string.outcome_blocked),
            container = MaterialTheme.colorScheme.errorContainer,
            labelColor = MaterialTheme.colorScheme.onErrorContainer,
            icon = Icons.Filled.Block,
        )
        "silenced" -> OutcomeStyle(
            accent = warningColor,
            label = stringResource(R.string.outcome_silenced),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.AutoMirrored.Filled.VolumeOff,
        )
        "flagged" -> OutcomeStyle(
            accent = warningColor,
            label = stringResource(R.string.outcome_flagged),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.Filled.Warning,
        )
        else -> OutcomeStyle(
            accent = successColor,
            label = stringResource(R.string.outcome_allowed),
            container = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Filled.Phone,
        )
    }

    val isSpam = entry.outcome in listOf("blocked", "rejected", "silenced", "flagged")
    val sourceLabel = runCatching { DecisionSource.valueOf(entry.decisionSource).displayLabel }.getOrNull()
    val showConfidence = isSpam && entry.confidenceScore > 0.0
    val confidencePct = (entry.confidenceScore * 100).roundToInt()

    ElevatedCard(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(if (showConfidence) 96.dp else 72.dp)
                        .background(style.accent),
                )
                Spacer(Modifier.width(12.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(style.accent.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        style.icon,
                        contentDescription = null,
                        tint = style.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))

                // Number + meta
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        entry.displayLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val subtitle = listOfNotNull(
                        entry.category?.replace('_', ' ')?.replaceFirstChar { it.uppercase() },
                        sourceLabel,
                    ).joinToString(" · ")
                    if (subtitle.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                    }
                    if (showConfidence) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { entry.confidenceScore.toFloat() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(MaterialTheme.shapes.small),
                                color = style.accent,
                                trackColor = style.accent.copy(alpha = 0.08f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "$confidencePct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = style.accent,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Time + badge
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
                ) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.screenedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(6.dp))
                    OutcomeBadge(
                        label = style.label,
                        containerColor = style.container,
                        labelColor = style.labelColor,
                    )
                }
            }

            // Inline action buttons for spam/silenced calls
            if (isSpam) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 68.dp, end = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onMarkNotSpam,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text("Not Spam", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = onReport,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = dangerColor.copy(alpha = 0.07f),
                            contentColor = dangerColor,
                        ),
                    ) {
                        Text("Report", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun OutcomeBadge(label: String, containerColor: Color, labelColor: Color) {
    Box(
        modifier = Modifier
            .background(
                color = containerColor,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyActivityState(modifier: Modifier = Modifier, isFiltered: Boolean = false) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (isFiltered) Icons.Filled.FilterList else Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isFiltered) "No calls match this filter"
            else stringResource(R.string.home_no_recent_calls),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

private data class OutcomeStyle(
    val accent: Color,
    val label: String,
    val container: Color,
    val labelColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
