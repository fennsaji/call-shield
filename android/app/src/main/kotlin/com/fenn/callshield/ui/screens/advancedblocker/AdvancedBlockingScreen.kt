package com.fenn.callshield.ui.screens.advancedblocker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.domain.model.BlockingPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedBlockingScreen(
    onBack: () -> Unit,
    onNavigateToContactPolicies: () -> Unit,
    onNavigateToTimePolicies: () -> Unit,
    onNavigateToRegionPolicies: () -> Unit,
    onNavigateToNumberRules: () -> Unit,
    onNavigateToDecisionOrder: () -> Unit,
    viewModel: AdvancedBlockingViewModel = hiltViewModel(),
) {
    val policy by viewModel.policy.collectAsStateWithLifecycle()
    var pendingPreset by remember { mutableStateOf(policy.preset) }
    val hasUnsavedChange = pendingPreset != policy.preset

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Blocking") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Preset picker ─────────────────────────────────────────────────
            item {
                SectionHeader("Blocking Preset")
                Spacer(Modifier.height(10.dp))
                PresetGrid(
                    currentPreset = pendingPreset,
                    onSelect = { pendingPreset = it },
                )
            }

            // ── Custom policy sections (only when CUSTOM) ─────────────────────
            if (pendingPreset == BlockingPreset.CUSTOM) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SectionHeader("Custom Policy Groups")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PolicyGroupCard(
                            icon = Icons.Filled.ContactPhone,
                            title = "Contact Policies",
                            description = "Allow contacts only, silence unknown callers",
                            onClick = onNavigateToContactPolicies,
                        )
                        PolicyGroupCard(
                            icon = Icons.Filled.DarkMode,
                            title = "Time Policies",
                            description = "Night Guard — silence calls during specific hours",
                            onClick = onNavigateToTimePolicies,
                        )
                        PolicyGroupCard(
                            icon = Icons.Filled.Language,
                            title = "Region Policies",
                            description = "Block international callers (non-+91)",
                            onClick = onNavigateToRegionPolicies,
                        )
                        PolicyGroupCard(
                            icon = Icons.Filled.Security,
                            title = "Number Rules",
                            description = "Auto-escalate repeated callers to blocklist",
                            onClick = onNavigateToNumberRules,
                        )
                    }
                }
            }

            // ── Save / Decision order ─────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasUnsavedChange) {
                        Button(
                            onClick = {
                                viewModel.setPreset(pendingPreset)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Apply — ${pendingPreset.displayName()}")
                        }
                    }
                    OutlinedButton(
                        onClick = onNavigateToDecisionOrder,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("View Decision Order")
                    }
                }
            }
        }
    }
}

private data class PresetEntry(
    val preset: BlockingPreset,
    val icon: ImageVector,
    val label: String,
)

@Composable
private fun PresetGrid(
    currentPreset: BlockingPreset,
    onSelect: (BlockingPreset) -> Unit,
) {
    val presets = listOf(
        PresetEntry(BlockingPreset.BALANCED, Icons.Filled.Balance, "Balanced"),
        PresetEntry(BlockingPreset.AGGRESSIVE, Icons.Filled.Security, "Aggressive"),
        PresetEntry(BlockingPreset.CONTACTS_ONLY, Icons.Filled.ContactPhone, "Contacts Only"),
        PresetEntry(BlockingPreset.NIGHT_GUARD, Icons.Filled.DarkMode, "Night Guard"),
        PresetEntry(BlockingPreset.INTERNATIONAL_LOCK, Icons.Filled.Language, "International Lock"),
        PresetEntry(BlockingPreset.CUSTOM, Icons.Filled.Tune, "Custom"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        presets.forEach { entry ->
            PresetCard(
                icon = entry.icon,
                label = entry.label,
                description = entry.preset.description(),
                details = entry.preset.details(),
                isSelected = currentPreset == entry.preset,
                onClick = { onSelect(entry.preset) },
            )
        }
    }
}

@Composable
private fun PresetCard(
    icon: ImageVector,
    label: String,
    description: String,
    details: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(primary),
                    )
                }
            }

            if (isSelected && details.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = primary.copy(alpha = 0.2f))
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    details.forEach { line ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Filled.Circle,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(5.dp),
                                tint = primary.copy(alpha = 0.7f),
                            )
                            Text(
                                line,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyGroupCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    )
}

private fun BlockingPreset.displayName(): String = when (this) {
    BlockingPreset.BALANCED -> "Balanced"
    BlockingPreset.AGGRESSIVE -> "Aggressive"
    BlockingPreset.CONTACTS_ONLY -> "Contacts Only"
    BlockingPreset.NIGHT_GUARD -> "Night Guard"
    BlockingPreset.INTERNATIONAL_LOCK -> "International Lock"
    BlockingPreset.CUSTOM -> "Custom"
}

private fun BlockingPreset.description(): String = when (this) {
    BlockingPreset.BALANCED -> "Default — spam detection only, no extra rules"
    BlockingPreset.AGGRESSIVE -> "Silence unknown callers + auto-escalate repeat callers"
    BlockingPreset.CONTACTS_ONLY -> "Only allow calls from saved contacts"
    BlockingPreset.NIGHT_GUARD -> "Silence unknown calls from 10 PM to 7 AM"
    BlockingPreset.INTERNATIONAL_LOCK -> "Silence all non-Indian numbers (+91 only)"
    BlockingPreset.CUSTOM -> "Mix and match individual policies"
}

private fun BlockingPreset.details(): List<String> = when (this) {
    BlockingPreset.BALANCED -> listOf(
        "Spam detection via community reports and local seed database",
        "High-confidence spam numbers are silenced before ringing",
        "No extra contact, time, or region filters",
        "Best for general use with minimal interruptions",
    )
    BlockingPreset.AGGRESSIVE -> listOf(
        "Unknown callers (not in contacts) are silenced",
        "Numbers rejected 2+ times are automatically added to your blocklist",
        "Contacts and whitelisted numbers always ring through",
        "Recommended if you receive frequent spam calls",
    )
    BlockingPreset.CONTACTS_ONLY -> listOf(
        "Only calls from your saved contacts are allowed",
        "All unknown callers are silenced (appear as missed calls)",
        "Contact lookup happens on-device — no data is uploaded",
        "Useful when you only expect calls from known people",
    )
    BlockingPreset.NIGHT_GUARD -> listOf(
        "Unknown calls are silenced between 10 PM and 7 AM",
        "Contacts can still reach you at any time",
        "Custom quiet hours and Reject action available with Pro",
        "Calls appear as missed — no permanent blocking",
    )
    BlockingPreset.INTERNATIONAL_LOCK -> listOf(
        "Calls from non-+91 numbers are silenced",
        "Contacts with international numbers are still allowed",
        "Country-specific whitelist / blacklist available with Pro",
        "Ideal if you never expect calls from outside India",
    )
    BlockingPreset.CUSTOM -> listOf(
        "Contact policies: allow contacts only or silence unknowns",
        "Time policies: Night Guard with custom quiet hours (Pro)",
        "Region policies: block international calls",
        "Number rules: auto-escalate repeated callers to blocklist",
    )
}
