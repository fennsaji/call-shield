package com.fenn.callshield.ui.screens.prefix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.fenn.callshield.ui.components.AppDialog
import com.fenn.callshield.ui.theme.LocalDangerColor
import com.fenn.callshield.ui.theme.LocalSuccessColor
import com.fenn.callshield.ui.theme.LocalWarningColor
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
    val warningColor = LocalWarningColor.current

    val atFreeLimit = !isPro && rules.size >= FREE_PREFIX_RULE_LIMIT

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pattern Rules") },
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
                Icon(Icons.Filled.Add, contentDescription = "Add rule")
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
                    "No pattern rules",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap + to block by prefix, suffix, or pattern",
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
                items(rules, key = { it.id }) { rule ->
                    val isBlock = rule.action == "block"
                    val isSilence = rule.action == "silence"
                    val accentColor = when {
                        isBlock   -> dangerColor
                        isSilence -> warningColor
                        else      -> successColor
                    }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                scope.launch { viewModel.remove(rule.id) }
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
                                        color = dangerColor.copy(alpha = 0.08f),
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
                                    if (isBlock || isSilence) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                    contentDescription = rule.action,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        rule.pattern,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        buildString {
                                            append(when (rule.matchType) {
                                                "suffix"   -> "ends with"
                                                "contains" -> "contains"
                                                else       -> "starts with"
                                            })
                                            if (rule.label.isNotBlank()) append(" · ${rule.label}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    )
                                }
                                // Action badge
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            when (rule.action) {
                                                "silence" -> "Silence"
                                                "allow"   -> "Allow"
                                                else      -> "Block"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = accentColor.copy(alpha = 0.07f),
                                        labelColor = accentColor,
                                    ),
                                    border = null,
                                )
                                IconButton(onClick = {
                                    scope.launch { viewModel.remove(rule.id) }
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
        AddPatternDialog(
            onConfirm = { pattern, matchType, action, label ->
                scope.launch { viewModel.add(pattern, matchType, action, label) }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    if (showLimitDialog) {
        AppDialog(
            onDismissRequest = { showLimitDialog = false },
            icon = Icons.Filled.FilterList,
            title = "Rule limit reached",
            confirmLabel = "Upgrade to Pro",
            onConfirm = {
                showLimitDialog = false
                onNavigateToPaywall()
            },
            onDismiss = { showLimitDialog = false },
        ) {
            Text(
                "Free plan supports up to $FREE_PREFIX_RULE_LIMIT rules. Upgrade to Pro for unlimited rules.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPatternDialog(
    onConfirm: (pattern: String, matchType: String, action: String, label: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pattern by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf("prefix") }
    var action by remember { mutableStateOf("block") }

    val placeholder = when (matchType) {
        "suffix"   -> "e.g. 9999"
        "contains" -> "e.g. 140"
        else       -> "e.g. +91140"
    }

    AppDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Filled.FilterList,
        title = "Add pattern rule",
        confirmLabel = stringResource(R.string.done),
        confirmEnabled = pattern.isNotBlank(),
        onConfirm = { if (pattern.isNotBlank()) onConfirm(pattern.trim(), matchType, action, label.trim()) },
        onDismiss = onDismiss,
    ) {
        // Match type
        Text(
            "Match type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = matchType == "prefix",
                onClick = { matchType = "prefix" },
                label = { Text("Starts with") },
            )
            FilterChip(
                selected = matchType == "suffix",
                onClick = { matchType = "suffix" },
                label = { Text("Ends with") },
            )
            FilterChip(
                selected = matchType == "contains",
                onClick = { matchType = "contains" },
                label = { Text("Contains") },
            )
        }

        OutlinedTextField(
            value = pattern,
            onValueChange = { pattern = it },
            label = { Text("Pattern ($placeholder)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Label (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Action
        Text(
            "Action",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = action == "block",
                onClick = { action = "block" },
                label = { Text("Block") },
            )
            FilterChip(
                selected = action == "silence",
                onClick = { action = "silence" },
                label = { Text("Silence") },
            )
            FilterChip(
                selected = action == "allow",
                onClick = { action = "allow" },
                label = { Text("Allow") },
            )
        }
    }
}
