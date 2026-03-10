package com.fenn.callshield.ui.screens.advancedblocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Night Guard ───────────────────────────────────────────────────
            item {
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
                                    "Silence unknown calls between ${formatHour(if (policy.nightGuardStartHour <= 1) policy.nightGuardStartHour + 24 else policy.nightGuardStartHour)} – ${formatHour(policy.nightGuardEndHour)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            Switch(
                                checked = policy.nightGuardEnabled,
                                onCheckedChange = {
                                    viewModel.updatePolicy(
                                        policy.copy(nightGuardEnabled = it, preset = BlockingPreset.CUSTOM)
                                    )
                                },
                            )
                        }

                        if (policy.nightGuardEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            if (isPro) {
                                val startSlider = if (policy.nightGuardStartHour <= 1)
                                    policy.nightGuardStartHour + 24f else policy.nightGuardStartHour.toFloat()

                                Text("Start: ${formatHour(startSlider.toInt())}", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = startSlider,
                                    onValueChange = {
                                        viewModel.updatePolicy(
                                            policy.copy(nightGuardStartHour = it.toInt() % 24, preset = BlockingPreset.CUSTOM)
                                        )
                                    },
                                    valueRange = 20f..25f,
                                    steps = 4,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("End: ${formatHour(policy.nightGuardEndHour)}", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = policy.nightGuardEndHour.toFloat(),
                                    onValueChange = {
                                        viewModel.updatePolicy(
                                            policy.copy(nightGuardEndHour = it.toInt(), preset = BlockingPreset.CUSTOM)
                                        )
                                    },
                                    valueRange = 5f..10f,
                                    steps = 4,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Action", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    UnknownCallAction.entries.forEach { action ->
                                        FilterChip(
                                            selected = policy.nightGuardAction == action,
                                            onClick = {
                                                viewModel.updatePolicy(
                                                    policy.copy(nightGuardAction = action, preset = BlockingPreset.CUSTOM)
                                                )
                                            },
                                            label = { Text(action.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Active on", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DAY_LABELS.forEachIndexed { idx, label ->
                                        FilterChip(
                                            selected = idx in policy.nightGuardDays,
                                            onClick = {
                                                val newDays = if (idx in policy.nightGuardDays)
                                                    policy.nightGuardDays - idx
                                                else
                                                    policy.nightGuardDays + idx
                                                viewModel.updatePolicy(
                                                    policy.copy(nightGuardDays = newDays, preset = BlockingPreset.CUSTOM)
                                                )
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        )
                                    }
                                }
                            } else {
                                // Free: locked preview inside the same card
                                Text("Start: 10:00 PM", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                Slider(value = 22f, onValueChange = {}, valueRange = 20f..25f, steps = 4, enabled = false)
                                Spacer(Modifier.height(4.dp))
                                Text("End: 7:00 AM", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                Slider(value = 7f, onValueChange = {}, valueRange = 5f..10f, steps = 4, enabled = false)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "Custom hours, action & days",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                    TextButton(onClick = onNavigateToPaywall) {
                                        Text("Unlock Pro", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Work Focus Window (Pro) ───────────────────────────────────────
            item {
                if (isPro) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Work Focus Window",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        if (policy.workFocusEnabled)
                                            "Silence unknown calls ${formatHour(policy.workFocusStartHour)} – ${formatHour(policy.workFocusEndHour)}"
                                        else
                                            "A second silence window for focus hours",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                }
                                Switch(
                                    checked = policy.workFocusEnabled,
                                    onCheckedChange = {
                                        viewModel.updatePolicy(
                                            policy.copy(workFocusEnabled = it, preset = BlockingPreset.CUSTOM)
                                        )
                                    },
                                )
                            }

                            if (policy.workFocusEnabled) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                Text("Start: ${formatHour(policy.workFocusStartHour)}", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = policy.workFocusStartHour.toFloat(),
                                    onValueChange = {
                                        viewModel.updatePolicy(
                                            policy.copy(workFocusStartHour = it.toInt(), preset = BlockingPreset.CUSTOM)
                                        )
                                    },
                                    valueRange = 6f..22f,
                                    steps = 15,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("End: ${formatHour(policy.workFocusEndHour)}", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = policy.workFocusEndHour.toFloat(),
                                    onValueChange = {
                                        viewModel.updatePolicy(
                                            policy.copy(workFocusEndHour = it.toInt(), preset = BlockingPreset.CUSTOM)
                                        )
                                    },
                                    valueRange = 6f..22f,
                                    steps = 15,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Action", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    UnknownCallAction.entries.forEach { action ->
                                        FilterChip(
                                            selected = policy.workFocusAction == action,
                                            onClick = {
                                                viewModel.updatePolicy(
                                                    policy.copy(workFocusAction = action, preset = BlockingPreset.CUSTOM)
                                                )
                                            },
                                            label = { Text(action.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Active on", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DAY_LABELS.forEachIndexed { idx, label ->
                                        FilterChip(
                                            selected = idx in policy.workFocusDays,
                                            onClick = {
                                                val newDays = if (idx in policy.workFocusDays)
                                                    policy.workFocusDays - idx
                                                else
                                                    policy.workFocusDays + idx
                                                viewModel.updatePolicy(
                                                    policy.copy(workFocusDays = newDays, preset = BlockingPreset.CUSTOM)
                                                )
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    ProFeatureCard(
                        valueText = "Silence work interruptions — a second quiet window for your focus hours",
                        upgradeLabel = "Unlock Work Focus",
                        onUpgradeClick = onNavigateToPaywall,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Work Focus Window",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Silence calls 9:00 AM – 6:00 PM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            Switch(checked = false, onCheckedChange = {}, enabled = false)
                        }
                    }
                }
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    val h24 = hour % 24
    val amPm = if (h24 < 12) "AM" else "PM"
    val h = if (h24 % 12 == 0) 12 else h24 % 12
    return "$h:00 $amPm"
}
