package com.andy.englishcoach.billing

import com.android.billingclient.api.BillingClient

/**
 * Domain representations of billing and product catalog query errors.
 */
enum class BillingError {
    SERVICE_UNAVAILABLE,
    BILLING_UNAVAILABLE,
    NETWORK_ERROR,
    DEVELOPER_ERROR,
    FEATURE_NOT_SUPPORTED,
    ITEM_UNAVAILABLE,
    UNKNOWN;

    /**
     * Converts domain errors into user-friendly localized messages without leaking stack traces.
     */
    fun toUserFacingMessage(): String {
        return when (this) {
            SERVICE_UNAVAILABLE,
            BILLING_UNAVAILABLE -> "目前無法使用 Google Play 付款服務"
            ITEM_UNAVAILABLE -> "此方案目前無法購買"
            NETWORK_ERROR -> "網路連線異常，請稍後再試"
            DEVELOPER_ERROR,
            FEATURE_NOT_SUPPORTED,
            UNKNOWN -> "購買暫時無法完成"
        }
    }

    companion object {
        fun fromBillingResponseCode(responseCode: Int): BillingError {
            return when (responseCode) {
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> SERVICE_UNAVAILABLE
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> BILLING_UNAVAILABLE
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> ITEM_UNAVAILABLE
                BillingClient.BillingResponseCode.DEVELOPER_ERROR -> DEVELOPER_ERROR
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> FEATURE_NOT_SUPPORTED
                BillingClient.BillingResponseCode.NETWORK_ERROR -> NETWORK_ERROR
                else -> UNKNOWN
            }
        }

        fun mapResponseCodeToMessage(responseCode: Int): String {
            return when (responseCode) {
                BillingClient.BillingResponseCode.USER_CANCELED -> "已取消購買"
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "目前無法使用 Google Play 付款服務"
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "此方案目前無法購買"
                BillingClient.BillingResponseCode.NETWORK_ERROR -> "網路連線異常，請稍後再試"
                else -> "購買暫時無法完成"
            }
        }
    }
}
