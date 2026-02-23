package com.fenn.callshield.ui.screens.prefix

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.R
import com.fenn.callshield.ui.theme.LocalDangerColor
import com.fenn.callshield.ui.theme.LocalSuccessColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefixRulesScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: PrefixRulesViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle(emptyList())
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    val dangerColor = LocalDangerColor.current
    val successColor = LocalSuccessColor.current

    val atFreeLimit = !isPro && rules.size >= FREE_PREFIX_RULE_LIMIT

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prefix_rules_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (atFreeLimit) showLimitDialog = true else showAddDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_prefix))
            }
        },
    ) { padding ->
        if (rules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No prefix rules",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap + to add your first rule",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rules, key = { it.prefix }) { rule ->
                    val isBlock = rule.action == "block"
                    val accentColor = if (isBlock) dangerColor else successColor

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                scope.launch { viewModel.remove(rule.prefix) }
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = dangerColor.copy(alpha = 0.15f),
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = dangerColor,
                                )
                            }
                        },
                    ) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    if (isBlock) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                    contentDescription = rule.action,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        rule.prefix,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (rule.label.isNotBlank()) {
                                        Text(
                                            rule.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                        )
                                    }
                                }
                                // Action badge chip
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            if (isBlock) "Block" else "Allow",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = accentColor.copy(alpha = 0.12f),
                                        labelColor = accentColor,
                                    ),
                                    border = null,
                                )
                                IconButton(onClick = {
                                    scope.launch { viewModel.remove(rule.prefix) }
                                }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPrefixDialog(
            onConfirm = { prefix, action, label ->
                scope.launch { viewModel.add(prefix, action, label) }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Rule limit reached") },
            text = { Text("Free plan supports up to $FREE_PREFIX_RULE_LIMIT prefix rules. Upgrade to Pro for unlimited rules.") },
            confirmButton = {
                TextButton(onClick = {
                    showLimitDialog = false
                    onNavigateToPaywall()
                }) { Text("Upgrade to Pro") }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun AddPrefixDialog(
    onConfirm: (prefix: String, action: String, label: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var prefix by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("block") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_prefix)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Prefix (e.g. +91140)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = action == "block",
                        onClick = { action = "block" },
                        label = { Text("Block") },
                    )
                    FilterChip(
                        selected = action == "allow",
                        onClick = { action = "allow" },
                        label = { Text("Allow") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (prefix.isNotBlank()) onConfirm(prefix.trim(), action, label.trim())
            }) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
