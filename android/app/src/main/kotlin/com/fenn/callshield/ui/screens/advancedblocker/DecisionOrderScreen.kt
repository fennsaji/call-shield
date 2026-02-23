package com.fenn.callshield.ui.screens.advancedblocker

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private data class DecisionStage(
    val priority: Int,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val outcome: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionOrderScreen(onBack: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary

    val stages = listOf(
        DecisionStage(1, "Personal Whitelist", "Numbers you explicitly trust.", Icons.Filled.CheckCircle, primary, "→ Always Allow"),
        DecisionStage(2, "Personal Blocklist", "Numbers you manually blocked.", Icons.Filled.Block, error, "→ Reject"),
        DecisionStage(3, "Advanced Blocking Policy", "Night Guard, Contacts Only, Region Lock, Escalation.", Icons.Filled.Tune, secondary, "→ Silence or Reject"),
        DecisionStage(4, "Prefix Rules", "Rules matching number prefixes you configured.", Icons.Filled.FilterList, tertiary, "→ Block, Silence, or Allow"),
        DecisionStage(5, "Hidden / Private Number", "Calls with no caller ID.", Icons.Filled.VisibilityOff, error, "→ Silence or Reject (Pro)"),
        DecisionStage(6, "Local Spam Database", "Bundled database of known spam numbers.", Icons.Filled.Shield, error, "→ Silence (Known Spam)"),
        DecisionStage(7, "Community Reputation", "Score from community reports.", Icons.Filled.Phone, secondary, "→ Flag or Silence"),
        DecisionStage(8, "Default", "No signal found.", Icons.Filled.CheckCircle, primary, "→ Allow"),
    )

    var selectedStage by remember { mutableStateOf<DecisionStage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Decision Order") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "CallShield checks each incoming call in this exact order. The first match wins.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
            }

            itemsIndexed(stages) { index, stage ->
                DecisionStageCard(
                    stage = stage,
                    isLast = index == stages.lastIndex,
                    onClick = { selectedStage = stage },
                )
            }
        }
    }

    selectedStage?.let { stage ->
        AlertDialog(
            onDismissRequest = { selectedStage = null },
            title = { Text("Priority ${stage.priority}: ${stage.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stage.description)
                    Text(
                        stage.outcome,
                        style = MaterialTheme.typography.titleSmall,
                        color = stage.color,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStage = null }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun DecisionStageCard(
    stage: DecisionStage,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(stage.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stage.priority.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = stage.color,
                    )
                }
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(stage.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    stage.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = stage.color,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stage.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stage.outcome,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
    }
}
