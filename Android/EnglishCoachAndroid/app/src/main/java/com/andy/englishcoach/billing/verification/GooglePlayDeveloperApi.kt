package com.andy.englishcoach.billing.verification

/**
 * Server-side abstraction of the official Google Play Developer API:
 * - androidpublisher.purchases.subscriptions.get
 * - androidpublisher.purchases.products.get
 *
 * Security & Deployment Note:
 * This interface represents the backend-to-Google communication boundary.
 * Google Service Account credentials, private keys, and API client secrets
 * MUST ONLY reside on secure backend servers and NEVER on Android clients.
 */
interface GooglePlayDeveloperApi {
    suspend fun verifySubscription(
        packageName: String,
        subscriptionId: String,
        token: String
    ): GooglePlaySubscriptionResult

    suspend fun verifyProduct(
        packageName: String,
        productId: String,
        token: String
    ): GooglePlayProductResult
}

data class GooglePlaySubscriptionResult(
    val paymentState: Int, // 1 = Payment received, 2 = Free trial, 0 = Pending
    val acknowledgementState: Int, // 0 = Yet to be acknowledged, 1 = Acknowledged
    val expiryTimeMillis: Long,
    val autoRenewing: Boolean,
    val cancelReason: Int? = null,
    val isValid: Boolean = true
)

data class GooglePlayProductResult(
    val purchaseState: Int, // 0 = Purchased, 1 = Canceled, 2 = Pending
    val consumptionState: Int, // 0 = Yet to be consumed, 1 = Consumed
    val acknowledgementState: Int, // 0 = Yet to be acknowledged, 1 = Acknowledged
    val purchaseTimeMillis: Long,
    val isValid: Boolean = true
)

/**
 * In-memory test implementation simulating Google Play Developer API responses.
 */
class FakeGooglePlayDeveloperApi : GooglePlayDeveloperApi {

    var simulateSubscriptionResult: GooglePlaySubscriptionResult? = null
    var simulateProductResult: GooglePlayProductResult? = null
    var simulateError: Boolean = false

    override suspend fun verifySubscription(
        packageName: String,
        subscriptionId: String,
        token: String
    ): GooglePlaySubscriptionResult {
        if (simulateError) throw IllegalStateException("Google Play Developer API temporary error")
        return simulateSubscriptionResult ?: GooglePlaySubscriptionResult(
            paymentState = 1,
            acknowledgementState = 1,
            expiryTimeMillis = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
            autoRenewing = true,
            isValid = true
        )
    }

    override suspend fun verifyProduct(
        packageName: String,
        productId: String,
        token: String
    ): GooglePlayProductResult {
        if (simulateError) throw IllegalStateException("Google Play Developer API temporary error")
        return simulateProductResult ?: GooglePlayProductResult(
            purchaseState = 0,
            consumptionState = 0,
            acknowledgementState = 1,
            purchaseTimeMillis = System.currentTimeMillis(),
            isValid = true
        )
    }
}
