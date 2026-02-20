package com.fenn.callguard.ui.screens.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callguard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    fromTrigger: Boolean = false,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadProducts()
    }

    LaunchedEffect(state.purchaseSuccess) {
        if (state.purchaseSuccess) onDismiss()
    }

    LaunchedEffect(state.familyWaitlistJoined) {
        // no-op — shown inline
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pro_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // PRD §3.11: context-aware copy if triggered by first spam call
            if (fromTrigger) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.pro_trial_trigger_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            stringResource(R.string.pro_trial_trigger_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            } else {
                Text(
                    "Upgrade to CallGuard Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Get unlimited protection and advanced controls",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            ProFeatureRow(stringResource(R.string.pro_feature_unlimited))
            ProFeatureRow(stringResource(R.string.pro_feature_history))
            ProFeatureRow(stringResource(R.string.pro_feature_prefix))
            ProFeatureRow("Auto-block high-confidence spam calls")

            HorizontalDivider()

            if (state.loading) {
                CircularProgressIndicator()
            } else {
                // Annual plan (primary)
                Button(
                    onClick = {
                        val product = state.annualProduct
                        if (product != null) viewModel.purchase(context, product)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.annualProduct != null,
                ) {
                    Text(
                        if (fromTrigger) stringResource(R.string.pro_trial_cta)
                        else (state.annualProduct?.subscriptionOfferDetails
                            ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                            ?.firstOrNull()?.formattedPrice
                            ?.let { "$it / year" }
                            ?: stringResource(R.string.pro_price_annual))
                    )
                }

                // Monthly plan (secondary)
                OutlinedButton(
                    onClick = {
                        val product = state.monthlyProduct
                        if (product != null) viewModel.purchase(context, product)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.monthlyProduct != null,
                ) {
                    Text(
                        state.monthlyProduct?.subscriptionOfferDetails
                            ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                            ?.firstOrNull()?.formattedPrice
                            ?.let { "$it / month" }
                            ?: stringResource(R.string.pro_price_monthly)
                    )
                }

                TextButton(
                    onClick = { viewModel.restorePurchase() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.pro_restore))
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // ── Family upsell (PRD §3.11) ─────────────────────────────────────
            HorizontalDivider()
            FamilyWaitlistSection(
                email = state.familyWaitlistEmail,
                joined = state.familyWaitlistJoined,
                onEmailChange = viewModel::onFamilyEmailChange,
                onJoin = viewModel::joinFamilyWaitlist,
            )

            Text(
                stringResource(R.string.pro_terms),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun FamilyWaitlistSection(
    email: String,
    joined: Boolean,
    onEmailChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.pro_family_upsell),
            style = MaterialTheme.typography.titleMedium,
        )
        if (joined) {
            Text(
                stringResource(R.string.pro_family_joined),
                color = Color(0xFF22C55E),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.pro_family_email_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth(),
                enabled = email.contains("@"),
            ) {
                Text(stringResource(R.string.pro_family_waitlist))
            }
        }
    }
}

@Composable
private fun ProFeatureRow(feature: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF22C55E),
        )
        Text(feature, style = MaterialTheme.typography.bodyLarge)
    }
}
