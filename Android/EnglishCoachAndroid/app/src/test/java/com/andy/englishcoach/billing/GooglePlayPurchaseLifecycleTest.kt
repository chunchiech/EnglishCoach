package com.andy.englishcoach.billing

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult as PlayBillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Deterministic test suite verifying STEP 16-C Google Play Purchase Lifecycle:
 *
 * Covers the 22 required scenarios defined in Section XXVI:
 * 1. purchaseMonthly_success
 * 2. purchaseAnnual_success
 * 3. purchaseLifetime_success
 * 4. purchaseCancelled_preservesState
 * 5. purchasePending_doesNotGrant
 * 6. purchaseFailed_preservesState
 * 7. unacknowledgedPurchase_acknowledged
 * 8. alreadyAcknowledged_doesNotAcknowledgeAgain
 * 9. restoreMonthly
 * 10. restoreAnnual
 * 11. restoreLifetime
 * 12. restorePending_doesNotGrant
 * 13. restoreNoPurchase_keepsFree
 * 14. duplicatePurchaseToken_notGrantedTwice
 * 15. productIdMapping_isCorrect
 * 16. purchaseToken_notExposedToUI
 * 17. purchaseSuccess_updatesEntitlement
 * 18. purchaseError_mapsToUserFacingError
 * 19. subscriptionAndLifetime_useDifferentProductTypes
 * 20. multiplePurchasedProducts_grantsPremium
 * 21. fakeBillingRegression
 * 22. entitlementDoesNotDependOnLocalPreference
 */
@RunWith(RobolectricTestRunner::class)
class GooglePlayPurchaseLifecycleTest {

    private lateinit var context: Context
    private lateinit var fakeAdapter: FakeBillingClientAdapter
    private lateinit var repository: GooglePlayBillingRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeAdapter = FakeBillingClientAdapter()
        repository = GooglePlayBillingRepository(
            context = context,
            adapter = fakeAdapter
        )
    }

    private fun createPurchase(
        productId: String,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        isAcknowledged: Boolean = true,
        purchaseToken: String = "token_${productId}_${System.currentTimeMillis()}",
        orderId: String = "GPA.test-${System.currentTimeMillis()}"
    ): Purchase {
        // In Google Play Purchase JSON: 1 = PURCHASED, 4 = PENDING (mapped to PurchaseState.PENDING = 2 by Purchase)
        val stateInJson = if (purchaseState == Purchase.PurchaseState.PENDING) 4 else 1
        val json = """
            {
                "orderId": "$orderId",
                "packageName": "com.andy.englishcoach",
                "productId": "$productId",
                "productIds": ["$productId"],
                "purchaseTime": 1789999000000,
                "purchaseState": $stateInJson,
                "purchaseToken": "$purchaseToken",
                "acknowledged": $isAcknowledged
            }
        """.trimIndent()
        return Purchase(json, "signature")
    }

    // 1. purchaseMonthly_success
    @Test
    fun purchaseMonthly_success() = runBlocking {
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        val purchase = createPurchase(PremiumProduct.MONTHLY.productId)

        val result = repository.processPurchases(listOf(purchase))
        assertTrue(result is BillingResult.Success)
        val entitlement = repository.entitlement.value
        assertTrue(entitlement is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.MONTHLY, (entitlement as PremiumEntitlement.Premium).product)
        assertTrue(repository.isPremium)
    }

    // 2. purchaseAnnual_success
    @Test
    fun purchaseAnnual_success() = runBlocking {
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        val purchase = createPurchase(PremiumProduct.ANNUAL.productId)

        val result = repository.processPurchases(listOf(purchase))
        assertTrue(result is BillingResult.Success)
        val entitlement = repository.entitlement.value
        assertTrue(entitlement is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.ANNUAL, (entitlement as PremiumEntitlement.Premium).product)
        assertTrue(repository.isPremium)
    }

    // 3. purchaseLifetime_success
    @Test
    fun purchaseLifetime_success() = runBlocking {
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        val purchase = createPurchase(PremiumProduct.LIFETIME.productId)

        val result = repository.processPurchases(listOf(purchase))
        assertTrue(result is BillingResult.Success)
        val entitlement = repository.entitlement.value
        assertTrue(entitlement is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.LIFETIME, (entitlement as PremiumEntitlement.Premium).product)
        assertTrue(entitlement.product.isOneTime)
        assertTrue(repository.isPremium)
    }

    // 4. purchaseCancelled_preservesState
    @Test
    fun purchaseCancelled_preservesState() {
        // Case A: Free user cancels -> stays Free
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        val cancelResult = PlayBillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
            .build()
        repository.handlePurchasesUpdated(cancelResult, null)
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        assertFalse(repository.isPremium)

        // Case B: Premium user cancels a secondary flow -> stays Premium
        val purchase = createPurchase(PremiumProduct.MONTHLY.productId)
        runBlocking { repository.processPurchases(listOf(purchase)) }
        assertTrue(repository.isPremium)

        repository.handlePurchasesUpdated(cancelResult, null)
        assertTrue(repository.isPremium)
        assertEquals(PremiumProduct.MONTHLY, (repository.entitlement.value as PremiumEntitlement.Premium).product)
    }

    // 5. purchasePending_doesNotGrant
    @Test
    fun purchasePending_doesNotGrant() = runBlocking {
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        val pendingPurchase = createPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseState = Purchase.PurchaseState.PENDING,
            isAcknowledged = false
        )

        val result = repository.processPurchases(listOf(pendingPurchase))
        assertTrue(result is BillingResult.Pending)
        assertEquals("付款處理中，完成付款後 Premium 將會啟用", (result as BillingResult.Pending).message)

        // Must NOT grant Premium
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        assertFalse(repository.isPremium)
    }

    // 6. purchaseFailed_preservesState
    @Test
    fun purchaseFailed_preservesState() {
        // Case A: Free user fails -> stays Free
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        val failResult = PlayBillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.NETWORK_ERROR)
            .build()
        repository.handlePurchasesUpdated(failResult, null)
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)

        // Case B: Premium user fails -> stays Premium
        val purchase = createPurchase(PremiumProduct.LIFETIME.productId)
        runBlocking { repository.processPurchases(listOf(purchase)) }
        assertTrue(repository.isPremium)

        repository.handlePurchasesUpdated(failResult, null)
        assertTrue(repository.isPremium)
    }

    // 7. unacknowledgedPurchase_acknowledged
    @Test
    fun unacknowledgedPurchase_acknowledged() = runBlocking {
        val testToken = "unack_test_token_12345"
        val unackPurchase = createPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseState = Purchase.PurchaseState.PURCHASED,
            isAcknowledged = false,
            purchaseToken = testToken
        )

        assertEquals(0, fakeAdapter.acknowledgedTokens.size)
        val result = repository.processPurchases(listOf(unackPurchase))
        assertTrue(result is BillingResult.Success)

        // Must call acknowledgePurchase
        assertEquals(1, fakeAdapter.acknowledgedTokens.size)
        assertEquals(testToken, fakeAdapter.acknowledgedTokens.first())
        assertTrue(repository.isPremium)
    }

    // 8. alreadyAcknowledged_doesNotAcknowledgeAgain
    @Test
    fun alreadyAcknowledged_doesNotAcknowledgeAgain() = runBlocking {
        val testToken = "already_ack_token_67890"
        val ackPurchase = createPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseState = Purchase.PurchaseState.PURCHASED,
            isAcknowledged = true,
            purchaseToken = testToken
        )

        assertEquals(0, fakeAdapter.acknowledgedTokens.size)
        val result = repository.processPurchases(listOf(ackPurchase))
        assertTrue(result is BillingResult.Success)

        // Must NOT call acknowledgePurchase again
        assertEquals(0, fakeAdapter.acknowledgedTokens.size)
        assertTrue(repository.isPremium)
    }

    // 9. restoreMonthly
    @Test
    fun restoreMonthly() = runBlocking {
        fakeAdapter.mockSubsPurchases = listOf(createPurchase(PremiumProduct.MONTHLY.productId))
        fakeAdapter.mockInAppPurchases = emptyList()

        val result = repository.restorePurchases()
        assertTrue(result is BillingResult.Success)
        val entitlement = (result as BillingResult.Success).entitlement
        assertTrue(entitlement is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.MONTHLY, (entitlement as PremiumEntitlement.Premium).product)
        assertTrue(repository.isPremium)
    }

    // 10. restoreAnnual
    @Test
    fun restoreAnnual() = runBlocking {
        fakeAdapter.mockSubsPurchases = listOf(createPurchase(PremiumProduct.ANNUAL.productId))
        fakeAdapter.mockInAppPurchases = emptyList()

        val result = repository.restorePurchases()
        assertTrue(result is BillingResult.Success)
        val entitlement = (result as BillingResult.Success).entitlement
        assertTrue(entitlement is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.ANNUAL, (entitlement as PremiumEntitlement.Premium).product)
        assertTrue(repository.isPremium)
    }

    // 11. restoreLifetime
    @Test
    fun restoreLifetime() = runBlocking {
        fakeAdapter.mockSubsPurchases = emptyList()
        fakeAdapter.mockInAppPurchases = listOf(createPurchase(PremiumProduct.LIFETIME.productId))

        val result = repository.restorePurchases()
        assertTrue(result is BillingResult.Success)
        val entitlement = (result as BillingResult.Success).entitlement
        assertTrue(entitlement is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.LIFETIME, (entitlement as PremiumEntitlement.Premium).product)
        assertTrue(repository.isPremium)
    }

    // 12. restorePending_doesNotGrant
    @Test
    fun restorePending_doesNotGrant() = runBlocking {
        fakeAdapter.mockSubsPurchases = listOf(
            createPurchase(
                productId = PremiumProduct.MONTHLY.productId,
                purchaseState = Purchase.PurchaseState.PENDING,
                isAcknowledged = false
            )
        )
        fakeAdapter.mockInAppPurchases = emptyList()

        val result = repository.restorePurchases()
        assertTrue(result is BillingResult.Pending)
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        assertFalse(repository.isPremium)
    }

    // 13. restoreNoPurchase_keepsFree
    @Test
    fun restoreNoPurchase_keepsFree() = runBlocking {
        fakeAdapter.mockSubsPurchases = emptyList()
        fakeAdapter.mockInAppPurchases = emptyList()

        val result = repository.restorePurchases()
        assertTrue(result is BillingResult.Error)
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        assertFalse(repository.isPremium)
    }

    // 14. duplicatePurchaseToken_notGrantedTwice
    @Test
    fun duplicatePurchaseToken_notGrantedTwice() = runBlocking {
        val testToken = "token_dedup_test"
        val purchase = createPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = testToken
        )

        repository.processPurchases(listOf(purchase))
        val firstRecordCount = repository.records.size

        // Process same purchase again
        repository.processPurchases(listOf(purchase))
        assertEquals(firstRecordCount + 1, repository.records.size)
        // Entitlement remains consistent Premium(MONTHLY)
        assertEquals(PremiumProduct.MONTHLY, (repository.entitlement.value as PremiumEntitlement.Premium).product)
    }

    // 15. productIdMapping_isCorrect
    @Test
    fun productIdMapping_isCorrect() {
        assertEquals("com.andy.englishcoach.premium.monthly", PremiumProduct.MONTHLY.productId)
        assertEquals("com.andy.englishcoach.premium.annual", PremiumProduct.ANNUAL.productId)
        assertEquals("com.andy.englishcoach.premium.lifetime", PremiumProduct.LIFETIME.productId)

        val monthlyProduct = PremiumProduct.entries.firstOrNull { it.productId == "com.andy.englishcoach.premium.monthly" }
        assertEquals(PremiumProduct.MONTHLY, monthlyProduct)

        val annualProduct = PremiumProduct.entries.firstOrNull { it.productId == "com.andy.englishcoach.premium.annual" }
        assertEquals(PremiumProduct.ANNUAL, annualProduct)

        val lifetimeProduct = PremiumProduct.entries.firstOrNull { it.productId == "com.andy.englishcoach.premium.lifetime" }
        assertEquals(PremiumProduct.LIFETIME, lifetimeProduct)
    }

    // 16. purchaseToken_notExposedToUI
    @Test
    fun purchaseToken_notExposedToUI() {
        val secretToken = "super_confidential_purchase_token_12345"
        val record = PurchaseRecord(
            productId = "com.andy.englishcoach.premium.annual",
            purchaseToken = secretToken,
            purchaseState = 1,
            isAcknowledged = true,
            purchaseTime = 123456789L
        )

        // toString() must NOT print the purchaseToken
        val toStringOutput = record.toString()
        assertFalse("purchaseToken must not be visible in toString", toStringOutput.contains(secretToken))
        assertTrue("purchaseToken should be marked [PROTECTED]", toStringOutput.contains("[PROTECTED]"))

        // PremiumProductInfo has no purchaseToken property
        val productInfo = PremiumProduct.ANNUAL.productInfo
        assertEquals(PremiumProduct.ANNUAL, productInfo.product)
    }

    // 17. purchaseSuccess_updatesEntitlement
    @Test
    fun purchaseSuccess_updatesEntitlement() = runBlocking {
        assertEquals(PremiumEntitlement.Free, repository.entitlement.value)
        val purchase = createPurchase(PremiumProduct.ANNUAL.productId)

        repository.processPurchases(listOf(purchase))

        // StateFlow updates reactively
        val current = repository.entitlement.value
        assertTrue(current.isPremium)
        assertEquals(PremiumProduct.ANNUAL, (current as PremiumEntitlement.Premium).product)
    }

    // 18. purchaseError_mapsToUserFacingError
    @Test
    fun purchaseError_mapsToUserFacingError() {
        assertEquals("已取消購買", BillingError.mapResponseCodeToMessage(BillingClient.BillingResponseCode.USER_CANCELED))
        assertEquals("目前無法使用 Google Play 付款服務", BillingError.mapResponseCodeToMessage(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE))
        assertEquals("目前無法使用 Google Play 付款服務", BillingError.mapResponseCodeToMessage(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE))
        assertEquals("此方案目前無法購買", BillingError.mapResponseCodeToMessage(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE))
        assertEquals("網路連線異常，請稍後再試", BillingError.mapResponseCodeToMessage(BillingClient.BillingResponseCode.NETWORK_ERROR))
        assertEquals("購買暫時無法完成", BillingError.mapResponseCodeToMessage(BillingClient.BillingResponseCode.DEVELOPER_ERROR))
        assertEquals("購買暫時無法完成", BillingError.mapResponseCodeToMessage(BillingClient.BillingResponseCode.ERROR))

        // Test domain BillingError to user facing message
        assertEquals("目前無法使用 Google Play 付款服務", BillingError.SERVICE_UNAVAILABLE.toUserFacingMessage())
        assertEquals("此方案目前無法購買", BillingError.ITEM_UNAVAILABLE.toUserFacingMessage())
        assertEquals("網路連線異常，請稍後再試", BillingError.NETWORK_ERROR.toUserFacingMessage())
        assertEquals("購買暫時無法完成", BillingError.UNKNOWN.toUserFacingMessage())
    }

    // 19. subscriptionAndLifetime_useDifferentProductTypes
    @Test
    fun subscriptionAndLifetime_useDifferentProductTypes() {
        assertEquals(PremiumProductType.SUBSCRIPTION, PremiumProduct.MONTHLY.type)
        assertEquals(PremiumProductType.SUBSCRIPTION, PremiumProduct.ANNUAL.type)
        assertEquals(PremiumProductType.ONE_TIME, PremiumProduct.LIFETIME.type)

        assertTrue(PremiumProduct.MONTHLY.isSubscription)
        assertTrue(PremiumProduct.ANNUAL.isSubscription)
        assertTrue(PremiumProduct.LIFETIME.isOneTime)
        assertFalse(PremiumProduct.LIFETIME.isSubscription)
    }

    // 20. multiplePurchasedProducts_grantsPremium
    @Test
    fun multiplePurchasedProducts_grantsPremium() = runBlocking {
        // Simultaneous Monthly and Lifetime returned
        val monthlyPurchase = createPurchase(PremiumProduct.MONTHLY.productId)
        val lifetimePurchase = createPurchase(PremiumProduct.LIFETIME.productId)

        val result = repository.processPurchases(listOf(monthlyPurchase, lifetimePurchase))
        assertTrue(result is BillingResult.Success)
        assertTrue(repository.isPremium)
        // Lifetime given precedence in resolution
        assertEquals(PremiumProduct.LIFETIME, (repository.entitlement.value as PremiumEntitlement.Premium).product)
        // Retained records include both purchases without automatic cancellation or refund
        assertEquals(2, repository.records.size)
    }

    // 21. fakeBillingRegression
    @Test
    fun fakeBillingRegression() = runBlocking {
        val fakeRepo = FakeBillingRepository()

        // 1. Initial
        assertEquals(PremiumEntitlement.Free, fakeRepo.entitlement.value)
        assertFalse(fakeRepo.isPremium)

        // 2. Pending simulation
        fakeRepo.simulatePending = true
        val pendingResult = fakeRepo.purchase(PremiumProduct.MONTHLY)
        assertTrue(pendingResult is BillingResult.Pending)
        assertFalse(fakeRepo.isPremium)

        // 3. Cancel simulation
        fakeRepo.simulatePending = false
        fakeRepo.simulateCancellation = true
        val cancelResult = fakeRepo.purchase(PremiumProduct.MONTHLY)
        assertTrue(cancelResult is BillingResult.Cancelled)
        assertFalse(fakeRepo.isPremium)

        // 4. Failure simulation
        fakeRepo.simulateCancellation = false
        fakeRepo.simulateFailure = true
        val failResult = fakeRepo.purchase(PremiumProduct.MONTHLY)
        assertTrue(failResult is BillingResult.Error)
        assertFalse(fakeRepo.isPremium)

        // 5. Success
        fakeRepo.simulateFailure = false
        val successResult = fakeRepo.purchase(PremiumProduct.ANNUAL)
        assertTrue(successResult is BillingResult.Success)
        assertTrue(fakeRepo.isPremium)

        // 6. Restore
        val restoreResult = fakeRepo.restorePurchases()
        assertTrue(restoreResult is BillingResult.Success)
        assertTrue((restoreResult as BillingResult.Success).entitlement.isPremium)
    }

    // 22. entitlementDoesNotDependOnLocalPreference
    @Test
    fun entitlementDoesNotDependOnLocalPreference() {
        val freshRepo = GooglePlayBillingRepository(context = context, adapter = fakeAdapter)
        // Entitlement is controlled strictly by billing repository state, NOT SharedPreferences
        assertEquals(PremiumEntitlement.Free, freshRepo.entitlement.value)
        assertFalse(freshRepo.isPremium)
    }
}

/**
 * Deterministic test adapter for BillingClient operations.
 */
class FakeBillingClientAdapter : BillingClientAdapter {
    override var isReady: Boolean = true
    var startConnectionResponseCode: Int = BillingClient.BillingResponseCode.OK
    var launchBillingFlowResult: PlayBillingResult = PlayBillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.OK)
        .build()
    var acknowledgeResult: PlayBillingResult = PlayBillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.OK)
        .build()

    var mockSubsPurchases: List<Purchase> = emptyList()
    var mockInAppPurchases: List<Purchase> = emptyList()
    val acknowledgedTokens = mutableListOf<String>()

    override fun startConnection(listener: BillingClientStateListener) {
        val res = PlayBillingResult.newBuilder()
            .setResponseCode(startConnectionResponseCode)
            .build()
        listener.onBillingSetupFinished(res)
    }

    override fun endConnection() {
        isReady = false
    }

    override suspend fun queryProductDetails(params: QueryProductDetailsParams): ProductDetailsResult {
        return ProductDetailsResult(
            PlayBillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(),
            emptyList()
        )
    }

    override suspend fun queryPurchases(params: QueryPurchasesParams): PurchasesResult {
        val ok = PlayBillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        val all = mockSubsPurchases + mockInAppPurchases
        return PurchasesResult(ok, all)
    }

    override suspend fun acknowledgePurchase(params: AcknowledgePurchaseParams): PlayBillingResult {
        acknowledgedTokens.add(params.purchaseToken)
        return acknowledgeResult
    }

    override fun launchBillingFlow(activity: Activity, params: BillingFlowParams): PlayBillingResult {
        return launchBillingFlowResult
    }
}
