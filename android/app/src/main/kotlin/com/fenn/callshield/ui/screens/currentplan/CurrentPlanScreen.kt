package com.fenn.callshield.ui.screens.currentplan

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.ProductDetails
import com.fenn.callshield.BuildConfig
import com.fenn.callshield.billing.PlanType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentPlanScreen(
    onBack: () -> Unit,
    viewModel: CurrentPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadProducts() }

    LaunchedEffect(state.switchSuccess) {
        if (state.switchSuccess) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Current plan card ────────────────────────────────────────────
            item {
                CurrentPlanCard(planType = state.planType)
            }

            // ── Error ────────────────────────────────────────────────────────
            if (state.error != null) {
                item {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            // ── Switch plan (monthly ↔ annual) ───────────────────────────────
            val switchOptions = buildSwitchOptions(state)
            if (switchOptions.isNotEmpty()) {
                item { SectionLabel("Switch Plan") }
                switchOptions.forEach { option ->
                    item {
                        PlanOptionCard(
                            title = option.title,
                            price = option.price,
                            badge = option.badge,
                            badgeColor = MaterialTheme.colorScheme.tertiary,
                            enabled = option.product != null,
                            onClick = { option.product?.let { viewModel.switchPlan(context, it) } },
                        )
                    }
                }
            }

            // ── Manage subscription ──────────────────────────────────────────
            item {
                SectionLabel("Manage")
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.openManageSubscriptions(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (state.planType == PlanType.PRO_LIFETIME) "View Purchase on Google Play"
                        else "Manage Subscription on Google Play"
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when (state.planType) {
                        PlanType.PRO_LIFETIME ->
                            "Lifetime purchases cannot be cancelled. Contact Google Play support for refunds."
                        PlanType.PROMO_PRO ->
                            "Your access was granted via a promo code and is not managed through Google Play."
                        else ->
                            "Cancel, pause, or update your payment method via Google Play."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            // ── Debug simulate ───────────────────────────────────────────────
            if (BuildConfig.DEBUG) {
                item {
                    DebugSimulateSection(onSimulate = { viewModel.debugSimulatePlan(it) })
                }
            }
        }
    }
}

// ── Current plan card ─────────────────────────────────────────────────────────

@Composable
private fun CurrentPlanCard(planType: PlanType) {
    val primary = MaterialTheme.colorScheme.primary
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.12f), primary.copy(alpha = 0.04f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        planType.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = primary,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Current Plan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    Text(
                        planType.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        planType.displayPrice(),
                        style = MaterialTheme.typography.bodySmall,
                        color = primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (planType != PlanType.NONE) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = primary,
                    )
                }
            }
        }
    }
}

// ── Plan option card ──────────────────────────────────────────────────────────

@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    badge: String?,
    badgeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Text(
                    price,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                ),
            ) {
                Text("Switch", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    )
}

// ── Debug simulate section ────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebugSimulateSection(onSimulate: (PlanType) -> Unit) {
    var selected by remember { mutableStateOf<PlanType?>(null) }
    val plans = listOf(
        PlanType.NONE         to "Free",
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
            Text(
                "DEBUG — Simulate Plan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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
                    if (selected != null) "Switch to ${plans.first { it.first == selected }.second}"
                    else "Select a plan above",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

// ── Plan option model ─────────────────────────────────────────────────────────

// product is nullable — sections always show even when Play Billing can't load
private data class PlanOption(
    val title: String,
    val price: String,
    val badge: String?,
    val product: ProductDetails?,
)

private fun buildSwitchOptions(state: CurrentPlanState): List<PlanOption> {
    val options = mutableListOf<PlanOption>()
    when (state.planType) {
        PlanType.PRO_MONTHLY ->
            options += PlanOption("Pro Annual", "₹399 / year", "Save 32%", state.annualProduct)
        PlanType.PRO_ANNUAL ->
            options += PlanOption("Pro Monthly", "₹49 / month", null, state.monthlyProduct)
        else -> Unit
    }
    return options
}

// ── PlanType helpers ──────────────────────────────────────────────────────────

private fun PlanType.displayName(): String = when (this) {
    PlanType.NONE         -> "Free"
    PlanType.PRO_MONTHLY  -> "Pro Monthly"
    PlanType.PRO_ANNUAL   -> "Pro Annual"
    PlanType.PRO_LIFETIME -> "Pro Lifetime"
    PlanType.PROMO_PRO    -> "Pro (Promo)"
}

private fun PlanType.displayPrice(): String = when (this) {
    PlanType.NONE         -> "Free"
    PlanType.PRO_MONTHLY  -> "₹49 / month"
    PlanType.PRO_ANNUAL   -> "₹399 / year"
    PlanType.PRO_LIFETIME -> "₹699 — one-time"
    PlanType.PROMO_PRO    -> "Promotional grant"
}

private fun PlanType.icon(): ImageVector = when (this) {
    else -> Icons.Outlined.WorkspacePremium
}
