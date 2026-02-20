package com.fenn.callguard.ui.screens.reason

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fenn.callguard.R
import com.fenn.callguard.data.local.entity.CallHistoryEntry
import com.fenn.callguard.domain.model.DecisionSource
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Bottom sheet shown when user taps a flagged/silenced call in the history list.
 * PRD §3.14: shows matched prefix rule, seed DB presence, reputation report count.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasonTransparencySheet(
    entry: CallHistoryEntry,
    onDismiss: () -> Unit,
    onMarkNotSpam: () -> Unit,
    onReport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Text(
                text = stringResource(R.string.reason_panel_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = entry.displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            // Risk label
            val (riskLabel, riskColor) = when (entry.outcome) {
                "rejected", "silenced" -> {
                    if (entry.decisionSource == DecisionSource.SEED_DB.name)
                        stringResource(R.string.call_outcome_known_spam) to Color(0xFFEF4444)
                    else
                        stringResource(R.string.call_outcome_likely_spam) to Color(0xFFF97316)
                }
                "flagged" -> stringResource(R.string.call_outcome_likely_spam) to Color(0xFFF97316)
                else -> stringResource(R.string.call_outcome_unknown) to MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = riskLabel,
                style = MaterialTheme.typography.titleMedium,
                color = riskColor,
            )

            HorizontalDivider()

            // Reason rows
            val source = DecisionSource.entries.firstOrNull { it.name == entry.decisionSource }
                ?: DecisionSource.DEFAULT

            when (source) {
                DecisionSource.SEED_DB -> {
                    ReasonRow(
                        icon = Icons.Filled.Shield,
                        tint = Color(0xFFEF4444),
                        text = stringResource(R.string.reason_seed_db),
                    )
                }
                DecisionSource.REMOTE -> {
                    val percent = (entry.confidenceScore * 100).roundToInt()
                    ReasonRow(
                        icon = Icons.Filled.Warning,
                        tint = Color(0xFFF97316),
                        text = stringResource(R.string.reason_remote, 0, percent),
                        // report_count not stored locally; show confidence only
                    )
                }
                DecisionSource.PREFIX -> {
                    ReasonRow(
                        icon = Icons.Filled.Block,
                        tint = Color(0xFFEF4444),
                        text = stringResource(R.string.reason_prefix, ""),
                    )
                }
                DecisionSource.HIDDEN -> {
                    ReasonRow(
                        icon = Icons.Filled.VisibilityOff,
                        tint = MaterialTheme.colorScheme.onSurface,
                        text = stringResource(R.string.reason_hidden),
                    )
                }
                DecisionSource.BLOCKLIST -> {
                    ReasonRow(
                        icon = Icons.Filled.Block,
                        tint = Color(0xFFEF4444),
                        text = stringResource(R.string.reason_blocklist),
                    )
                }
                else -> {
                    ReasonRow(
                        icon = Icons.Filled.Info,
                        tint = MaterialTheme.colorScheme.onSurface,
                        text = source.displayLabel,
                    )
                }
            }

            entry.category?.let { cat ->
                Text(
                    text = "Category: ${cat.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Actions
            Button(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onMarkNotSpam()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(stringResource(R.string.not_spam_undo))
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onReport()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.report_title))
            }
        }
    }
}

@Composable
private fun ReasonRow(
    icon: ImageVector,
    tint: Color,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
