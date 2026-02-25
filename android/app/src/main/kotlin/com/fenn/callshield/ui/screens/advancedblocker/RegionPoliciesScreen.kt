package com.fenn.callshield.ui.screens.advancedblocker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.domain.model.BlockingPreset
import com.fenn.callshield.domain.model.CountryFilterMode
import com.fenn.callshield.util.HomeCountryProvider
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionPoliciesScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: AdvancedBlockingViewModel = hiltViewModel(),
) {
    val policy by viewModel.policy.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val homeCode = viewModel.homeCallingCode

    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Build sorted country list once — stable across recompositions
    val allCountries = remember {
        HomeCountryProvider.CALLING_CODES.keys
            .sortedBy { Locale("", it).displayCountry }
    }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) allCountries
        else allCountries.filter {
            Locale("", it).displayCountry.contains(searchQuery, ignoreCase = true) ||
                it.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Region Policies") },
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
                    "Block or silence calls from outside your home country. Numbers that don't start with $homeCode will be treated as international.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                PolicyToggleCard(
                    title = "Block international calls",
                    description = "Calls from non-$homeCode numbers will be silenced. Contacts are still allowed.",
                    checked = policy.blockInternational,
                    onCheckedChange = {
                        viewModel.updatePolicy(
                            policy.copy(
                                blockInternational = it,
                                preset = BlockingPreset.CUSTOM,
                            )
                        )
                    },
                )
            }

            // ── Country Filter section ──────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Country Filter",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!isPro) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "Pro",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Allow only calls from specific countries, or block calls from a list of countries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
            }

            if (isPro) {
                // Mode selector card
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CountryFilterMode.entries.forEachIndexed { index, mode ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updatePolicy(
                                                policy.copy(
                                                    countryFilterMode = mode,
                                                    // Clear the list when turning off
                                                    countryFilterList = if (mode == CountryFilterMode.OFF) emptySet() else policy.countryFilterList,
                                                    preset = BlockingPreset.CUSTOM,
                                                )
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    RadioButton(
                                        selected = policy.countryFilterMode == mode,
                                        onClick = null,
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            mode.displayName(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (policy.countryFilterMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                        Text(
                                            mode.description(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (policy.countryFilterMode != CountryFilterMode.OFF) {
                    // Summary line
                    item {
                        val count = policy.countryFilterList.size
                        Text(
                            if (count == 0) "No countries selected — tap to add"
                            else "$count ${if (count == 1) "country" else "countries"} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (count == 0)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            else
                                MaterialTheme.colorScheme.primary,
                            fontWeight = if (count > 0) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }

                    // Search bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search countries…") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    // Country rows
                    items(filteredCountries, key = { it }) { iso ->
                        val name = remember(iso) { Locale("", iso).displayCountry }
                        val isChecked = iso in policy.countryFilterList
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newList = if (isChecked)
                                        policy.countryFilterList - iso
                                    else
                                        policy.countryFilterList + iso
                                    viewModel.updatePolicy(
                                        policy.copy(
                                            countryFilterList = newList,
                                            preset = BlockingPreset.CUSTOM,
                                        )
                                    )
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                Text(
                                    HomeCountryProvider.CALLING_CODES[iso] ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            } else {
                // Locked state
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToPaywall),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    "Unlock Country Filter",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "Allow only or block specific countries with Pro.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun CountryFilterMode.displayName(): String = when (this) {
    CountryFilterMode.OFF -> "Off"
    CountryFilterMode.ALLOW_ONLY -> "Allow listed countries only"
    CountryFilterMode.BLOCK_LISTED -> "Block listed countries"
}

private fun CountryFilterMode.description(): String = when (this) {
    CountryFilterMode.OFF -> "Country filter is disabled"
    CountryFilterMode.ALLOW_ONLY -> "Only calls from selected countries are allowed through"
    CountryFilterMode.BLOCK_LISTED -> "Calls from selected countries are silenced"
}
