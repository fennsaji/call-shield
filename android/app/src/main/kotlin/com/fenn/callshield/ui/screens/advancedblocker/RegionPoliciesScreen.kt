package com.fenn.callshield.ui.screens.advancedblocker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    val homeIso = viewModel.homeIsoCode
    val homeCountryName = remember(homeIso) { Locale("", homeIso).displayCountry }

    var showCountryPicker by rememberSaveable { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Block or silence calls from outside your home country ($homeCountryName, $homeCode). Numbers that don't start with $homeCode will be treated as international.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            PolicyToggleCard(
                title = "Block international calls",
                description = "Only $homeCountryName ($homeCode) calls are allowed. Contacts are still allowed.",
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

            if (policy.blockInternational) {
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

                Text(
                    "Allow only calls from specific countries, or block calls from a list of countries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                if (isPro) {
                    // Mode selector card
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
                                            // Enforce home country membership:
                                            // ALLOW_ONLY → home must be in list (always allowed)
                                            // BLOCK_LISTED → home must NOT be in list (can't block home)
                                            val adjustedList = when (mode) {
                                                CountryFilterMode.ALLOW_ONLY -> policy.countryFilterList + homeIso
                                                CountryFilterMode.BLOCK_LISTED -> policy.countryFilterList - homeIso
                                                CountryFilterMode.OFF -> policy.countryFilterList
                                            }
                                            viewModel.updatePolicy(
                                                policy.copy(
                                                    countryFilterMode = mode,
                                                    countryFilterList = adjustedList,
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
                                            mode.description("$homeCountryName ($homeCode)"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Country picker button
                    if (policy.countryFilterMode != CountryFilterMode.OFF) {
                        val count = policy.countryFilterList.size
                        OutlinedButton(
                            onClick = { showCountryPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                if (count == 0) Icons.Outlined.Public else Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (count == 0) "Add countries"
                                else "Edit countries ($count selected)",
                            )
                        }
                    }
                } else {
                    // Locked state
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

        if (showCountryPicker) {
            CountryPickerSheet(
                selected = policy.countryFilterList,
                homeIso = homeIso,
                onSelectionChange = { newList ->
                    viewModel.updatePolicy(
                        policy.copy(
                            countryFilterList = newList,
                            preset = BlockingPreset.CUSTOM,
                        )
                    )
                },
                onDismiss = { showCountryPicker = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    selected: Set<String>,
    homeIso: String,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val allCountries = remember {
        HomeCountryProvider.CALLING_CODES.keys
            .sortedBy { Locale("", it).displayCountry }
    }
    val filteredCountries = remember(searchQuery, selected, homeIso) {
        val q = searchQuery.trim()
        val base = if (q.isBlank()) allCountries
        else allCountries.filter { iso ->
            Locale("", iso).displayCountry.contains(q, ignoreCase = true) ||
                iso.contains(q, ignoreCase = true) ||
                (HomeCountryProvider.CALLING_CODES[iso] ?: "").contains(q, ignoreCase = true)
        }
        // Home country always first, then selected, then rest
        base.sortedWith(
            compareByDescending<String> { it == homeIso }
                .thenByDescending { it in selected }
        )
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.85f),
        ) {
            // Title row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Choose countries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val count = selected.size
                if (count > 0) {
                    Text(
                        "$count selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Search bar — pinned, outside LazyColumn
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
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

            // Country list — fills remaining height; imePadding handles keyboard
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                items(filteredCountries, key = { it }) { iso ->
                    val name = remember(iso) { Locale("", iso).displayCountry }
                    val isChecked = iso in selected
                    val isHome = iso == homeIso
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isHome) Modifier  // locked — no click
                                else Modifier.clickable {
                                    onSelectionChange(
                                        if (isChecked) selected - iso else selected + iso
                                    )
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null,
                            enabled = !isHome,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isChecked || isHome) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (isHome) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                "Your region",
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                Icons.Outlined.Home,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                        border = null,
                                        modifier = Modifier.height(22.dp),
                                    )
                                }
                            }
                            Text(
                                HomeCountryProvider.CALLING_CODES[iso] ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
            ) {
                Text("Done")
            }
        }
    }
}

private fun CountryFilterMode.displayName(): String = when (this) {
    CountryFilterMode.OFF -> "Block All International Calls"
    CountryFilterMode.ALLOW_ONLY -> "Allow listed countries only"
    CountryFilterMode.BLOCK_LISTED -> "Block listed countries"
}

private fun CountryFilterMode.description(homeLabel: String = ""): String = when (this) {
    CountryFilterMode.OFF -> "All international calls blocked — except $homeLabel"
    CountryFilterMode.ALLOW_ONLY -> "Only calls from selected countries are allowed through"
    CountryFilterMode.BLOCK_LISTED -> "Calls from selected countries are silenced"
}
