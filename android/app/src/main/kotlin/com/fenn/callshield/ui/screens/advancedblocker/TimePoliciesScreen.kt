package com.fenn.callshield.ui.screens.advancedblocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.domain.model.BlockingPreset
import com.fenn.callshield.domain.model.UnknownCallAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePoliciesScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: AdvancedBlockingViewModel = hiltViewModel(),
) {
    val policy by viewModel.policy.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time Policies") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Night Guard silences unknown calls during quiet hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                // Night Guard toggle
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Night Guard",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Silence unknown calls between ${formatHour(policy.nightGuardStartHour)} – ${formatHour(policy.nightGuardEndHour)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            Switch(
                                checked = policy.nightGuardEnabled,
                                onCheckedChange = {
                                    viewModel.updatePolicy(
                                        policy.copy(
                                            nightGuardEnabled = it,
                                            preset = BlockingPreset.CUSTOM,
                                        )
                                    )
                                },
                            )
                        }

                        if (policy.nightGuardEnabled) {
                            Spacer(Modifier.height(16.dp))

                            // Start hour slider (Free)
                            Text(
                                "Start: ${formatHour(policy.nightGuardStartHour)}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Slider(
                                value = policy.nightGuardStartHour.toFloat(),
                                onValueChange = {
                                    viewModel.updatePolicy(
                                        policy.copy(nightGuardStartHour = it.toInt(), preset = BlockingPreset.CUSTOM)
                                    )
                                },
                                valueRange = 18f..23f,
                                steps = 4,
                                enabled = isPro,
                            )
                            if (!isPro) {
                                Text(
                                    "Custom hours require Pro",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // End hour slider (Free)
                            Text(
                                "End: ${formatHour(policy.nightGuardEndHour)}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Slider(
                                value = policy.nightGuardEndHour.toFloat(),
                                onValueChange = {
                                    viewModel.updatePolicy(
                                        policy.copy(nightGuardEndHour = it.toInt(), preset = BlockingPreset.CUSTOM)
                                    )
                                },
                                valueRange = 5f..10f,
                                steps = 4,
                                enabled = isPro,
                            )

                            Spacer(Modifier.height(8.dp))

                            // Action — REJECT requires Pro
                            Text(
                                "Action",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                UnknownCallAction.entries.forEach { action ->
                                    val isProRequired = action == UnknownCallAction.REJECT
                                    val isEnabled = !isProRequired || isPro
                                    FilterChip(
                                        selected = policy.nightGuardAction == action,
                                        onClick = {
                                            if (isEnabled) {
                                                viewModel.updatePolicy(
                                                    policy.copy(nightGuardAction = action, preset = BlockingPreset.CUSTOM)
                                                )
                                            } else {
                                                onNavigateToPaywall()
                                            }
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Text(action.name.lowercase().replaceFirstChar { it.uppercase() })
                                                if (isProRequired && !isPro) {
                                                    Icon(
                                                        Icons.Outlined.Lock,
                                                        contentDescription = null,
                                                        modifier = Modifier.padding(start = 2.dp),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Free: Fixed 10 PM–7 AM window, Silence action.\nPro: Custom hours and Reject action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "$h:00 $amPm"
}
