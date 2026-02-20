package com.fenn.callguard.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callguard.R
import com.fenn.callguard.data.local.entity.CallHistoryEntry
import com.fenn.callguard.ui.components.ScamDigestCard
import com.fenn.callguard.ui.screens.reason.ReasonTransparencySheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBlocklist: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToPrefixRules: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToReport: (hash: String, label: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToPrivacy) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = "Privacy")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Status card ───────────────────────────────────────────────────
            item {
                StatusCard(isActive = state.isScreeningActive)
            }

            // ── Stats row ─────────────────────────────────────────────────────
            item {
                StatsRow(screened = state.stats.totalScreened, blocked = state.stats.totalBlocked)
            }

            // ── Quick-access buttons ──────────────────────────────────────────
            item {
                QuickAccessRow(
                    onBlocklist = onNavigateToBlocklist,
                    onWhitelist = onNavigateToWhitelist,
                    onPrefixRules = onNavigateToPrefixRules,
                )
            }

            // ── Scam digest card (PRD §3.10) ──────────────────────────────────
            state.scamDigest?.let { digest ->
                item {
                    ScamDigestCard(
                        entry = digest,
                        onDismiss = { viewModel.dismissScamDigest(digest.id) },
                    )
                }
            }

            // ── Recent calls ──────────────────────────────────────────────────
            item {
                Text(
                    stringResource(R.string.home_recent_calls),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (state.recentCalls.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.home_no_recent_calls),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                items(state.recentCalls, key = { it.id }) { entry ->
                    var showReasonSheet by remember { mutableStateOf(false) }
                    CallHistoryRow(
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(isActive: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = if (isActive) stringResource(R.string.home_status_active)
                else stringResource(R.string.home_status_inactive),
                style = MaterialTheme.typography.titleLarge,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun StatsRow(screened: Int, blocked: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            label = stringResource(R.string.home_calls_screened),
            value = screened.toString(),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.home_calls_blocked),
            value = blocked.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun QuickAccessRow(
    onBlocklist: () -> Unit,
    onWhitelist: () -> Unit,
    onPrefixRules: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickButton(Icons.Filled.Block, "Blocklist", onBlocklist, Modifier.weight(1f))
        QuickButton(Icons.Filled.Check, "Whitelist", onWhitelist, Modifier.weight(1f))
        QuickButton(Icons.Filled.List, "Prefixes", onPrefixRules, Modifier.weight(1f))
    }
}

@Composable
private fun QuickButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Card(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CallHistoryRow(entry: CallHistoryEntry, onTap: () -> Unit) {
    val (icon, tint) = when (entry.outcome) {
        "blocked" -> Icons.Filled.Block to Color(0xFFEF4444)
        "flagged" -> Icons.Filled.Warning to Color(0xFFF97316)
        else -> Icons.Filled.Check to Color(0xFF22C55E)
    }

    Card(modifier = Modifier.fillMaxWidth(), onClick = onTap) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayLabel, style = MaterialTheme.typography.titleMedium)
                entry.category?.let {
                    Text(
                        it.replaceFirstChar { c -> c.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.screenedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
