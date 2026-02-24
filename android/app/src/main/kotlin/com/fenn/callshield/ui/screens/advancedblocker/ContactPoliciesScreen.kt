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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.domain.model.BlockingPreset

/** Which unknown-caller mode is active — maps to the two booleans in AdvancedBlockingPolicy. */
private enum class UnknownCallerMode { ALLOW, SILENCE, CONTACTS_ONLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPoliciesScreen(
    onBack: () -> Unit,
    viewModel: AdvancedBlockingViewModel = hiltViewModel(),
) {
    val policy by viewModel.policy.collectAsStateWithLifecycle()

    val currentMode = when {
        policy.allowContactsOnly -> UnknownCallerMode.CONTACTS_ONLY
        policy.silenceUnknownNumbers -> UnknownCallerMode.SILENCE
        else -> UnknownCallerMode.ALLOW
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Policies") },
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Choose how unknown callers — numbers not in your contacts — are handled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                UnknownCallerOptionCard(
                    icon = Icons.Outlined.People,
                    title = "Allow all callers",
                    description = "Unknown numbers ring normally. No contact-based filtering.",
                    selected = currentMode == UnknownCallerMode.ALLOW,
                    onClick = {
                        viewModel.updatePolicy(
                            policy.copy(
                                allowContactsOnly = false,
                                silenceUnknownNumbers = false,
                                preset = BlockingPreset.CUSTOM,
                            )
                        )
                    },
                )
            }

            item {
                UnknownCallerOptionCard(
                    icon = Icons.Outlined.VolumeOff,
                    title = "Silence unknown numbers",
                    description = "Their phone keeps ringing but you are not disturbed. The call appears in your history so you can choose to call back.",
                    selected = currentMode == UnknownCallerMode.SILENCE,
                    onClick = {
                        viewModel.updatePolicy(
                            policy.copy(
                                silenceUnknownNumbers = true,
                                allowContactsOnly = false,
                                preset = BlockingPreset.CUSTOM,
                            )
                        )
                    },
                )
            }

            item {
                UnknownCallerOptionCard(
                    icon = Icons.Outlined.ContactPhone,
                    title = "Contacts only",
                    description = "Only saved contacts ring through. Unknown callers get an immediate busy signal — the call is rejected before it reaches you.",
                    selected = currentMode == UnknownCallerMode.CONTACTS_ONLY,
                    onClick = {
                        viewModel.updatePolicy(
                            policy.copy(
                                allowContactsOnly = true,
                                silenceUnknownNumbers = false,
                                preset = BlockingPreset.CUSTOM,
                            )
                        )
                    },
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Contacts are checked on-device only. No contact data is ever uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun UnknownCallerOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) primary.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(primary),
                )
            }
        }
    }
}

@Composable
internal fun PolicyToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLocked: Boolean = false,
    onLockedClick: () -> Unit = {},
) {
    androidx.compose.material3.ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (isLocked) {
                TextButton(onClick = onLockedClick) {
                    Text("Pro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        }
    }
}
