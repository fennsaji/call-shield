package com.fenn.callshield.ui.screens.family

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.family.FamilyRole
import com.fenn.callshield.ui.components.AppDialog
import com.fenn.callshield.ui.components.QrScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyProtectionScreen(
    onBack: () -> Unit,
    viewModel: FamilyProtectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnpairConfirm by remember { mutableStateOf(false) }

    // Show errors as snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Protection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AnimatedContent(
            targetState = state.showScanner,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(padding),
            label = "scanner_transition",
        ) { showingScanner ->
            if (showingScanner) {
                // Full-screen QR scanner overlay
                Box(Modifier.fillMaxSize()) {
                    QrScanner(
                        modifier = Modifier.fillMaxSize(),
                        onQrScanned = viewModel::onQrScanned,
                    )
                    TextButton(
                        onClick = viewModel::dismissScanner,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp),
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (state.role) {
                        null      -> UnpairedContent(
                            isLoading = state.isLoading,
                            onGuardian = viewModel::startAsGuardian,
                            onDependent = viewModel::showScanner,
                        )
                        FamilyRole.GUARDIAN -> GuardianContent(
                            state     = state,
                            onRefreshQr = viewModel::refreshGuardianQr,
                            onUnpair  = { showUnpairConfirm = true },
                        )
                        FamilyRole.DEPENDENT -> DependentContent(
                            state   = state,
                            onSync  = viewModel::syncNow,
                            onUnpair = { showUnpairConfirm = true },
                        )
                    }
                }
            }
        }
    }

    if (showUnpairConfirm) {
        AppDialog(
            onDismissRequest = { showUnpairConfirm = false },
            icon = Icons.Outlined.LinkOff,
            iconTint = MaterialTheme.colorScheme.error,
            title = "Remove Pairing?",
            confirmLabel = "Remove",
            isDestructive = true,
            onConfirm = { showUnpairConfirm = false; viewModel.unpair() },
            onDismiss = { showUnpairConfirm = false },
        ) {
            Text(
                "This will stop rule sync between the paired devices. No call data or contacts are shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

// ── Not paired ────────────────────────────────────────────────────────────────

@Composable
private fun UnpairedContent(
    isLoading: Boolean,
    onGuardian: () -> Unit,
    onDependent: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(
            Icons.Outlined.FamilyRestroom,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Protect a family member",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            "Share your blocking rules with a family member's device — no contacts or call logs are shared.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        RoleCard(
            icon = Icons.Outlined.Shield,
            title = "I want to protect",
            description = "Generate a QR code. Family member scans it to receive your rules.",
            enabled = !isLoading,
            onClick = onGuardian,
        )
        RoleCard(
            icon = Icons.Outlined.PersonAdd,
            title = "I want to be protected",
            description = "Scan a QR code from the guardian device to receive their rules.",
            enabled = !isLoading,
            onClick = onDependent,
        )

        if (isLoading) CircularProgressIndicator()
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ── Guardian (paired) ─────────────────────────────────────────────────────────

@Composable
private fun GuardianContent(
    state: FamilyProtectionUiState,
    onRefreshQr: () -> Unit,
    onUnpair: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Guardian Device",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Ask the family member to scan this QR code with their CallShield app.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        // QR code display
        Card(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    state.qrBitmap != null -> Image(
                        bitmap = state.qrBitmap.asImageBitmap(),
                        contentDescription = "Pairing QR code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    else -> Text("Tap refresh to generate QR", textAlign = TextAlign.Center)
                }
            }
        }

        Text(
            "QR code expires in 10 minutes. Tap refresh to generate a new one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )

        OutlinedButton(
            onClick = onRefreshQr,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Refresh QR")
        }

        Spacer(Modifier.height(8.dp))

        // Privacy note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
        ) {
            Text(
                "Only your blocking rules are shared — never your contacts, call logs, or phone numbers.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onUnpair,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Outlined.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Remove Pairing")
        }
    }
}

// ── Dependent (paired) ────────────────────────────────────────────────────────

@Composable
private fun DependentContent(
    state: FamilyProtectionUiState,
    onSync: () -> Unit,
    onUnpair: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Protected Device",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        // Synced rules summary
        val prefixCount     = state.syncedRules.count { it.rule_type == "prefix" }
        val preferenceCount = state.syncedRules.count { it.rule_type == "preference" }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Synced Rules", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                if (state.syncedRules.isEmpty()) {
                    Text(
                        "No rules synced yet. Tap sync to fetch the latest from the guardian.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                } else {
                    Text("• $prefixCount prefix rule sets")
                    Text("• $preferenceCount preference sets")
                }
            }
        }

        Button(
            onClick = onSync,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Sync Now")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
        ) {
            Text(
                "Your phone numbers and call logs are never shared with the guardian device.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onUnpair,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Outlined.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Remove Pairing")
        }
    }
}
