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
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.domain.model.BlockingPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPoliciesScreen(
    onBack: () -> Unit,
    viewModel: AdvancedBlockingViewModel = hiltViewModel(),
) {
    val policy by viewModel.policy.collectAsStateWithLifecycle()

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "These settings control how calls from unknown or unsaved contacts are handled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                PolicyToggleCard(
                    title = "Allow contacts only",
                    description = "Calls from numbers not in your contacts will be silenced.",
                    checked = policy.allowContactsOnly,
                    onCheckedChange = {
                        viewModel.updatePolicy(
                            policy.copy(
                                allowContactsOnly = it,
                                preset = BlockingPreset.CUSTOM,
                                // Disable conflicting toggle
                                silenceUnknownNumbers = if (it) false else policy.silenceUnknownNumbers,
                            )
                        )
                    },
                )
            }

            item {
                PolicyToggleCard(
                    title = "Silence unknown numbers",
                    description = "Calls from numbers not in contacts will ring silently and appear as missed calls.",
                    checked = policy.silenceUnknownNumbers,
                    onCheckedChange = {
                        viewModel.updatePolicy(
                            policy.copy(
                                silenceUnknownNumbers = it,
                                preset = BlockingPreset.CUSTOM,
                                allowContactsOnly = if (it) false else policy.allowContactsOnly,
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
internal fun PolicyToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLocked: Boolean = false,
    onLockedClick: () -> Unit = {},
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
