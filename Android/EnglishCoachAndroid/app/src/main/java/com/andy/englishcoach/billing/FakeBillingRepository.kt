package com.andy.englishcoach.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory deterministic BillingRepository for testing, development, and previews.
 * Allows simulating purchase success, failure, cancellation, pending state, and restore.
 */
class FakeBillingRepository(
    initialEntitlement: PremiumEntitlement = PremiumEntitlement.Free
) : BillingRepository {

    private val _entitlement = MutableStateFlow<PremiumEntitlement>(initialEntitlement)
    override val entitlement: StateFlow<PremiumEntitlement> = _entitlement.asStateFlow()

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Connected)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _catalogState = MutableStateFlow<BillingProductCatalog>(
        BillingProductCatalog.Available(getProducts())
    )
    override val catalogState: StateFlow<BillingProductCatalog> = _catalogState.asStateFlow()

    var simulateCancellation: Boolean = false
    var simulateFailure: Boolean = false
    var simulatePending: Boolean = false
    var failureErrorMessage: String = "Google Play 服務暫時無法連線"
    var pendingMessage: String = "付款處理中，完成付款後 Premium 將會啟用"

    var mockActiveSubscriptionPurchaseToken: String? = null
    override val activeSubscriptionPurchaseToken: String?
        get() = mockActiveSubscriptionPurchaseToken ?: (recordedPurchases.lastOrNull {
            val p = PremiumProduct.entries.firstOrNull { prod -> prod.productId == it.productId }
            p?.isSubscription == true && it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
        }?.purchaseToken)

    private var activeProduct: PremiumProduct? = (initialEntitlement as? PremiumEntitlement.Premium)?.product
    val recordedPurchases = mutableListOf<PurchaseRecord>()

    fun setConnectionStateForTesting(state: BillingConnectionState) {
        _connectionState.value = state
    }

    fun setCatalogStateForTesting(state: BillingProductCatalog) {
        _catalogState.value = state
    }

    override fun getProducts(): List<PremiumProductInfo> {
        return PremiumProduct.entries.map { it.productInfo }
    }

    override suspend fun refreshEntitlements(): PremiumEntitlement {
        return _entitlement.value
    }

    override suspend fun purchase(product: PremiumProduct): BillingResult {
        if (simulateCancellation) {
            return BillingResult.Cancelled("已取消購買")
        }
        if (simulateFailure) {
            return BillingResult.Error(failureErrorMessage)
        }
        if (simulatePending) {
            return BillingResult.Pending(pendingMessage)
        }

        activeProduct = product
        val newEntitlement = PremiumEntitlement.Premium(product)
        _entitlement.value = newEntitlement
        recordedPurchases.add(
            PurchaseRecord(
                productId = product.productId,
                purchaseToken = "fake_token_${product.productId}_${System.currentTimeMillis()}",
                purchaseState = 1,
                isAcknowledged = true,
                purchaseTime = System.currentTimeMillis()
            )
        )
        return BillingResult.Success(newEntitlement)
    }

    override suspend fun restorePurchases(): BillingResult {
        if (simulateFailure) {
            return BillingResult.Error(failureErrorMessage)
        }
        if (simulatePending) {
            return BillingResult.Pending(pendingMessage)
        }

        val productToRestore = activeProduct
        return if (productToRestore != null) {
            val restored = PremiumEntitlement.Premium(productToRestore)
            _entitlement.value = restored
            BillingResult.Success(restored)
        } else {
            _entitlement.value = PremiumEntitlement.Free
            BillingResult.Success(PremiumEntitlement.Free)
        }
    }

    /**
     * Test helper to set entitlement directly in test fixtures.
     */
    fun setEntitlementForTesting(entitlement: PremiumEntitlement) {
        _entitlement.value = entitlement
        activeProduct = (entitlement as? PremiumEntitlement.Premium)?.product
    }
}
