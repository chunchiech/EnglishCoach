package com.andy.englishcoach.billing.verification

import com.andy.englishcoach.billing.PremiumProduct

/**
 * Validates purchase verification requests against schema and security policies
 * before sending to Backend or processing by verification adapters.
 */
object PurchaseVerificationValidator {

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    fun validate(request: VerifyPurchaseRequest): ValidationResult {
        if (request.purchaseToken.isBlank()) {
            return ValidationResult.Invalid("Purchase token cannot be empty")
        }

        if (request.productId.isBlank()) {
            return ValidationResult.Invalid("Product ID cannot be empty")
        }

        val matchedProduct = PremiumProduct.entries.firstOrNull { it.productId == request.productId }
            ?: return ValidationResult.Invalid("Unknown product ID: ${request.productId}")

        if (request.productType != "SUBS" && request.productType != "INAPP") {
            return ValidationResult.Invalid("Invalid product type: ${request.productType}. Expected SUBS or INAPP.")
        }

        // Validate product type matches product specification
        if (matchedProduct.isSubscription && request.productType != "SUBS") {
            return ValidationResult.Invalid("Product ${matchedProduct.productId} requires productType SUBS but received ${request.productType}")
        }
        if (matchedProduct.isOneTime && request.productType != "INAPP") {
            return ValidationResult.Invalid("Product ${matchedProduct.productId} requires productType INAPP but received ${request.productType}")
        }

        return ValidationResult.Valid
    }
}
