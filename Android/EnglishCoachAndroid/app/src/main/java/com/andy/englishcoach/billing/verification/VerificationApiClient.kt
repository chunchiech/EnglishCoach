package com.andy.englishcoach.billing.verification

import com.andy.englishcoach.billing.PremiumProduct

/**
 * HTTPS API Client abstraction connecting Android to the server-side verification endpoint.
 */
interface VerificationApiClient {
    /**
     * Sends verification request to POST /v1/google-play/verify-purchase.
     */
    suspend fun verifyPurchase(request: VerifyPurchaseRequest): VerifyPurchaseResponse
}

/**
 * Production-ready API client abstraction enforcing HTTPS transport and safe error handling.
 */
class DefaultVerificationApiClient(
    val endpointUrl: String = "https://api.englishcoach.app/v1/google-play/verify-purchase"
) : VerificationApiClient {

    init {
        require(endpointUrl.startsWith("https://", ignoreCase = true)) {
            "Insecure HTTP connection rejected. Purchase token verification requires HTTPS."
        }
    }

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): VerifyPurchaseResponse {
        val validation = PurchaseVerificationValidator.validate(request)
        if (validation is PurchaseVerificationValidator.ValidationResult.Invalid) {
            return VerifyPurchaseResponse(
                status = VerificationStatus.INVALID,
                productId = request.productId,
                productType = request.productType,
                entitlement = VerifiedEntitlement.FREE,
                message = validation.reason
            )
        }

        // In production, this issues an authenticated HTTPS POST with JSON payload.
        // For STEP 16-D architectural baseline without production backend deployment,
        // it serves as the safe contract boundary.
        return VerifyPurchaseResponse(
            status = VerificationStatus.TEMPORARY_ERROR,
            productId = request.productId,
            productType = request.productType,
            entitlement = VerifiedEntitlement.FREE,
            message = "Production backend not deployed (STEP 16-D architectural baseline)"
        )
    }
}
