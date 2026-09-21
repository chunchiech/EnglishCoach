package com.andy.englishcoach.billing

/**
 * Represents the state of the loaded Google Play product catalog.
 */
sealed interface BillingProductCatalog {
    data object Loading : BillingProductCatalog
    data class Available(val products: List<PremiumProductInfo>) : BillingProductCatalog
    data object Empty : BillingProductCatalog
    data class Error(val error: BillingError, val message: String = "") : BillingProductCatalog
}
