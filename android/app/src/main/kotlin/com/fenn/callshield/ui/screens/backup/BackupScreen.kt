package com.fenn.callshield.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.ui.components.AppDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigateToPaywall.collect { onNavigateToPaywall() }
    }

    // SAF launchers
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.onExportFileChosen(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.onImportFileChosen(it) } }

    // Snackbar on success / error
    LaunchedEffect(state.status) {
        when (val s = state.status) {
            is BackupStatus.Success -> { snackbarHostState.showSnackbar(s.message); viewModel.clearStatus() }
            is BackupStatus.Error   -> { snackbarHostState.showSnackbar(s.message); viewModel.clearStatus() }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BackupActionCard(
                icon = Icons.Outlined.CloudUpload,
                title = "Export Backup",
                description = "Save your blocklist, whitelist, prefix rules, and all app settings " +
                    "(advanced blocking, notifications) to an encrypted file. " +
                    "You will set a PIN to protect it.",
                buttonLabel = "Choose Save Location",
                isProcessing = state.status is BackupStatus.Processing,
                onClick = { exportLauncher.launch("callshield_backup.csbk") },
            )

            BackupActionCard(
                icon = Icons.Outlined.CloudDownload,
                title = "Restore Backup",
                description = "Restore your rules from a previously exported backup file. " +
                    "You will need the PIN you set during export.",
                buttonLabel = "Choose Backup File",
                isProcessing = false,
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )

            Text(
                "Your backup is encrypted on-device. No data is uploaded to any server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    // PIN dialog
    state.showPinDialog?.let { purpose ->
        PinEntryDialog(
            title  = if (purpose == PinDialogPurpose.EXPORT) "Set Backup PIN" else "Enter Backup PIN",
            hint   = if (purpose == PinDialogPurpose.EXPORT)
                "Choose a PIN to encrypt your backup. You will need it to restore."
            else
                "Enter the PIN you used when creating this backup.",
            onConfirm = viewModel::onPinEntered,
            onDismiss = viewModel::onPinDialogDismissed,
        )
    }

    // Import confirmation dialog
    if (state.showImportConfirm) {
        state.pendingPayload?.let { payload ->
            val prefixCount = if (state.freeRestoreOnly)
                minOf(payload.prefixRules.size, 5)
            else
                payload.prefixRules.size
            AppDialog(
                onDismissRequest = viewModel::onImportCancelled,
                icon = Icons.Outlined.CloudDownload,
                title = "Restore Backup?",
                confirmLabel = "Restore",
                isDestructive = true,
                onConfirm = viewModel::onImportConfirmed,
                onDismiss = viewModel::onImportCancelled,
            ) {
                Text(
                    "This will replace all existing rules with the backup contents:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("• ${payload.blocklist.size} blocked numbers")
                    Text("• ${payload.whitelist.size} whitelisted numbers")
                    Text("• $prefixCount prefix rules${if (state.freeRestoreOnly && payload.prefixRules.size > 5) " (free limit)" else ""}")
                    if (payload.settings != null) Text("• App settings & advanced blocking config")
                }
                Text(
                    "Your current rules will be permanently overwritten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    // Pro plan upgrade dialog — shown when a Pro backup is opened on a free device
    if (state.showProUpgradeDialog) {
        state.pendingPayload?.let { _ ->
            AppDialog(
                onDismissRequest = viewModel::onRestoreFreeContentOnly,
                icon = Icons.Filled.WorkspacePremium,
                title = "Pro Backup Detected",
                confirmLabel = "View Pro Plans",
                dismissLabel = "Restore Free Content",
                onConfirm = viewModel::onProUpgradeClicked,
                onDismiss = viewModel::onRestoreFreeContentOnly,
            ) {
                Text(
                    "This backup was created with a Pro subscription. Some settings require an active plan to fully restore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Text(
                    "Upgrade or restore your subscription to unlock everything, or continue with free content only (blocklist, whitelist, and up to 5 prefix rules).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun BackupActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    isProcessing: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Button(
                onClick = onClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
private fun PinEntryDialog(
    title: String,
    hint: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AppDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Outlined.Lock,
        title = title,
        confirmLabel = "Confirm",
        confirmEnabled = pin.length >= 4,
        onConfirm = { onConfirm(pin) },
        onDismiss = onDismiss,
    ) {
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8) },
            label = { Text("PIN (4–8 digits)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
