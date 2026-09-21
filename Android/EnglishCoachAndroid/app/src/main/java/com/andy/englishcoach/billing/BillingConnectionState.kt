package com.andy.englishcoach.billing

/**
 * State of the connection between the application and Google Play Billing Service.
 */
sealed interface BillingConnectionState {
    data object Disconnected : BillingConnectionState
    data object Connecting : BillingConnectionState
    data object Connected : BillingConnectionState
    data class Unavailable(val message: String = "Google Play 商店連線不可用") : BillingConnectionState
}
