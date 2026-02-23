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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
                        val roleManager = context.getSystemService(RoleManager::class.java)
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        screeningRoleLauncher.launch(intent)
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
) {
    val borderModifier = if (granted)
        Modifier.border(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium,
        )
    else Modifier

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().then(borderModifier),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (granted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$number. $title",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // Animated check overlay when granted
                AnimatedVisibility(
                    visible = granted,
                    enter = scaleIn(tween(300)) + fadeIn(tween(300)),
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            if (subBody != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
