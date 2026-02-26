package com.fenn.callshield.family

import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.billing.PlanType
import com.fenn.callshield.util.DeviceTokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the guardian's billing plan and propagates subscription state changes to the backend.
 *
 * When the guardian's plan changes to a non-family plan (cancelled / downgraded), all dependent
 * pairings are immediately revoked via POST /family-revoke so dependents lose access on next sync.
 *
 * When the guardian renews or upgrades to a family plan, POST /family-renew reactivates
 * pairings with a fresh expiry timestamp so dependents regain access automatically.
 *
 * Only runs when this device is a GUARDIAN — no-ops on DEPENDENT or unpaired devices.
 */
@Singleton
class FamilySubscriptionSyncer @Inject constructor(
    private val billingManager: BillingManager,
    private val familySyncRepository: FamilySyncRepository,
    private val familyTokenManager: FamilyTokenManager,
    private val deviceTokenManager: DeviceTokenManager,
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            billingManager.planType
                .collect { newPlan ->
                    // Only act if this device is currently the GUARDIAN
                    val role = familyTokenManager.observeRole().first()
                    if (role != FamilyRole.GUARDIAN) return@collect

                    val guardianHash = deviceTokenManager.deviceTokenHash

                    if (newPlan.isFamilyPlan()) {
                        val expiresAt = billingManager.subscriptionExpiresAt.value
                            ?.let { Instant.ofEpochMilli(it).toString() }
                        familySyncRepository.renewSubscription(
                            guardianDeviceHash = guardianHash,
                            planType = newPlan.name,
                            subscriptionExpiresAt = expiresAt,
                        )
                    } else {
                        familySyncRepository.revokeSubscription(guardianHash)
                    }
                }
        }
    }

    private fun PlanType.isFamilyPlan(): Boolean =
        this == PlanType.FAMILY_ANNUAL ||
        this == PlanType.FAMILY_LIFETIME ||
        this == PlanType.PROMO_FAMILY
}
