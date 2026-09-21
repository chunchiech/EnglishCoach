package com.andy.englishcoach.billing

/**
 * Domain representation of Google Play purchase or restore results.
 * Decouples Google Play internal BillingResult from UI and domain layers.
 */
sealed interface BillingResult {
    data class Success(val entitlement: PremiumEntitlement) : BillingResult
    data class Cancelled(val message: String = "已取消購買") : BillingResult
    data class Pending(val message: String = "付款處理中，完成付款後 Premium 將會啟用") : BillingResult
    data class Error(val message: String, val cause: Throwable? = null) : BillingResult
}

/**
 * Type alias connecting domain purchase results to the billing abstraction.
 */
typealias BillingPurchaseResult = BillingResult
