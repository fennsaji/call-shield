package com.fenn.callshield.ui.screens.activity

import android.Manifest
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.R
import com.fenn.callshield.data.local.entity.CallHistoryEntry
import com.fenn.callshield.domain.model.DecisionSource
import com.fenn.callshield.ui.components.AppDialog
import com.fenn.callshield.ui.screens.calllog.CallLogEntryWithHash
import com.fenn.callshield.ui.screens.calllog.CallLogViewModel
import com.fenn.callshield.ui.screens.home.HomeViewModel
import com.fenn.callshield.ui.screens.reason.ReasonTransparencySheet
import com.fenn.callshield.ui.theme.LocalDangerColor
import com.fenn.callshield.ui.theme.LocalSuccessColor
import com.fenn.callshield.ui.theme.LocalWarningColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Masks the middle of a phone number for display: `+91 9876****3210`.
 * Strips leading country code for display, shows first 4 + **** + last 4 of the local number.
 */
private fun maskNumber(number: String): String {
    if (number.isBlank()) return "Unknown"
    // Strip +91 / 0091 / leading 91 (India) to get the 10-digit local number
    val local = when {
        number.startsWith("+91") -> number.removePrefix("+91").filter { it.isDigit() }
        number.startsWith("0091") -> number.removePrefix("0091").filter { it.isDigit() }
        else -> number.filter { it.isDigit() }.let { d ->
            if (d.length == 12 && d.startsWith("91")) d.drop(2) else d
        }
    }
    return when {
        local.length > 8 -> "+91 ${local.take(4)}****${local.takeLast(4)}"
        local.length >= 4 -> "+91 ****${local.takeLast(4)}"
        else -> number
    }
}

private enum class ActivityFilter(val label: String) {
    ALL("All"),
    BLOCKED("Blocked"),
    SILENCED("Silenced"),
    ALLOWED("Allowed"),
}

/** Unified item for the merged timeline. */
private sealed class ActivityItem {
    abstract val timestamp: Long

    data class Screened(val entry: CallHistoryEntry) : ActivityItem() {
        override val timestamp: Long = entry.screenedAt
    }

    data class FromDevice(val item: CallLogEntryWithHash) : ActivityItem() {
        override val timestamp: Long = item.entry.timestamp
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToReport: (hash: String, label: String, screenedAt: Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    callLogViewModel: CallLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val callLogState by callLogViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var activeFilter by rememberSaveable { mutableStateOf(ActivityFilter.ALL) }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> callLogViewModel.onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(callLogState.snackbar) {
        callLogState.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            callLogViewModel.clearSnackbar()
        }
    }
    // Auto-load on open: if READ_CALL_LOG is already granted entries appear immediately.
    // If not granted, callLogState.permissionGranted stays false → prompt card shown.
    LaunchedEffect(Unit) {
        callLogViewModel.checkPermissionAndLoad(context)
    }

    // Screened calls filtered by active tab
    val filteredCalls = remember(state.recentCalls, activeFilter) {
        when (activeFilter) {
            ActivityFilter.ALL -> state.recentCalls
            ActivityFilter.BLOCKED -> state.recentCalls.filter { it.outcome == "blocked" || it.outcome == "rejected" }
            ActivityFilter.SILENCED -> state.recentCalls.filter { it.outcome == "silenced" }
            ActivityFilter.ALLOWED -> state.recentCalls.filter { it.outcome == "allowed" }
        }
    }

    // Device log entries to merge:
    //   - unknown callers only (name == null means no saved contact)
    //   - not already represented in the screened calls list (avoids duplicates)
    val mergedItems = remember(filteredCalls, callLogState.entries, callLogState.permissionGranted) {
        val screenedHashes = filteredCalls.map { it.numberHash }.toSet()
        val deviceItems: List<ActivityItem> = if (callLogState.permissionGranted) {
            callLogState.entries
                .filter { item ->
                    item.entry.name == null &&       // unknown caller (not saved in contacts)
                        item.hash != null &&              // number can be hashed
                        item.hash !in screenedHashes      // not already shown as screened
                }
                .map { ActivityItem.FromDevice(it) }
        } else {
            emptyList()
        }

        (filteredCalls.map { ActivityItem.Screened(it) } + deviceItems)
            .sortedByDescending { it.timestamp }
    }

    // Date-group the merged list
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val grouped = remember(mergedItems) {
        mergedItems.groupBy { dateFormat.format(Date(it.timestamp)) }
    }

    // Permission dialog
    if (showPermissionDialog) {
        CallLogPermissionDialog(
            onConfirm = {
                showPermissionDialog = false
                permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
            },
            onDismiss = { showPermissionDialog = false },
        )
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.nav_activity),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

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

        // Filtered-and-empty state (non-ALL filter)
        if (activeFilter != ActivityFilter.ALL && filteredCalls.isEmpty()) {
            EmptyActivityState(modifier = Modifier.fillMaxSize(), isFiltered = true)
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mergedItems.isEmpty() && !callLogState.loading) {
                // Nothing screened yet, nothing from device log
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.home_no_recent_calls),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }
            } else {
                grouped.forEach { (dateLabel, items) ->
                    item(key = "header_$dateLabel") {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(items, key = { item ->
                        when (item) {
                            is ActivityItem.Screened -> "s_${item.entry.id}"
                            is ActivityItem.FromDevice -> "d_${item.item.entry.number}_${item.item.entry.timestamp}"
                        }
                    }) { item ->
                        when (item) {
                            is ActivityItem.Screened -> {
                                var showReasonSheet by remember { mutableStateOf(false) }
                                val hash = item.entry.numberHash
                                ActivityCallRow(
                                    entry = item.entry,
                                    onTap = { showReasonSheet = true },
                                    onReport = {
                                        onNavigateToReport(hash, item.entry.displayLabel, item.entry.screenedAt)
                                    },
                                    onMarkNotSpam = {
                                        viewModel.markNotSpam(hash, item.entry.displayLabel)
                                    },
                                )
                                if (showReasonSheet) {
                                    ReasonTransparencySheet(
                                        entry = item.entry,
                                        isBlocked = hash in state.blockedHashes,
                                        isWhitelisted = hash in state.whitelistedHashes,
                                        onDismiss = { showReasonSheet = false },
                                        onMarkNotSpam = {
                                            showReasonSheet = false
                                            viewModel.markNotSpam(hash, item.entry.displayLabel)
                                        },
                                        onReport = {
                                            showReasonSheet = false
                                            onNavigateToReport(hash, item.entry.displayLabel, item.entry.screenedAt)
                                        },
                                        onTraiReported = {
                                            viewModel.recordTraiReport(hash, item.entry.displayLabel)
                                        },
                                        onBlock = { viewModel.blockNumber(hash, item.entry.displayLabel) },
                                        onUnblock = { viewModel.unblockNumber(hash) },
                                        onWhitelist = { viewModel.whitelistNumber(hash, item.entry.displayLabel) },
                                        onUnwhitelist = { viewModel.unwhitelistNumber(hash) },
                                    )
                                }
                            }
                            is ActivityItem.FromDevice -> {
                                var showDeviceSheet by remember { mutableStateOf(false) }
                                val hash = item.item.hash ?: return@items
                                val label = maskNumber(item.item.entry.number)
                                DeviceCallLogRow(
                                    item = item.item,
                                    onTap = { showDeviceSheet = true },
                                )
                                if (showDeviceSheet) {
                                    DeviceCallLogSheet(
                                        item = item.item,
                                        isBlocked = hash in state.blockedHashes,
                                        isWhitelisted = hash in state.whitelistedHashes,
                                        onDismiss = { showDeviceSheet = false },
                                        onBlock = { viewModel.blockNumber(hash, label) },
                                        onUnblock = { viewModel.unblockNumber(hash) },
                                        onWhitelist = { viewModel.whitelistNumber(hash, label) },
                                        onUnwhitelist = { viewModel.unwhitelistNumber(hash) },
                                        onReport = {
                                            showDeviceSheet = false
                                            onNavigateToReport(hash, label, item.item.entry.timestamp)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom of list — spinner while loading, prompt if permission not yet granted
            when {
                callLogState.loading -> item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                    }
                }
                !callLogState.permissionGranted -> item(key = "prompt") {
                    DeviceLogPromptCard(
                        onLoad = { showPermissionDialog = true },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Device log composables ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceLogPromptCard(onLoad: () -> Unit) {
    ElevatedCard(
        onClick = onLoad,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "See older calls from unknown numbers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Read from your device — nothing is uploaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun CallLogPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Filled.History,
        title = "Show calls from unknown numbers?",
        confirmLabel = "Grant Access",
        onConfirm = onConfirm,
        dismissLabel = "Not Now",
        onDismiss = onDismiss,
    ) {
        Text(
            "CallShield will ask for Read Call Log permission. Here's what it's used for:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PermissionPoint(
                icon = Icons.Filled.PhoneInTalk,
                text = "Show incoming calls from numbers not in your contacts",
            )
            PermissionPoint(
                icon = Icons.Filled.Lock,
                text = "Read locally on this device — never uploaded",
            )
            PermissionPoint(
                icon = Icons.Filled.Block,
                text = "Block or report any past unknown caller in one tap",
            )
            PermissionPoint(
                icon = Icons.Filled.CloudOff,
                text = "No account, no sync, no analytics",
            )
        }
    }
}

@Composable
private fun PermissionPoint(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCallLogRow(
    item: CallLogEntryWithHash,
    onTap: () -> Unit,
) {
    val entry = item.entry
    val warningColor = LocalWarningColor.current

    val (icon, accent, typeLabel) = when (entry.type) {
        CallLog.Calls.OUTGOING_TYPE -> Triple(
            Icons.Filled.CallMade as ImageVector,
            MaterialTheme.colorScheme.primary,
            "Outgoing",
        )
        CallLog.Calls.MISSED_TYPE -> Triple(
            Icons.AutoMirrored.Filled.CallMissed as ImageVector,
            warningColor,
            "Missed",
        )
        else -> Triple(
            Icons.Filled.CallReceived as ImageVector,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            "Incoming",
        )
    }

    val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    val numberLabel = maskNumber(entry.number)

    ElevatedCard(onClick = onTap, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(accent),
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(accent.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = typeLabel, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(numberLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(typeLabel)
                        if (entry.duration > 0) append(" · ${entry.duration}s")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Text(
                    timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(6.dp))
                OutcomeBadge(
                    label = "Unscreened",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCallLogSheet(
    item: CallLogEntryWithHash,
    isBlocked: Boolean,
    isWhitelisted: Boolean,
    onDismiss: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onWhitelist: () -> Unit,
    onUnwhitelist: () -> Unit,
    onReport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val entry = item.entry
    val dangerColor = LocalDangerColor.current
    val warningColor = LocalWarningColor.current
    val successColor = LocalSuccessColor.current

    val (callIcon, callAccent, typeLabel) = when (entry.type) {
        CallLog.Calls.OUTGOING_TYPE -> Triple(Icons.Filled.CallMade as ImageVector, MaterialTheme.colorScheme.primary, "Outgoing")
        CallLog.Calls.MISSED_TYPE -> Triple(Icons.AutoMirrored.Filled.CallMissed as ImageVector, warningColor, "Missed")
        else -> Triple(Icons.Filled.CallReceived as ImageVector, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), "Incoming")
    }

    val numberLabel = maskNumber(entry.number)
    val onSurface = MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header — same layout as ReasonTransparencySheet
            Text(
                text = numberLabel,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = buildString {
                    append(typeLabel)
                    if (entry.duration > 0) append(" · ${entry.duration}s")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = onSurface.copy(alpha = 0.6f),
            )

            // "Unscreened" badge — same style as risk badge in screened sheet
            Surface(
                color = onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "Unscreened",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            HorizontalDivider()

            // Reason row — same structure as ReasonTransparencySheet's reason row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(callIcon, contentDescription = null, modifier = Modifier.size(28.dp), tint = callAccent)
                Text(
                    text = "Not screened by CallShield",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Block / Whitelist toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isBlocked) {
                    FilledTonalButton(
                        onClick = { scope.launch { sheetState.hide(); onUnblock() } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = dangerColor.copy(alpha = 0.10f),
                            contentColor = dangerColor,
                        ),
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Unblock")
                    }
                } else {
                    OutlinedButton(
                        onClick = { scope.launch { sheetState.hide(); onBlock() } },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Block")
                    }
                }

                if (isWhitelisted) {
                    FilledTonalButton(
                        onClick = { scope.launch { sheetState.hide(); onUnwhitelist() } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = successColor.copy(alpha = 0.10f),
                            contentColor = successColor,
                        ),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Whitelisted")
                    }
                } else {
                    OutlinedButton(
                        onClick = { scope.launch { sheetState.hide(); onWhitelist() } },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Whitelist")
                    }
                }
            }

            // Report — filled primary, same style/weight as "Mark as Not Spam" in screened sheet
            Button(
                onClick = { scope.launch { sheetState.hide(); onReport() } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(stringResource(R.string.report_title))
            }
        }
    }
}

// ── Screened-call composables ─────────────────────────────────────────────────

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

    ElevatedCard(onClick = onTap, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(if (showConfidence) 96.dp else 72.dp)
                        .background(style.accent),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(style.accent.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(style.icon, contentDescription = null, tint = style.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text(entry.displayLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val subtitle = listOfNotNull(
                        entry.category?.replace('_', ' ')?.replaceFirstChar { it.uppercase() },
                        sourceLabel,
                    ).joinToString(" · ")
                    if (subtitle.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                    if (showConfidence) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { entry.confidenceScore.toFloat() },
                                modifier = Modifier.weight(1f).height(4.dp).clip(MaterialTheme.shapes.small),
                                color = style.accent,
                                trackColor = style.accent.copy(alpha = 0.08f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("$confidencePct%", style = MaterialTheme.typography.labelSmall, color = style.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
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
                    OutcomeBadge(label = style.label, containerColor = style.container, labelColor = style.labelColor)
                }
            }
            if (isSpam) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 68.dp, end = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onMarkNotSpam,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) { Text("Not Spam", style = MaterialTheme.typography.labelSmall) }
                    FilledTonalButton(
                        onClick = onReport,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = dangerColor.copy(alpha = 0.07f),
                            contentColor = dangerColor,
                        ),
                    ) { Text("Report", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
private fun OutcomeBadge(label: String, containerColor: Color, labelColor: Color) {
    Box(
        modifier = Modifier
            .background(color = containerColor, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = FontWeight.SemiBold)
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
            text = if (isFiltered) "No calls match this filter" else stringResource(R.string.home_no_recent_calls),
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
    val icon: ImageVector,
)
