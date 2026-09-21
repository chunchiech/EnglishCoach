package com.andy.englishcoach.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Billing abstraction layer.
 * Decouples UI and Domain logic from Google Play Billing Client.
 */
interface BillingRepository : PremiumEntitlementProvider {
    val connectionState: StateFlow<BillingConnectionState>
    val catalogState: StateFlow<BillingProductCatalog>
    fun getProducts(): List<PremiumProductInfo>
    suspend fun refreshEntitlements(): PremiumEntitlement
    suspend fun purchase(product: PremiumProduct): BillingResult
    suspend fun restorePurchases(): BillingResult
}
