package com.fenn.callshield.ui.screens.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenn.callshield.BuildConfig
import com.fenn.callshield.R
import com.fenn.callshield.billing.PlanType
import com.fenn.callshield.ui.theme.LocalDangerColor
import com.fenn.callshield.ui.theme.LocalSuccessColor
import com.fenn.callshield.ui.theme.LocalWarningColor

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    fromTrigger: Boolean = false,
    scrollToRestore: Boolean = false,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { viewModel.loadProducts() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // When purchase succeeds and there's an active subscription that needs manual cancellation,
    // show a dialog first. Otherwise dismiss immediately.
    if (state.purchaseSuccess && state.showSubscriptionCancelPrompt) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Lifetime Pro Activated!") },
            text = {
                Text(
                    "You still have an active subscription running. " +
                    "Please cancel it on Google Play to avoid future charges. " +
                    "Your Lifetime access is already active."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(
                                "https://play.google.com/store/account/subscriptions?package=${context.packageName}"
                            )
                        )
                        context.startActivity(intent)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Cancel Subscription") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Do it later") }
            },
        )
    }
    LaunchedEffect(state.purchaseSuccess) {
        if (state.purchaseSuccess && !state.showSubscriptionCancelPrompt) onDismiss()
    }
    LaunchedEffect(scrollToRestore, scrollState.maxValue) {
        if (scrollToRestore && scrollState.maxValue > 0) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            HeroSection(fromTrigger = fromTrigger)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                ProFeaturesList()

                if (state.loading) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                } else {
                    ProPlanSection(state = state, context = context, viewModel = viewModel)
                }

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (state.hasPendingPurchase) { PendingPaymentBanner() }

                PromoCodeRow(state = state, viewModel = viewModel)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = { viewModel.restorePurchase() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.pro_restore)) }
                    Text(
                        "Purchases are tied to your Google account. Restore only works if you're signed in with the same account used when you originally subscribed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                }

                if (BuildConfig.DEBUG) {
                    DebugSimulateSection(onSimulate = { viewModel.debugSimulatePlan(it) })
                }

                Text(
                    stringResource(R.string.pro_terms),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Floating close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(fromTrigger: Boolean) {
    val primary    = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(
                Brush.verticalGradient(
                    0f   to primary.copy(alpha = 0.22f),
                    0.6f to primary.copy(alpha = 0.06f),
                    1f   to background,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 48.dp, bottom = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(48.dp), tint = primary)
            }
            Text(
                stringResource(R.string.pro_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                if (fromTrigger) stringResource(R.string.pro_trial_trigger_title)
                else "Complete call protection — forever.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

// ── Feature list ──────────────────────────────────────────────────────────────

@Composable
private fun ProFeaturesList() {
    val dangerColor  = LocalDangerColor.current
    val successColor = LocalSuccessColor.current
    val warningColor = LocalWarningColor.current
    val primary      = MaterialTheme.colorScheme.primary
    val tertiary     = MaterialTheme.colorScheme.tertiary
    val secondary    = MaterialTheme.colorScheme.secondary

    val features: List<Triple<ImageVector, Color, String>> = listOf(
        Triple(Icons.Filled.Block,            dangerColor,  "Auto-block spam before it rings"),
        Triple(Icons.Outlined.VisibilityOff,  tertiary,     "Block hidden & private numbers"),
        Triple(Icons.Outlined.FilterList,     primary,      "Unlimited pattern rules"),
        Triple(Icons.Filled.DarkMode,     secondary, "Night Guard — custom hours & reject"),
        Triple(Icons.Filled.Language,     primary,   "International call blocking"),
        Triple(Icons.Filled.SystemUpdate, tertiary,  "Priority spam database updates"),
    )

    Column {
        Text(
            "Everything in Pro",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        features.forEach { (icon, tint, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = tint)
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ── Plan section ──────────────────────────────────────────────────────────────

@Composable
private fun ProPlanSection(
    state: PaywallState,
    context: android.content.Context,
    viewModel: PaywallViewModel,
) {
    val primary  = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    // Annual pricing phases — first phase = intro (if multiple), last = base
    val annualPhases = state.annualProduct?.subscriptionOfferDetails
        ?.firstOrNull()?.pricingPhases?.pricingPhaseList ?: emptyList()
    val annualIntroPhase = if (annualPhases.size > 1) annualPhases.first() else null
    val annualBasePrice  = annualPhases.lastOrNull()?.formattedPrice
        ?: stringResource(R.string.pro_price_annual)

    // Monthly pricing phases
    val monthlyPhases = state.monthlyProduct?.subscriptionOfferDetails
        ?.firstOrNull()?.pricingPhases?.pricingPhaseList ?: emptyList()
    val monthlyIntroPhase = if (monthlyPhases.size > 1) monthlyPhases.first() else null
    val monthlyBasePrice  = monthlyPhases.lastOrNull()?.formattedPrice
        ?: stringResource(R.string.pro_price_monthly)

    // Dynamic savings % from base prices
    val annualBaseMicros  = annualPhases.lastOrNull()?.priceAmountMicros
    val monthlyBaseMicros = monthlyPhases.lastOrNull()?.priceAmountMicros
    val savingsText = if (annualBaseMicros != null && monthlyBaseMicros != null && monthlyBaseMicros > 0) {
        val savings = ((monthlyBaseMicros - annualBaseMicros / 12.0) / monthlyBaseMicros * 100).toInt()
        "Save $savings% vs monthly · Cancel anytime"
    } else {
        "Save 32% vs monthly · Cancel anytime"
    }

    // Lifetime discount — compare Play price against ₹699 base
    val lifetimeCurrentMicros = state.lifetimeProduct?.oneTimePurchaseOfferDetails?.priceAmountMicros
    val lifetimeBaseMicros    = 69900000L // ₹699 in micros
    val lifetimeHasDiscount   = lifetimeCurrentMicros != null && lifetimeCurrentMicros < lifetimeBaseMicros
    val lifetimeDisplayPrice  = state.lifetimeProduct?.oneTimePurchaseOfferDetails?.formattedPrice
        ?: stringResource(R.string.pro_price_lifetime)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Choose your plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Annual — solid primary gradient (the CTA)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.85f))))
                .then(
                    if (state.annualProduct != null)
                        Modifier.clickable { viewModel.purchase(context, state.annualProduct!!) }
                    else Modifier
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ANNUAL · BEST VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    if (annualIntroPhase != null) {
                        Text(
                            "INTRO OFFER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                if (annualIntroPhase != null) {
                    Text(
                        "$annualBasePrice / year",
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    Text(
                        "${annualIntroPhase.formattedPrice} / first year",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                } else {
                    Text(
                        "$annualBasePrice / year",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
                Text(
                    savingsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
                if (annualIntroPhase != null) {
                    Text(
                        "Then $annualBasePrice/year from year 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        // Monthly
        ElevatedCard(
            onClick = { state.monthlyProduct?.let { viewModel.purchase(context, it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "MONTHLY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                    if (monthlyIntroPhase != null) {
                        Text(
                            "$monthlyBasePrice / month",
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                        Text(
                            "${monthlyIntroPhase.formattedPrice} / first month",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Text(
                            "$monthlyBasePrice / month",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    "Cancel\nanytime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.End,
                )
            }
        }

        // Lifetime
        ElevatedCard(
            onClick = { state.lifetimeProduct?.let { viewModel.purchase(context, it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(13.dp), tint = tertiary)
                        Text("LIFETIME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tertiary)
                    }
                    if (lifetimeHasDiscount) {
                        Text(
                            stringResource(R.string.pro_price_lifetime),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                    }
                    Text(
                        lifetimeDisplayPrice,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("One-time", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = tertiary)
                    Text("No renewals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                }
            }
        }
    }
}

// ── Promo code row ────────────────────────────────────────────────────────────

@Composable
private fun PromoCodeRow(state: PaywallState, viewModel: PaywallViewModel) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.promoCode,
                onValueChange = { viewModel.onPromoCodeChange(it) },
                placeholder = { Text("Promo code") },
                singleLine = true,
                isError = state.promoError,
                modifier = Modifier.weight(1f),
                supportingText = if (state.promoError) {
                    { Text(state.promoErrorMessage, color = MaterialTheme.colorScheme.error) }
                } else null,
            )
            Button(
                onClick = { viewModel.redeemPromoCode() },
                enabled = state.promoCode.isNotBlank(),
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Apply") }
        }
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/redeem"))
                    .setPackage("com.android.vending")
                context.startActivity(intent)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                "Redeem a Google Play code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
    }
}

// ── Pending payment banner ────────────────────────────────────────────────────

@Composable
private fun PendingPaymentBanner() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.HourglassTop, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Payment processing", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    "Your payment is being verified. Pro will activate automatically once confirmed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// ── Debug simulate section ────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebugSimulateSection(onSimulate: (PlanType) -> Unit) {
    var selected by remember { mutableStateOf<PlanType?>(null) }
    val plans = listOf(
        PlanType.PRO_MONTHLY  to "Monthly",
        PlanType.PRO_ANNUAL   to "Annual",
        PlanType.PRO_LIFETIME to "Lifetime",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("DEBUG — Simulate Purchase", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                plans.forEach { (plan, label) ->
                    FilterChip(
                        selected = selected == plan,
                        onClick = { selected = plan },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.height(32.dp),
                    )
                }
            }
            Button(
                onClick = { selected?.let { onSimulate(it) } },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                Text(
                    if (selected != null) "Activate ${plans.first { it.first == selected }.second}"
                    else "Select a plan above",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
