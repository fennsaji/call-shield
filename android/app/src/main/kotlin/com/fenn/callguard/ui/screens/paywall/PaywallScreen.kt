package com.fenn.callguard.ui.screens.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callguard.BuildConfig
import com.fenn.callguard.R
import com.fenn.callguard.ui.theme.LocalSuccessColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    fromTrigger: Boolean = false,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val successColor = LocalSuccessColor.current

    LaunchedEffect(Unit) {
        viewModel.loadProducts()
    }

    LaunchedEffect(state.purchaseSuccess) {
        if (state.purchaseSuccess) onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Hero gradient section ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.pro_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // PRD §3.11: context-aware copy if triggered by first spam call
                if (fromTrigger) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
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
                        "Get unlimited protection and advanced controls",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Feature list
                ProFeatureRow(Icons.Filled.Block, stringResource(R.string.pro_feature_unlimited))
                ProFeatureRow(Icons.Filled.History, stringResource(R.string.pro_feature_history))
                ProFeatureRow(Icons.Outlined.FilterList, stringResource(R.string.pro_feature_prefix))
                ProFeatureRow(Icons.Filled.People, "Auto-block high-confidence spam calls")

                HorizontalDivider()

                if (state.loading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    // ── Annual plan card (primary) ─────────────────────────────
                    ElevatedCard(
                        onClick = {
                            val product = state.annualProduct
                            if (product != null) viewModel.purchase(context, product)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium,
                            ),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        enabled = state.annualProduct != null,
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column {
                                Text(
                                    if (fromTrigger) stringResource(R.string.pro_trial_cta)
                                    else (state.annualProduct?.subscriptionOfferDetails
                                        ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                                        ?.firstOrNull()?.formattedPrice
                                        ?.let { "$it / year" }
                                        ?: stringResource(R.string.pro_price_annual)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "Save 32% vs monthly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                )
                            }
                            // Best value badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(6.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    "Best value",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }

                    // ── Monthly plan card (secondary) ──────────────────────────
                    OutlinedCard(
                        onClick = {
                            val product = state.monthlyProduct
                            if (product != null) viewModel.purchase(context, product)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.monthlyProduct != null,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                state.monthlyProduct?.subscriptionOfferDetails
                                    ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                                    ?.firstOrNull()?.formattedPrice
                                    ?.let { "$it / month" }
                                    ?: stringResource(R.string.pro_price_monthly),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "Monthly, cancel anytime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
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

                if (BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = { viewModel.debugSimulatePurchase() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("[DEBUG] Simulate Purchase")
                    }
                }

                HorizontalDivider()

                // ── Family upsell (PRD §3.11) ─────────────────────────────────
                FamilyWaitlistSection(
                    email = state.familyWaitlistEmail,
                    joined = state.familyWaitlistJoined,
                    onEmailChange = viewModel::onFamilyEmailChange,
                    onJoin = viewModel::joinFamilyWaitlist,
                    successColor = successColor,
                )

                Text(
                    stringResource(R.string.pro_terms),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FamilyWaitlistSection(
    email: String,
    joined: Boolean,
    onEmailChange: (String) -> Unit,
    onJoin: () -> Unit,
    successColor: Color,
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
                color = successColor,
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
private fun ProFeatureRow(icon: ImageVector, feature: String) {
    val successColor = LocalSuccessColor.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = successColor,
        )
        Text(feature, style = MaterialTheme.typography.bodyLarge)
    }
}
