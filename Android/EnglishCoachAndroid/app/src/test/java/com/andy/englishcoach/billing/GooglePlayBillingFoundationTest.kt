package com.andy.englishcoach.billing

import com.android.billingclient.api.BillingClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic unit test suite verifying STEP 16-B Google Play BillingClient foundation integration:
 * - BillingClient connection state machine
 * - Official product IDs and structure
 * - ProductDetails mapping for SUBS and INAPP
 * - 7-day trial offer token extraction
 * - Safe handling of products without trials
 * - Localized pricing preservation
 * - Reactive catalog state (Loading, Available, Empty, Error)
 * - Billing response code error mapping
 * - Entitlement isolation (catalog updates never modify user entitlement)
 */
class GooglePlayBillingFoundationTest {

    // 1. billingRepository_initialState
    @Test
    fun billingRepository_initialState() {
        val fakeRepo = FakeBillingRepository()
        assertEquals(BillingConnectionState.Connected, fakeRepo.connectionState.value)
        assertTrue(fakeRepo.catalogState.value is BillingProductCatalog.Available)
        assertEquals(PremiumEntitlement.Free, fakeRepo.entitlement.value)

        val defaultRepo = DefaultBillingRepository()
        assertEquals(BillingConnectionState.Connected, defaultRepo.connectionState.value)
        assertTrue(defaultRepo.catalogState.value is BillingProductCatalog.Available)
        assertEquals(PremiumEntitlement.Free, defaultRepo.entitlement.value)
    }

    // 2. billingConnection_disconnected
    @Test
    fun billingConnection_disconnected() {
        val repo = FakeBillingRepository()
        repo.setConnectionStateForTesting(BillingConnectionState.Disconnected)
        assertEquals(BillingConnectionState.Disconnected, repo.connectionState.value)
        assertTrue(repo.connectionState.value is BillingConnectionState.Disconnected)
    }

    // 3. billingConnection_connecting
    @Test
    fun billingConnection_connecting() {
        val repo = FakeBillingRepository()
        repo.setConnectionStateForTesting(BillingConnectionState.Connecting)
        assertEquals(BillingConnectionState.Connecting, repo.connectionState.value)
        assertTrue(repo.connectionState.value is BillingConnectionState.Connecting)
    }

    // 4. billingProductIds_matchSpecification
    @Test
    fun billingProductIds_matchSpecification() {
        assertEquals("com.andy.englishcoach.premium.monthly", PremiumProduct.MONTHLY.productId)
        assertEquals("com.andy.englishcoach.premium.annual", PremiumProduct.ANNUAL.productId)
        assertEquals("com.andy.englishcoach.premium.lifetime", PremiumProduct.LIFETIME.productId)

        // Verify enum size and types
        assertEquals(3, PremiumProduct.entries.size)
        assertEquals(PremiumProductType.SUBSCRIPTION, PremiumProduct.MONTHLY.type)
        assertEquals(PremiumProductType.SUBSCRIPTION, PremiumProduct.ANNUAL.type)
        assertEquals(PremiumProductType.ONE_TIME, PremiumProduct.LIFETIME.type)
    }

    // 5. subscriptionProduct_mappingMonthly
    @Test
    fun subscriptionProduct_mappingMonthly() {
        val monthlyData = GooglePlayProductData(
            productId = "com.andy.englishcoach.premium.monthly",
            productType = BillingClient.ProductType.SUBS,
            title = "Monthly Plan",
            subscriptionOffers = listOf(
                GooglePlaySubscriptionOfferData(
                    basePlanId = "monthly-plan",
                    offerId = null,
                    offerToken = "token_monthly_test_123",
                    pricingPhases = listOf(
                        GooglePlayPricingPhaseData(
                            formattedPrice = "NT$150",
                            priceCurrencyCode = "TWD",
                            priceAmountMicros = 150_000_000L,
                            billingPeriod = "P1M",
                            recurrenceMode = 1
                        )
                    )
                )
            )
        )

        val productInfo = GooglePlayProductDetailsMapper.mapProductData(monthlyData)
        assertNotNull(productInfo)
        productInfo?.let {
            assertEquals(PremiumProduct.MONTHLY, it.product)
            assertEquals("com.andy.englishcoach.premium.monthly", it.productId)
            assertEquals(PremiumProductType.SUBSCRIPTION, it.type)
            assertEquals("NT$150", it.displayPrice)
            assertEquals("TWD", it.currencyCode)
            assertEquals("token_monthly_test_123", it.offerToken)
            assertFalse(it.hasFreeTrial)
            assertEquals(0, it.freeTrialDays)
        }
    }

    // 6. subscriptionProduct_mappingAnnual
    @Test
    fun subscriptionProduct_mappingAnnual() {
        val annualData = GooglePlayProductData(
            productId = "com.andy.englishcoach.premium.annual",
            productType = BillingClient.ProductType.SUBS,
            title = "Annual Plan",
            subscriptionOffers = listOf(
                GooglePlaySubscriptionOfferData(
                    basePlanId = "annual-plan",
                    offerId = null,
                    offerToken = "token_annual_test_456",
                    pricingPhases = listOf(
                        GooglePlayPricingPhaseData(
                            formattedPrice = "NT$990",
                            priceCurrencyCode = "TWD",
                            priceAmountMicros = 990_000_000L,
                            billingPeriod = "P1Y",
                            recurrenceMode = 1
                        )
                    )
                )
            )
        )

        val productInfo = GooglePlayProductDetailsMapper.mapProductData(annualData)
        assertNotNull(productInfo)
        productInfo?.let {
            assertEquals(PremiumProduct.ANNUAL, it.product)
            assertEquals("com.andy.englishcoach.premium.annual", it.productId)
            assertEquals(PremiumProductType.SUBSCRIPTION, it.type)
            assertEquals("NT$990", it.displayPrice)
            assertEquals("TWD", it.currencyCode)
            assertEquals("token_annual_test_456", it.offerToken)
        }
    }

    // 7. annualTrialOfferToken_extracted
    @Test
    fun annualTrialOfferToken_extracted() {
        val annualWithTrialData = GooglePlayProductData(
            productId = "com.andy.englishcoach.premium.annual",
            productType = BillingClient.ProductType.SUBS,
            title = "Annual Plan with Free Trial",
            subscriptionOffers = listOf(
                GooglePlaySubscriptionOfferData(
                    basePlanId = "annual-plan",
                    offerId = "free-trial-7d",
                    offerToken = "token_annual_trial_7d_verified",
                    pricingPhases = listOf(
                        GooglePlayPricingPhaseData(
                            formattedPrice = "Free",
                            priceCurrencyCode = "TWD",
                            priceAmountMicros = 0L,
                            billingPeriod = "P7D",
                            recurrenceMode = 2
                        ),
                        GooglePlayPricingPhaseData(
                            formattedPrice = "NT$990",
                            priceCurrencyCode = "TWD",
                            priceAmountMicros = 990_000_000L,
                            billingPeriod = "P1Y",
                            recurrenceMode = 1
                        )
                    )
                )
            )
        )

        val productInfo = GooglePlayProductDetailsMapper.mapProductData(annualWithTrialData)
        assertNotNull(productInfo)
        productInfo?.let {
            assertEquals(PremiumProduct.ANNUAL, it.product)
            assertTrue(it.hasFreeTrial)
            assertEquals(7, it.freeTrialDays)
            assertEquals("token_annual_trial_7d_verified", it.offerToken)
            assertEquals("NT$990", it.displayPrice)
            assertEquals("TWD", it.currencyCode)
        }
    }

    // 8. annualNoTrialOffer_handledSafely
    @Test
    fun annualNoTrialOffer_handledSafely() {
        val annualNoTrialData = GooglePlayProductData(
            productId = "com.andy.englishcoach.premium.annual",
            productType = BillingClient.ProductType.SUBS,
            title = "Annual Plan Without Trial",
            subscriptionOffers = listOf(
                GooglePlaySubscriptionOfferData(
                    basePlanId = "annual-plan",
                    offerId = null,
                    offerToken = "token_annual_notrial",
                    pricingPhases = listOf(
                        GooglePlayPricingPhaseData(
                            formattedPrice = "NT$990",
                            priceCurrencyCode = "TWD",
                            priceAmountMicros = 990_000_000L,
                            billingPeriod = "P1Y",
                            recurrenceMode = 1
                        )
                    )
                )
            )
        )

        val productInfo = GooglePlayProductDetailsMapper.mapProductData(annualNoTrialData)
        assertNotNull(productInfo)
        productInfo?.let {
            assertEquals(PremiumProduct.ANNUAL, it.product)
            assertFalse(it.hasFreeTrial)
            assertEquals(0, it.freeTrialDays)
            assertEquals("token_annual_notrial", it.offerToken)
            assertEquals("NT$990", it.displayPrice)
        }
    }

    // 9. lifetimeProduct_mapping
    @Test
    fun lifetimeProduct_mapping() {
        val lifetimeData = GooglePlayProductData(
            productId = "com.andy.englishcoach.premium.lifetime",
            productType = BillingClient.ProductType.INAPP,
            title = "Lifetime Plan",
            oneTimeOffer = GooglePlayOneTimeOfferData(
                formattedPrice = "NT$1,990",
                priceCurrencyCode = "TWD",
                priceAmountMicros = 1_990_000_000L
            )
        )

        val productInfo = GooglePlayProductDetailsMapper.mapProductData(lifetimeData)
        assertNotNull(productInfo)
        productInfo?.let {
            assertEquals(PremiumProduct.LIFETIME, it.product)
            assertEquals("com.andy.englishcoach.premium.lifetime", it.productId)
            assertEquals(PremiumProductType.ONE_TIME, it.type)
            assertEquals("NT$1,990", it.displayPrice)
            assertEquals("TWD", it.currencyCode)
            assertFalse(it.hasFreeTrial)
            assertEquals(0, it.freeTrialDays)
            assertNull(it.offerToken)
        }
    }

    // 10. localizedPrice_preserved
    @Test
    fun localizedPrice_preserved() {
        // USD Test
        val usdProduct = GooglePlayProductData(
            productId = "com.andy.englishcoach.premium.annual",
            productType = BillingClient.ProductType.SUBS,
            subscriptionOffers = listOf(
                GooglePlaySubscriptionOfferData(
                    offerToken = "token_usd",
                    pricingPhases = listOf(
                        GooglePlayPricingPhaseData(
                            formattedPrice = "US$29.99",
                            priceCurrencyCode = "USD",
                            priceAmountMicros = 29_990_000L,
                            billingPeriod = "P1Y"
                        )
                    )
                )
            )
        )
        val usdMapped = GooglePlayProductDetailsMapper.mapProductData(usdProduct)
        assertEquals("US$29.99", usdMapped?.displayPrice)
        assertEquals("USD", usdMapped?.currencyCode)

        // JPY Test
        val jpyProduct = GooglePlayProductData(
            productId = "com.andy.englishcoach.premium.lifetime",
            productType = BillingClient.ProductType.INAPP,
            oneTimeOffer = GooglePlayOneTimeOfferData(
                formattedPrice = "¥4,000",
                priceCurrencyCode = "JPY",
                priceAmountMicros = 4_000_000_000L
            )
        )
        val jpyMapped = GooglePlayProductDetailsMapper.mapProductData(jpyProduct)
        assertEquals("¥4,000", jpyMapped?.displayPrice)
        assertEquals("JPY", jpyMapped?.currencyCode)

        // EUR Test
        val eurProduct = GooglePlayProductData(
            productId = "com.andy.englishcoach.premium.monthly",
            productType = BillingClient.ProductType.SUBS,
            subscriptionOffers = listOf(
                GooglePlaySubscriptionOfferData(
                    offerToken = "token_eur",
                    pricingPhases = listOf(
                        GooglePlayPricingPhaseData(
                            formattedPrice = "4,99 €",
                            priceCurrencyCode = "EUR",
                            priceAmountMicros = 4_990_000L,
                            billingPeriod = "P1M"
                        )
                    )
                )
            )
        )
        val eurMapped = GooglePlayProductDetailsMapper.mapProductData(eurProduct)
        assertEquals("4,99 €", eurMapped?.displayPrice)
        assertEquals("EUR", eurMapped?.currencyCode)
    }

    // 11. productCatalog_loading
    @Test
    fun productCatalog_loading() {
        val catalog: BillingProductCatalog = BillingProductCatalog.Loading
        assertTrue(catalog is BillingProductCatalog.Loading)
    }

    // 12. productCatalog_available
    @Test
    fun productCatalog_available() {
        val products = listOf(
            PremiumProduct.MONTHLY.productInfo,
            PremiumProduct.ANNUAL.productInfo
        )
        val catalog: BillingProductCatalog = BillingProductCatalog.Available(products)
        assertTrue(catalog is BillingProductCatalog.Available)
        val available = catalog as BillingProductCatalog.Available
        assertEquals(2, available.products.size)
        assertEquals(PremiumProduct.MONTHLY, available.products[0].product)
        assertEquals(PremiumProduct.ANNUAL, available.products[1].product)
    }

    // 13. productCatalog_error
    @Test
    fun productCatalog_error() {
        val catalog: BillingProductCatalog = BillingProductCatalog.Error(
            error = BillingError.NETWORK_ERROR,
            message = "Unable to reach Google Play servers"
        )
        assertTrue(catalog is BillingProductCatalog.Error)
        val errorCatalog = catalog as BillingProductCatalog.Error
        assertEquals(BillingError.NETWORK_ERROR, errorCatalog.error)
        assertEquals("Unable to reach Google Play servers", errorCatalog.message)
    }

    // 14. itemUnavailable_handled
    @Test
    fun itemUnavailable_handled() {
        val mapped = BillingError.fromBillingResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
        assertEquals(BillingError.ITEM_UNAVAILABLE, mapped)
    }

    // 15. serviceUnavailable_handled
    @Test
    fun serviceUnavailable_handled() {
        val serviceUnavailable = BillingError.fromBillingResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
        assertEquals(BillingError.SERVICE_UNAVAILABLE, serviceUnavailable)

        val serviceDisconnected = BillingError.fromBillingResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
        assertEquals(BillingError.SERVICE_UNAVAILABLE, serviceDisconnected)

        val billingUnavailable = BillingError.fromBillingResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
        assertEquals(BillingError.BILLING_UNAVAILABLE, billingUnavailable)

        val developerError = BillingError.fromBillingResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
        assertEquals(BillingError.DEVELOPER_ERROR, developerError)

        val featureNotSupported = BillingError.fromBillingResponseCode(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED)
        assertEquals(BillingError.FEATURE_NOT_SUPPORTED, featureNotSupported)

        val networkError = BillingError.fromBillingResponseCode(BillingClient.BillingResponseCode.NETWORK_ERROR)
        assertEquals(BillingError.NETWORK_ERROR, networkError)

        val unknownCode = BillingError.fromBillingResponseCode(999)
        assertEquals(BillingError.UNKNOWN, unknownCode)
    }

    // 16. fakeBillingTests_stillPass
    @Test
    fun fakeBillingTests_stillPass() = runBlocking {
        val repo = FakeBillingRepository()

        // 1. Success purchase
        val successResult = repo.purchase(PremiumProduct.ANNUAL)
        assertTrue(successResult is BillingResult.Success)
        val entitlement = repo.entitlement.value
        assertTrue(entitlement is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.ANNUAL, (entitlement as PremiumEntitlement.Premium).product)

        // 2. Cancellation simulation
        repo.simulateCancellation = true
        val cancelResult = repo.purchase(PremiumProduct.MONTHLY)
        assertTrue(cancelResult is BillingResult.Cancelled)

        // 3. Failure simulation
        repo.simulateCancellation = false
        repo.simulateFailure = true
        repo.failureErrorMessage = "模擬連線錯誤"
        val failResult = repo.purchase(PremiumProduct.MONTHLY)
        assertTrue(failResult is BillingResult.Error)
        assertEquals("模擬連線錯誤", (failResult as BillingResult.Error).message)

        // 4. Restore purchases
        repo.simulateFailure = false
        val restoreResult = repo.restorePurchases()
        val restoredEntitlement = (restoreResult as BillingResult.Success).entitlement
        assertTrue(restoredEntitlement.isPremium)
        assertEquals(
            (entitlement as PremiumEntitlement.Premium).product,
            (restoredEntitlement as PremiumEntitlement.Premium).product
        )

        // 5. Direct entitlement setting
        repo.setEntitlementForTesting(PremiumEntitlement.Free)
        assertEquals(PremiumEntitlement.Free, repo.entitlement.value)
    }

    // 17. entitlementDoesNotChange_whenProductCatalogLoads
    @Test
    fun entitlementDoesNotChange_whenProductCatalogLoads() {
        val repo = FakeBillingRepository()

        // Invariant: User entitlement is Free initially
        assertEquals(PremiumEntitlement.Free, repo.entitlement.value)

        // Transition catalog to Loading
        repo.setCatalogStateForTesting(BillingProductCatalog.Loading)
        assertEquals(PremiumEntitlement.Free, repo.entitlement.value)

        // Transition catalog to Available
        repo.setCatalogStateForTesting(BillingProductCatalog.Available(repo.getProducts()))
        assertEquals(PremiumEntitlement.Free, repo.entitlement.value)

        // Transition catalog to Empty
        repo.setCatalogStateForTesting(BillingProductCatalog.Empty)
        assertEquals(PremiumEntitlement.Free, repo.entitlement.value)

        // Transition catalog to Error
        repo.setCatalogStateForTesting(
            BillingProductCatalog.Error(BillingError.SERVICE_UNAVAILABLE, "Store error")
        )
        assertEquals(PremiumEntitlement.Free, repo.entitlement.value)

        // Verify for user with Premium entitlement: catalog loading does not downgrade or reset them
        repo.setEntitlementForTesting(PremiumEntitlement.Premium(PremiumProduct.LIFETIME))
        assertEquals(
            PremiumEntitlement.Premium(PremiumProduct.LIFETIME),
            repo.entitlement.value
        )

        repo.setCatalogStateForTesting(BillingProductCatalog.Loading)
        assertEquals(
            PremiumEntitlement.Premium(PremiumProduct.LIFETIME),
            repo.entitlement.value
        )

        repo.setCatalogStateForTesting(
            BillingProductCatalog.Error(BillingError.NETWORK_ERROR, "Network fail")
        )
        assertEquals(
            PremiumEntitlement.Premium(PremiumProduct.LIFETIME),
            repo.entitlement.value
        )
    }
}
