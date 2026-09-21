package com.andy.englishcoach.billing.verification

import com.andy.englishcoach.billing.PremiumProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository orchestrating purchase token server verification and maintaining
 * server-verified commercial entitlement state.
 *
 * Responsibilities:
 * - Delegates verification to [VerificationApiClient]
 * - Maintains offline cache and retry policy
 * - Preserves existing Premium on temporary server errors (does not prematurely downgrade)
 * - Revokes entitlement when server reports EXPIRED or REVOKED
 * - Guarantees idempotent handling of duplicate purchase tokens
 */
interface PurchaseVerificationRepository {
    val verifiedEntitlement: StateFlow<VerifiedEntitlement>
    val isPremium: Boolean get() = verifiedEntitlement.value == VerifiedEntitlement.PREMIUM

    suspend fun verifyPurchase(
        productId: String,
        purchaseToken: String,
        productType: String
    ): VerificationResult

    fun getCachedEntitlement(): VerifiedEntitlement
    fun clearCache()
}

/**
 * Production implementation of [PurchaseVerificationRepository].
 */
class DefaultPurchaseVerificationRepository(
    private val apiClient: VerificationApiClient,
    initialEntitlement: VerifiedEntitlement = VerifiedEntitlement.FREE
) : PurchaseVerificationRepository {

    private val _verifiedEntitlement = MutableStateFlow(initialEntitlement)
    override val verifiedEntitlement: StateFlow<VerifiedEntitlement> = _verifiedEntitlement.asStateFlow()

    private var cachedEntitlement: VerifiedEntitlement = initialEntitlement
    private val verifiedTokenCache = ConcurrentHashMap<String, VerificationResult>()

    override fun getCachedEntitlement(): VerifiedEntitlement = cachedEntitlement

    override fun clearCache() {
        cachedEntitlement = VerifiedEntitlement.FREE
        _verifiedEntitlement.value = VerifiedEntitlement.FREE
        verifiedTokenCache.clear()
    }

    override suspend fun verifyPurchase(
        productId: String,
        purchaseToken: String,
        productType: String
    ): VerificationResult {
        // Section XVIII: Idempotency check
        verifiedTokenCache[purchaseToken]?.let { cachedResult ->
            return cachedResult
        }

        val request = VerifyPurchaseRequest(
            productId = productId,
            purchaseToken = purchaseToken,
            productType = productType
        )

        // Validation gate
        val validation = PurchaseVerificationValidator.validate(request)
        if (validation is PurchaseVerificationValidator.ValidationResult.Invalid) {
            return VerificationResult.Invalid(validation.reason)
        }

        val matchedProduct = PremiumProduct.entries.first { it.productId == productId }

        val response = try {
            apiClient.verifyPurchase(request)
        } catch (e: Exception) {
            // Section XV: Network or HTTP errors mapped to TEMPORARY_ERROR
            VerifyPurchaseResponse(
                status = VerificationStatus.TEMPORARY_ERROR,
                productId = productId,
                productType = productType,
                entitlement = cachedEntitlement,
                message = e.message
            )
        }

        val result = when (response.status) {
            VerificationStatus.VERIFIED -> {
                cachedEntitlement = VerifiedEntitlement.PREMIUM
                _verifiedEntitlement.value = VerifiedEntitlement.PREMIUM
                VerificationResult.Success(
                    entitlement = VerifiedEntitlement.PREMIUM,
                    product = matchedProduct,
                    status = VerificationStatus.VERIFIED,
                    expiryTimeMillis = response.expiryTimeMillis
                )
            }
            VerificationStatus.PENDING -> {
                // Section IX: PENDING does NOT grant Premium
                VerificationResult.Pending(response.message ?: "付款處理中，驗證完成後將自動開通")
            }
            VerificationStatus.EXPIRED -> {
                // Section IX: Expired subscription resets to FREE
                cachedEntitlement = VerifiedEntitlement.FREE
                _verifiedEntitlement.value = VerifiedEntitlement.FREE
                VerificationResult.Expired(response.message ?: "訂閱方案已過期")
            }
            VerificationStatus.REVOKED -> {
                // Section IX: Revoked subscription resets to FREE
                cachedEntitlement = VerifiedEntitlement.FREE
                _verifiedEntitlement.value = VerifiedEntitlement.FREE
                VerificationResult.Revoked(response.message ?: "訂閱已被撤銷或退款")
            }
            VerificationStatus.INVALID,
            VerificationStatus.NOT_FOUND -> {
                VerificationResult.Invalid(response.message ?: "無效的購買憑證")
            }
            VerificationStatus.TEMPORARY_ERROR -> {
                // Section XV & XVI: TEMPORARY_ERROR preserves existing cached Premium!
                // Does NOT revoke existing Premium on single failure or network timeout.
                VerificationResult.TemporaryError(response.message ?: "伺服器暫時無法連線，將於稍後重試")
            }
        }

        if (result is VerificationResult.Success) {
            verifiedTokenCache[purchaseToken] = result
        }

        return result
    }
}

/**
 * In-memory deterministic Fake repository for testing verification architecture without network.
 */
class FakePurchaseVerificationRepository(
    initialEntitlement: VerifiedEntitlement = VerifiedEntitlement.FREE
) : PurchaseVerificationRepository {

    private val _verifiedEntitlement = MutableStateFlow(initialEntitlement)
    override val verifiedEntitlement: StateFlow<VerifiedEntitlement> = _verifiedEntitlement.asStateFlow()

    var mockStatus: VerificationStatus = VerificationStatus.VERIFIED
    var mockExpiryTimeMillis: Long? = null
    var simulateNetworkError: Boolean = false
    var isOffline: Boolean = false
    var errorMessage: String = "模擬連線錯誤"

    private var cachedEntitlement: VerifiedEntitlement = initialEntitlement
    private val processedTokens = mutableSetOf<String>()

    override fun getCachedEntitlement(): VerifiedEntitlement = cachedEntitlement

    override fun clearCache() {
        cachedEntitlement = VerifiedEntitlement.FREE
        _verifiedEntitlement.value = VerifiedEntitlement.FREE
        processedTokens.clear()
    }

    fun setEntitlementForTesting(entitlement: VerifiedEntitlement) {
        cachedEntitlement = entitlement
        _verifiedEntitlement.value = entitlement
    }

    override suspend fun verifyPurchase(
        productId: String,
        purchaseToken: String,
        productType: String
    ): VerificationResult {
        val request = VerifyPurchaseRequest(productId, purchaseToken, productType)
        val validation = PurchaseVerificationValidator.validate(request)
        if (validation is PurchaseVerificationValidator.ValidationResult.Invalid) {
            return VerificationResult.Invalid(validation.reason)
        }

        val matchedProduct = PremiumProduct.entries.first { it.productId == productId }

        if (isOffline || simulateNetworkError) {
            // Section XV & XVI: Offline / Network error preserves existing cached entitlement
            return VerificationResult.TemporaryError(errorMessage)
        }

        processedTokens.add(purchaseToken)

        return when (mockStatus) {
            VerificationStatus.VERIFIED -> {
                cachedEntitlement = VerifiedEntitlement.PREMIUM
                _verifiedEntitlement.value = VerifiedEntitlement.PREMIUM
                VerificationResult.Success(
                    entitlement = VerifiedEntitlement.PREMIUM,
                    product = matchedProduct,
                    status = VerificationStatus.VERIFIED,
                    expiryTimeMillis = mockExpiryTimeMillis
                )
            }
            VerificationStatus.PENDING -> {
                VerificationResult.Pending()
            }
            VerificationStatus.EXPIRED -> {
                cachedEntitlement = VerifiedEntitlement.FREE
                _verifiedEntitlement.value = VerifiedEntitlement.FREE
                VerificationResult.Expired()
            }
            VerificationStatus.REVOKED -> {
                cachedEntitlement = VerifiedEntitlement.FREE
                _verifiedEntitlement.value = VerifiedEntitlement.FREE
                VerificationResult.Revoked()
            }
            VerificationStatus.INVALID,
            VerificationStatus.NOT_FOUND -> {
                VerificationResult.Invalid()
            }
            VerificationStatus.TEMPORARY_ERROR -> {
                VerificationResult.TemporaryError(errorMessage)
            }
        }
    }
}
