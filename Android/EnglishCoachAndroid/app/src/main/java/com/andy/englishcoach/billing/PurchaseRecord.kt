package com.andy.englishcoach.billing

/**
 * Domain record of an active or pending Google Play purchase transaction.
 *
 * NOTE: [purchaseToken] is sensitive transaction data and must NEVER be:
 * - logged to Logcat / console output
 * - rendered on the UI
 * - committed to version control
 * - transmitted to external analytics or unauthorized services
 *
 * Important Entitlement Architecture Note:
 * This record represents client-side Google Play Billing state.
 * Client-side billing state != Server-verified purchase.
 * Official server-side verification and token validation will be integrated in STEP 16-D.
 */
data class PurchaseRecord(
    val productId: String,
    val purchaseToken: String,
    val purchaseState: Int,
    val isAcknowledged: Boolean,
    val purchaseTime: Long,
    val orderId: String? = null
) {
    /**
     * Protects the sensitive purchaseToken from leaking via logging or string evaluation.
     */
    override fun toString(): String {
        return "PurchaseRecord(productId='$productId', purchaseState=$purchaseState, isAcknowledged=$isAcknowledged, purchaseTime=$purchaseTime, orderId=$orderId, purchaseToken=[PROTECTED])"
    }
}
