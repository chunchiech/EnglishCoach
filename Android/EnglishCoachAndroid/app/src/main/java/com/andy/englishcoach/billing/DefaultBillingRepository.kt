package com.andy.englishcoach.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default production implementation of BillingRepository.
 * Operates as a safe placeholder until Google Play Billing Client is connected.
 * Defaults safely to Free, returns the official product list, and gracefully notifies
 * the user that billing is pending Google Play Console release.
 */
class DefaultBillingRepository : BillingRepository {

    private val _entitlement = MutableStateFlow<PremiumEntitlement>(PremiumEntitlement.Free)
    override val entitlement: StateFlow<PremiumEntitlement> = _entitlement.asStateFlow()

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Connected)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _catalogState = MutableStateFlow<BillingProductCatalog>(
        BillingProductCatalog.Available(getProducts())
    )
    override val catalogState: StateFlow<BillingProductCatalog> = _catalogState.asStateFlow()

    override fun getProducts(): List<PremiumProductInfo> {
        return PremiumProduct.entries.map { it.productInfo }
    }

    override suspend fun refreshEntitlements(): PremiumEntitlement {
        return _entitlement.value
    }

    override suspend fun purchase(product: PremiumProduct): BillingResult {
        return BillingResult.Error("Google Play 商店尚未開放正式購買，敬請期待正式上架！")
    }

    override suspend fun restorePurchases(): BillingResult {
        return BillingResult.Error("尚未找到已購買的 Google Play 訂閱記錄")
    }
}
