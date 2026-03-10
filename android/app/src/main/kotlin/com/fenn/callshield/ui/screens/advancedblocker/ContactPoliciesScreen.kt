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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.fenn.callshield.data.local.entity.VipContactEntry
import com.fenn.callshield.domain.model.BlockingPreset

/** Which unknown-caller mode is active — maps to the fields in AdvancedBlockingPolicy. */
private enum class UnknownCallerMode { ALLOW, SILENCE, CONTACTS_ONLY, VIP_ONLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPoliciesScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: AdvancedBlockingViewModel = hiltViewModel(),
) {
    val policy by viewModel.policy.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val vipContacts by viewModel.vipContacts.collectAsStateWithLifecycle()
    val vipSearchResults by viewModel.vipSearchResults.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showVipManager by rememberSaveable { mutableStateOf(false) }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showVipManager = true }

    fun openVipManager() {
        val hasContacts = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasContacts) showVipManager = true
        else contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    val currentMode = when {
        policy.vipContactsOnlyEnabled -> UnknownCallerMode.VIP_ONLY
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
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
                                vipContactsOnlyEnabled = false,
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
                                vipContactsOnlyEnabled = false,
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
                                vipContactsOnlyEnabled = false,
                                preset = BlockingPreset.CUSTOM,
                            )
                        )
                    },
                )
            }

            // ── VIP Contacts Only (Pro) ───────────────────────────────────────
            item {
                if (isPro) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnknownCallerOptionCard(
                            icon = Icons.Outlined.Star,
                            title = "VIP contacts only",
                            description = "Only your manually-added VIP numbers ring through. Saved contacts and unknown callers are both silenced.",
                            selected = currentMode == UnknownCallerMode.VIP_ONLY,
                            badge = { ProBadge() },
                            onClick = {
                                viewModel.updatePolicy(
                                    policy.copy(
                                        vipContactsOnlyEnabled = true,
                                        allowContactsOnly = false,
                                        silenceUnknownNumbers = false,
                                        preset = BlockingPreset.CUSTOM,
                                    )
                                )
                            },
                        )
                        if (currentMode == UnknownCallerMode.VIP_ONLY) {
                            TextButton(
                                onClick = { openVipManager() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    if (vipContacts.isEmpty()) "Add VIP contacts"
                                    else "Manage VIP contacts (${vipContacts.size})",
                                )
                            }
                        }
                    }
                } else {
                    ProFeatureCard(
                        valueText = "Let through only the people who truly matter — your curated VIP list",
                        upgradeLabel = "Unlock VIP Contacts",
                        onUpgradeClick = onNavigateToPaywall,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "VIP contacts only",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Only your manually-added VIP numbers ring through",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                }
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

    if (showVipManager) {
        VipManagerSheet(
            vipContacts = vipContacts,
            searchResults = vipSearchResults,
            onQueryChange = { viewModel.setVipSearchQuery(it) },
            onAdd = { e164, label -> viewModel.addVipContact(e164, label) },
            onRemove = { hash -> viewModel.removeVipContact(hash) },
            onDismiss = { showVipManager = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VipManagerSheet(
    vipContacts: List<VipContactEntry>,
    searchResults: List<Triple<String, String, Boolean>>,
    onQueryChange: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "VIP Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (vipContacts.isNotEmpty()) {
                    Text(
                        "${vipContacts.size} added",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                placeholder = { Text("Search contacts to add…") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                if (vipContacts.isNotEmpty() && searchQuery.isBlank()) {
                    item {
                        Text(
                            "Your VIP list",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    items(vipContacts, key = { "vip_${it.numberHash}" }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(contact.displayLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemove(contact.numberHash) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider()
                    }
                }

                if (searchResults.isNotEmpty()) {
                    item {
                        Text(
                            "Add from contacts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    items(searchResults, key = { "result_${it.second}" }) { (name, e164, isAlreadyAdded) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (!isAlreadyAdded) Modifier.clickable { onAdd(e164, name) }
                                    else Modifier
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                Text(e164, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            if (isAlreadyAdded) {
                                Text("Added", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Outlined.PersonAdd, contentDescription = "Add", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider()
                    }
                } else if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            "No contacts found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }

            HorizontalDivider()
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text("Done")
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
    badge: (@Composable () -> Unit)? = null,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
                    )
                    badge?.invoke()
                }
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
