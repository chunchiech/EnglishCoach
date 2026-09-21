package com.andy.englishcoach.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface consumed by ViewModels to observe the current entitlement state
 * without direct dependency on billing client implementation details.
 */
interface PremiumEntitlementProvider {
    val entitlement: StateFlow<PremiumEntitlement>
    val isPremium: Boolean get() = entitlement.value.isPremium
}
