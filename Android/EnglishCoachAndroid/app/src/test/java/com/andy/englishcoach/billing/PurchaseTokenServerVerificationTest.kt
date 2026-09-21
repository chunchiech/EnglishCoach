package com.andy.englishcoach.billing.verification

import com.andy.englishcoach.billing.PremiumProduct
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Deterministic test suite verifying STEP 16-D:
 * Purchase Token Server Verification Architecture.
 *
 * Covers all 20 required scenarios defined in Section XXIII:
 * 1. verifiedSubscription_grantsPremium
 * 2. verifiedLifetime_grantsPremium
 * 3. pending_doesNotGrantNewPremium
 * 4. expired_removesPremium
 * 5. revoked_removesPremium
 * 6. invalid_doesNotGrantPremium
 * 7. temporaryError_doesNotImmediatelyRevokeExistingPremium
 * 8. purchaseTokenNeverExposedToUi
 * 9. duplicateVerification_isIdempotent
 * 10. monthlyProductVerification
 * 11. annualProductVerification
 * 12. lifetimeProductVerification
 * 13. unknownProductRejected
 * 14. emptyTokenRejected
 * 15. invalidProductTypeRejected
 * 16. offlineExistingPremium_preservesCachedState
 * 17. newPurchaseOffline_remainsPending
 * 18. verificationResponseMappedToDomain
 * 19. HTTPErrorMappedToTemporaryError
 * 20. fakeVerificationRegression
 */
class PurchaseTokenServerVerificationTest {

    private lateinit var mockApiClient: TestVerificationApiClient
    private lateinit var repository: DefaultPurchaseVerificationRepository

    @Before
    fun setUp() {
        mockApiClient = TestVerificationApiClient()
        repository = DefaultPurchaseVerificationRepository(mockApiClient)
    }

    // 1. verifiedSubscription_grantsPremium
    @Test
    fun verifiedSubscription_grantsPremium() = runBlocking {
        assertEquals(VerifiedEntitlement.FREE, repository.verifiedEntitlement.value)
        assertFalse(repository.isPremium)

        mockApiClient.mockResponse = VerifyPurchaseResponse(
            status = VerificationStatus.VERIFIED,
            productId = PremiumProduct.MONTHLY.productId,
            productType = "SUBS",
            entitlement = VerifiedEntitlement.PREMIUM,
            expiryTimeMillis = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        )

        val result = repository.verifyPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "valid_token_monthly_123",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.Success)
        assertEquals(VerifiedEntitlement.PREMIUM, (result as VerificationResult.Success).entitlement)
        assertEquals(PremiumProduct.MONTHLY, result.product)
        assertEquals(VerifiedEntitlement.PREMIUM, repository.verifiedEntitlement.value)
        assertTrue(repository.isPremium)
    }

    // 2. verifiedLifetime_grantsPremium
    @Test
    fun verifiedLifetime_grantsPremium() = runBlocking {
        assertEquals(VerifiedEntitlement.FREE, repository.verifiedEntitlement.value)

        mockApiClient.mockResponse = VerifyPurchaseResponse(
            status = VerificationStatus.VERIFIED,
            productId = PremiumProduct.LIFETIME.productId,
            productType = "INAPP",
            entitlement = VerifiedEntitlement.PREMIUM,
            expiryTimeMillis = null
        )

        val result = repository.verifyPurchase(
            productId = PremiumProduct.LIFETIME.productId,
            purchaseToken = "valid_token_lifetime_456",
            productType = "INAPP"
        )

        assertTrue(result is VerificationResult.Success)
        assertEquals(PremiumProduct.LIFETIME, (result as VerificationResult.Success).product)
        assertEquals(VerifiedEntitlement.PREMIUM, repository.verifiedEntitlement.value)
        assertTrue(repository.isPremium)
    }

    // 3. pending_doesNotGrantNewPremium
    @Test
    fun pending_doesNotGrantNewPremium() = runBlocking {
        assertEquals(VerifiedEntitlement.FREE, repository.verifiedEntitlement.value)

        mockApiClient.mockResponse = VerifyPurchaseResponse(
            status = VerificationStatus.PENDING,
            productId = PremiumProduct.ANNUAL.productId,
            productType = "SUBS",
            entitlement = VerifiedEntitlement.FREE,
            message = "付款處理中，驗證完成後將自動開通"
        )

        val result = repository.verifyPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseToken = "pending_token_789",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.Pending)
        // Must NOT grant Premium
        assertEquals(VerifiedEntitlement.FREE, repository.verifiedEntitlement.value)
        assertFalse(repository.isPremium)
    }

    // 4. expired_removesPremium
    @Test
    fun expired_removesPremium() = runBlocking {
        // User starts with active Premium
        repository = DefaultPurchaseVerificationRepository(
            apiClient = mockApiClient,
            initialEntitlement = VerifiedEntitlement.PREMIUM
        )
        assertTrue(repository.isPremium)

        mockApiClient.mockResponse = VerifyPurchaseResponse(
            status = VerificationStatus.EXPIRED,
            productId = PremiumProduct.ANNUAL.productId,
            productType = "SUBS",
            entitlement = VerifiedEntitlement.FREE,
            message = "訂閱方案已過期"
        )

        val result = repository.verifyPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseToken = "expired_token_000",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.Expired)
        assertEquals(VerifiedEntitlement.FREE, repository.verifiedEntitlement.value)
        assertFalse(repository.isPremium)
    }

    // 5. revoked_removesPremium
    @Test
    fun revoked_removesPremium() = runBlocking {
        repository = DefaultPurchaseVerificationRepository(
            apiClient = mockApiClient,
            initialEntitlement = VerifiedEntitlement.PREMIUM
        )
        assertTrue(repository.isPremium)

        mockApiClient.mockResponse = VerifyPurchaseResponse(
            status = VerificationStatus.REVOKED,
            productId = PremiumProduct.MONTHLY.productId,
            productType = "SUBS",
            entitlement = VerifiedEntitlement.FREE,
            message = "訂閱已被撤銷或退款"
        )

        val result = repository.verifyPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "revoked_token_111",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.Revoked)
        assertEquals(VerifiedEntitlement.FREE, repository.verifiedEntitlement.value)
        assertFalse(repository.isPremium)
    }

    // 6. invalid_doesNotGrantPremium
    @Test
    fun invalid_doesNotGrantPremium() = runBlocking {
        assertEquals(VerifiedEntitlement.FREE, repository.verifiedEntitlement.value)

        mockApiClient.mockResponse = VerifyPurchaseResponse(
            status = VerificationStatus.INVALID,
            productId = PremiumProduct.MONTHLY.productId,
            productType = "SUBS",
            entitlement = VerifiedEntitlement.FREE,
            message = "無效的購買憑證"
        )

        val result = repository.verifyPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "invalid_token_999",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.Invalid)
        assertEquals(VerifiedEntitlement.FREE, repository.verifiedEntitlement.value)
        assertFalse(repository.isPremium)
    }

    // 7. temporaryError_doesNotImmediatelyRevokeExistingPremium
    @Test
    fun temporaryError_doesNotImmediatelyRevokeExistingPremium() = runBlocking {
        // Critical Section XV policy: User already has verified Premium
        repository = DefaultPurchaseVerificationRepository(
            apiClient = mockApiClient,
            initialEntitlement = VerifiedEntitlement.PREMIUM
        )
        assertTrue(repository.isPremium)

        // Server responds with temporary 5xx / connection failure
        mockApiClient.mockResponse = VerifyPurchaseResponse(
            status = VerificationStatus.TEMPORARY_ERROR,
            productId = PremiumProduct.ANNUAL.productId,
            productType = "SUBS",
            entitlement = VerifiedEntitlement.PREMIUM,
            message = "伺服器維護中"
        )

        val result = repository.verifyPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseToken = "valid_cached_token",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.TemporaryError)
        // Must NOT revoke existing Premium on temporary server error
        assertEquals(VerifiedEntitlement.PREMIUM, repository.verifiedEntitlement.value)
        assertEquals(VerifiedEntitlement.PREMIUM, repository.getCachedEntitlement())
        assertTrue(repository.isPremium)
    }

    // 8. purchaseTokenNeverExposedToUi
    @Test
    fun purchaseTokenNeverExposedToUi() {
        val secretToken = "super_confidential_purchase_token_sec_888"
        val request = VerifyPurchaseRequest(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = secretToken,
            productType = "SUBS"
        )

        val requestString = request.toString()
        assertFalse("Request string representation must not contain raw token", requestString.contains(secretToken))
        assertTrue("Request string representation should mask token with [PROTECTED]", requestString.contains("[PROTECTED]"))

        val response = VerifyPurchaseResponse(
            status = VerificationStatus.VERIFIED,
            productId = PremiumProduct.MONTHLY.productId,
            productType = "SUBS",
            entitlement = VerifiedEntitlement.PREMIUM
        )
        assertFalse(response.toString().contains(secretToken))
    }

    // 9. duplicateVerification_isIdempotent
    @Test
    fun duplicateVerification_isIdempotent() = runBlocking {
        val testToken = "idempotent_token_xyz"
        mockApiClient.mockResponse = VerifyPurchaseResponse(
            status = VerificationStatus.VERIFIED,
            productId = PremiumProduct.MONTHLY.productId,
            productType = "SUBS",
            entitlement = VerifiedEntitlement.PREMIUM
        )

        val result1 = repository.verifyPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = testToken,
            productType = "SUBS"
        )
        assertEquals(1, mockApiClient.callCount)
        assertTrue(result1 is VerificationResult.Success)

        // Second verification with identical token must return cached result without redundant network call
        val result2 = repository.verifyPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = testToken,
            productType = "SUBS"
        )
        assertEquals(1, mockApiClient.callCount) // Idempotent: call count not incremented
        assertEquals(result1, result2)
        assertTrue(repository.isPremium)
    }

    // 10. monthlyProductVerification
    @Test
    fun monthlyProductVerification() {
        val request = VerifyPurchaseRequest(
            productId = "com.andy.englishcoach.premium.monthly",
            purchaseToken = "token_monthly",
            productType = "SUBS"
        )
        val validation = PurchaseVerificationValidator.validate(request)
        assertTrue(validation is PurchaseVerificationValidator.ValidationResult.Valid)
    }

    // 11. annualProductVerification
    @Test
    fun annualProductVerification() {
        val request = VerifyPurchaseRequest(
            productId = "com.andy.englishcoach.premium.annual",
            purchaseToken = "token_annual",
            productType = "SUBS"
        )
        val validation = PurchaseVerificationValidator.validate(request)
        assertTrue(validation is PurchaseVerificationValidator.ValidationResult.Valid)
    }

    // 12. lifetimeProductVerification
    @Test
    fun lifetimeProductVerification() {
        val request = VerifyPurchaseRequest(
            productId = "com.andy.englishcoach.premium.lifetime",
            purchaseToken = "token_lifetime",
            productType = "INAPP"
        )
        val validation = PurchaseVerificationValidator.validate(request)
        assertTrue(validation is PurchaseVerificationValidator.ValidationResult.Valid)
    }

    // 13. unknownProductRejected
    @Test
    fun unknownProductRejected() = runBlocking {
        val request = VerifyPurchaseRequest(
            productId = "com.fake.hacker.product",
            purchaseToken = "token_hack",
            productType = "SUBS"
        )
        val validation = PurchaseVerificationValidator.validate(request)
        assertTrue(validation is PurchaseVerificationValidator.ValidationResult.Invalid)

        val result = repository.verifyPurchase(
            productId = "com.fake.hacker.product",
            purchaseToken = "token_hack",
            productType = "SUBS"
        )
        assertTrue(result is VerificationResult.Invalid)
        assertEquals(0, mockApiClient.callCount) // Rejected before dispatch
    }

    // 14. emptyTokenRejected
    @Test
    fun emptyTokenRejected() = runBlocking {
        val request = VerifyPurchaseRequest(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "   ",
            productType = "SUBS"
        )
        val validation = PurchaseVerificationValidator.validate(request)
        assertTrue(validation is PurchaseVerificationValidator.ValidationResult.Invalid)

        val result = repository.verifyPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "",
            productType = "SUBS"
        )
        assertTrue(result is VerificationResult.Invalid)
    }

    // 15. invalidProductTypeRejected
    @Test
    fun invalidProductTypeRejected() {
        // Unknown productType
        val req1 = VerifyPurchaseRequest(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "token_1",
            productType = "CONSUMABLE"
        )
        val val1 = PurchaseVerificationValidator.validate(req1)
        assertTrue(val1 is PurchaseVerificationValidator.ValidationResult.Invalid)

        // Type mismatch: Subscription configured as INAPP
        val req2 = VerifyPurchaseRequest(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "token_2",
            productType = "INAPP"
        )
        val val2 = PurchaseVerificationValidator.validate(req2)
        assertTrue(val2 is PurchaseVerificationValidator.ValidationResult.Invalid)

        // Type mismatch: Lifetime configured as SUBS
        val req3 = VerifyPurchaseRequest(
            productId = PremiumProduct.LIFETIME.productId,
            purchaseToken = "token_3",
            productType = "SUBS"
        )
        val val3 = PurchaseVerificationValidator.validate(req3)
        assertTrue(val3 is PurchaseVerificationValidator.ValidationResult.Invalid)
    }

    // 16. offlineExistingPremium_preservesCachedState
    @Test
    fun offlineExistingPremium_preservesCachedState() = runBlocking {
        val fakeRepo = FakePurchaseVerificationRepository(initialEntitlement = VerifiedEntitlement.PREMIUM)
        assertTrue(fakeRepo.isPremium)

        fakeRepo.isOffline = true
        val result = fakeRepo.verifyPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseToken = "cached_token_offline",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.TemporaryError)
        // Existing cached entitlement remains PREMIUM
        assertTrue(fakeRepo.isPremium)
        assertEquals(VerifiedEntitlement.PREMIUM, fakeRepo.getCachedEntitlement())
    }

    // 17. newPurchaseOffline_remainsPending
    @Test
    fun newPurchaseOffline_remainsPending() = runBlocking {
        val fakeRepo = FakePurchaseVerificationRepository(initialEntitlement = VerifiedEntitlement.FREE)
        assertFalse(fakeRepo.isPremium)

        fakeRepo.isOffline = true
        val result = fakeRepo.verifyPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseToken = "new_token_offline",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.TemporaryError)
        // Fresh purchase offline must NOT grant verified Premium
        assertFalse(fakeRepo.isPremium)
        assertEquals(VerifiedEntitlement.FREE, fakeRepo.verifiedEntitlement.value)
    }

    // 18. verificationResponseMappedToDomain
    @Test
    fun verificationResponseMappedToDomain() {
        val respVerified = VerifyPurchaseResponse(VerificationStatus.VERIFIED, PremiumProduct.MONTHLY.productId, "SUBS", VerifiedEntitlement.PREMIUM)
        assertEquals(VerificationStatus.VERIFIED, respVerified.status)
        assertTrue(respVerified.status.isVerified)

        val respPending = VerifyPurchaseResponse(VerificationStatus.PENDING, PremiumProduct.MONTHLY.productId, "SUBS", VerifiedEntitlement.FREE)
        assertEquals(VerificationStatus.PENDING, respPending.status)
        assertFalse(respPending.status.isVerified)

        val respExpired = VerifyPurchaseResponse(VerificationStatus.EXPIRED, PremiumProduct.MONTHLY.productId, "SUBS", VerifiedEntitlement.FREE)
        assertEquals(VerificationStatus.EXPIRED, respExpired.status)

        val respRevoked = VerifyPurchaseResponse(VerificationStatus.REVOKED, PremiumProduct.MONTHLY.productId, "SUBS", VerifiedEntitlement.FREE)
        assertEquals(VerificationStatus.REVOKED, respRevoked.status)

        val respInvalid = VerifyPurchaseResponse(VerificationStatus.INVALID, PremiumProduct.MONTHLY.productId, "SUBS", VerifiedEntitlement.FREE)
        assertEquals(VerificationStatus.INVALID, respInvalid.status)
    }

    // 19. HTTPErrorMappedToTemporaryError
    @Test
    fun HTTPErrorMappedToTemporaryError() = runBlocking {
        mockApiClient.simulateException = IOException("HTTP 503 Service Unavailable: connection timed out")

        val result = repository.verifyPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "tok_http_error",
            productType = "SUBS"
        )

        assertTrue(result is VerificationResult.TemporaryError)
        assertEquals("HTTP 503 Service Unavailable: connection timed out", (result as VerificationResult.TemporaryError).message)
    }

    // 20. fakeVerificationRegression
    @Test
    fun fakeVerificationRegression() = runBlocking {
        val fakeRepo = FakePurchaseVerificationRepository()

        // 1. Initial FREE
        assertEquals(VerifiedEntitlement.FREE, fakeRepo.verifiedEntitlement.value)
        assertFalse(fakeRepo.isPremium)

        // 2. Verified -> PREMIUM
        fakeRepo.mockStatus = VerificationStatus.VERIFIED
        val resSuccess = fakeRepo.verifyPurchase(PremiumProduct.ANNUAL.productId, "tok_1", "SUBS")
        assertTrue(resSuccess is VerificationResult.Success)
        assertTrue(fakeRepo.isPremium)

        // 3. Pending
        fakeRepo.mockStatus = VerificationStatus.PENDING
        fakeRepo.setEntitlementForTesting(VerifiedEntitlement.FREE)
        val resPending = fakeRepo.verifyPurchase(PremiumProduct.MONTHLY.productId, "tok_2", "SUBS")
        assertTrue(resPending is VerificationResult.Pending)
        assertFalse(fakeRepo.isPremium)

        // 4. Expired -> FREE
        fakeRepo.setEntitlementForTesting(VerifiedEntitlement.PREMIUM)
        fakeRepo.mockStatus = VerificationStatus.EXPIRED
        val resExpired = fakeRepo.verifyPurchase(PremiumProduct.MONTHLY.productId, "tok_3", "SUBS")
        assertTrue(resExpired is VerificationResult.Expired)
        assertFalse(fakeRepo.isPremium)

        // 5. Revoked -> FREE
        fakeRepo.setEntitlementForTesting(VerifiedEntitlement.PREMIUM)
        fakeRepo.mockStatus = VerificationStatus.REVOKED
        val resRevoked = fakeRepo.verifyPurchase(PremiumProduct.MONTHLY.productId, "tok_4", "SUBS")
        assertTrue(resRevoked is VerificationResult.Revoked)
        assertFalse(fakeRepo.isPremium)

        // 6. Invalid
        fakeRepo.mockStatus = VerificationStatus.INVALID
        val resInvalid = fakeRepo.verifyPurchase(PremiumProduct.MONTHLY.productId, "tok_5", "SUBS")
        assertTrue(resInvalid is VerificationResult.Invalid)

        // 7. Temporary error
        fakeRepo.mockStatus = VerificationStatus.TEMPORARY_ERROR
        val resTemp = fakeRepo.verifyPurchase(PremiumProduct.MONTHLY.productId, "tok_6", "SUBS")
        assertTrue(resTemp is VerificationResult.TemporaryError)
    }
}

/**
 * Test mock for [VerificationApiClient].
 */
class TestVerificationApiClient : VerificationApiClient {
    var mockResponse: VerifyPurchaseResponse = VerifyPurchaseResponse(
        status = VerificationStatus.VERIFIED,
        productId = PremiumProduct.MONTHLY.productId,
        productType = "SUBS",
        entitlement = VerifiedEntitlement.PREMIUM
    )
    var simulateException: Exception? = null
    var callCount = 0

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): VerifyPurchaseResponse {
        simulateException?.let { throw it }
        callCount++
        return mockResponse
    }
}
