package com.andy.englishcoach.billing.verification

import com.andy.englishcoach.billing.PremiumProduct

/**
 * Verification status returned by the Google Play Developer API via Backend.
 */
enum class VerificationStatus {
    VERIFIED,
    PENDING,
    EXPIRED,
    REVOKED,
    INVALID,
    NOT_FOUND,
    TEMPORARY_ERROR;

    val isVerified: Boolean get() = this == VERIFIED
}

/**
 * Server-verified entitlement authority for EnglishCoach Android.
 * Note: Client-side local entitlement is only a cached presentation state.
 * Backend-verified entitlement is the ultimate commercial source of truth.
 */
enum class VerifiedEntitlement {
    FREE,
    PREMIUM;

    val isPremium: Boolean get() = this == PREMIUM
}

/**
 * Request payload for POST /v1/google-play/verify-purchase.
 *
 * Security note:
 * [purchaseToken] is sensitive transaction data and MUST NEVER be:
 * - logged to logcat / console
 * - displayed on UI
 * - transmitted via unencrypted HTTP
 * - persisted into insecure local storage
 */
data class VerifyPurchaseRequest(
    val productId: String,
    val purchaseToken: String,
    val productType: String // "SUBS" or "INAPP"
) {
    /**
     * Prevents accidental leaking of purchaseToken in logs or debugging output.
     */
    override fun toString(): String {
        return "VerifyPurchaseRequest(productId='$productId', productType='$productType', purchaseToken=[PROTECTED])"
    }
}

/**
 * Response payload returned by the Backend verification endpoint.
 * Decouples internal Google Play Developer API responses into domain contract.
 */
data class VerifyPurchaseResponse(
    val status: VerificationStatus,
    val productId: String,
    val productType: String,
    val entitlement: VerifiedEntitlement,
    val expiryTimeMillis: Long? = null,
    val message: String? = null
) {
    override fun toString(): String {
        return "VerifyPurchaseResponse(status=$status, productId='$productId', productType='$productType', entitlement=$entitlement, expiryTimeMillis=$expiryTimeMillis)"
    }
}

/**
 * Domain-level verification result passed to ViewModels and UI layers.
 */
sealed interface VerificationResult {
    data class Success(
        val entitlement: VerifiedEntitlement,
        val product: PremiumProduct,
        val status: VerificationStatus = VerificationStatus.VERIFIED,
        val expiryTimeMillis: Long? = null
    ) : VerificationResult

    data class Pending(val message: String = "付款處理中，驗證完成後將自動開通") : VerificationResult
    data class Expired(val message: String = "訂閱方案已過期") : VerificationResult
    data class Revoked(val message: String = "訂閱已被撤銷或退款") : VerificationResult
    data class Invalid(val message: String = "無效的購買憑證") : VerificationResult
    data class TemporaryError(val message: String = "伺服器暫時無法連線，將於稍後重試") : VerificationResult
}
