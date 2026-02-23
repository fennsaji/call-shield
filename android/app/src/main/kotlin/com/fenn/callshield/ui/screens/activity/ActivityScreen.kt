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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToReport: (hash: String, label: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Column(modifier = modifier) {
        // Header
        Text(
            text = stringResource(R.string.nav_activity),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        if (state.recentCalls.isEmpty()) {
            EmptyActivityState(modifier = Modifier.fillMaxSize())
        } else {
            // Group by date
            val grouped = state.recentCalls.groupBy { entry ->
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
                                    onNavigateToReport(entry.numberHash, entry.displayLabel)
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

@Composable
private fun ActivityCallRow(entry: CallHistoryEntry, onTap: () -> Unit) {
    val dangerColor = LocalDangerColor.current
    val warningColor = LocalWarningColor.current
    val successColor = LocalSuccessColor.current

    val style = when (entry.outcome) {
        "blocked", "rejected" -> OutcomeStyle(
            border = dangerColor,
            label = stringResource(R.string.outcome_blocked),
            container = MaterialTheme.colorScheme.errorContainer,
            labelColor = MaterialTheme.colorScheme.onErrorContainer,
            icon = Icons.Filled.Block,
        )
        "silenced" -> OutcomeStyle(
            border = warningColor,
            label = stringResource(R.string.outcome_silenced),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.AutoMirrored.Filled.VolumeOff,
        )
        "flagged" -> OutcomeStyle(
            border = warningColor,
            label = stringResource(R.string.outcome_flagged),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.Filled.Warning,
        )
        else -> OutcomeStyle(
            border = successColor,
            label = stringResource(R.string.outcome_allowed),
            container = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Filled.Phone,
        )
    }

    val sourceLabel = runCatching {
        DecisionSource.valueOf(entry.decisionSource).displayLabel
    }.getOrNull()

    val showConfidence = entry.outcome != "allowed" && entry.confidenceScore > 0.0
    val confidencePct = (entry.confidenceScore * 100).roundToInt()

    ElevatedCard(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colored left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .background(style.border),
            )
            Spacer(Modifier.width(12.dp))

            // Avatar circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(style.border.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    style.icon,
                    contentDescription = null,
                    tint = style.border,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))

            // Number + source + confidence
            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
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
                            color = style.border,
                            trackColor = style.border.copy(alpha = 0.15f),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$confidencePct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = style.border,
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
private fun EmptyActivityState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_no_recent_calls),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

private data class OutcomeStyle(
    val border: Color,
    val label: String,
    val container: Color,
    val labelColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
