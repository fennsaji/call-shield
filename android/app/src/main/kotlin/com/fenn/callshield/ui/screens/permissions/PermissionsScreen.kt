package com.fenn.callshield.ui.screens.permissions

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.R

@Composable
fun PermissionsScreen(
    onAllGranted: () -> Unit,
    showSkip: Boolean = true,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh permission state whenever the screen resumes (user returns from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Compute progress
    val hasNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val totalCount = if (hasNotification) 3 else 2
    val grantedList = listOf(
        state.screeningRoleGranted,
        if (hasNotification) state.notificationsGranted else true,
        state.batteryOptimisationDisabled,
    )
    val grantedCount = grantedList.count { it }.toFloat()
    val allGranted = grantedList.all { it }

    // Call Screening role request
    val screeningRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    // Notification permission (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    // Battery optimisation — StartActivityForResult so we get a callback on return
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    // Call log (optional)
    val callLogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "Almost ready",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Grant the following to enable call protection",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            // Progress bar
            LinearProgressIndicator(
                progress = { grantedCount / totalCount },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            // ── Call Screening role ───────────────────────────────────────────
            PermissionCard(
                number = 1,
                icon = Icons.Filled.Call,
                title = stringResource(R.string.permission_screening_title),
                body = stringResource(R.string.permission_screening_body),
                granted = state.screeningRoleGranted,
                buttonLabel = stringResource(R.string.permission_screening_button),
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val roleManager = context.getSystemService(RoleManager::class.java)
                            val intent = roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                            if (intent != null) screeningRoleLauncher.launch(intent)
                        } catch (_: Exception) {
                            // MIUI may throw or return null — silently ignore
                        }
                    }
                },
            )

            // ── Notifications ─────────────────────────────────────────────────
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    number = 2,
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.permission_notifications_title),
                    body = stringResource(R.string.permission_notifications_body),
                    granted = state.notificationsGranted,
                    buttonLabel = stringResource(R.string.permission_notifications_button),
                    onRequest = {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            // ── Battery optimisation ──────────────────────────────────────────
            PermissionCard(
                number = if (hasNotification) 3 else 2,
                icon = Icons.Filled.BatteryChargingFull,
                title = stringResource(R.string.permission_battery_title),
                body = stringResource(R.string.permission_battery_body),
                subBody = state.oemBatteryHint.ifBlank { null },
                granted = state.batteryOptimisationDisabled,
                buttonLabel = stringResource(R.string.permission_battery_button),
                onRequest = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    batteryLauncher.launch(intent)
                },
            )

            // ── Call log (optional) ───────────────────────────────────────────
            PermissionCard(
                number = 0,
                stepLabel = "Optional",
                icon = Icons.Filled.History,
                title = "Read call history",
                body = "Shows unknown calls from your device log alongside screened calls in the Activity tab. Nothing is uploaded.",
                granted = state.callLogGranted,
                buttonLabel = "Allow",
                onRequest = {
                    callLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
                },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onAllGranted,
                modifier = Modifier.fillMaxWidth(),
                enabled = allGranted,
            ) {
                Text("Continue")
            }

            if (showSkip) {
                OutlinedButton(
                    onClick = onAllGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                ) {
                    Text(stringResource(R.string.permission_skip))
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    number: Int,
    icon: ImageVector,
    title: String,
    body: String,
    granted: Boolean,
    buttonLabel: String,
    onRequest: () -> Unit,
    subBody: String? = null,
    stepLabel: String? = null,
) {
    val primary = MaterialTheme.colorScheme.primary

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Number + icon badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = if (granted) 0.15f else 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = primary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stepLabel ?: "Step $number",
                        style = MaterialTheme.typography.labelSmall,
                        color = primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                AnimatedVisibility(
                    visible = granted,
                    enter = scaleIn(tween(300)) + fadeIn(tween(300)),
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Granted",
                        tint = primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            if (subBody != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            if (!granted) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRequest,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(buttonLabel)
                }
            }
        }
    }
}
