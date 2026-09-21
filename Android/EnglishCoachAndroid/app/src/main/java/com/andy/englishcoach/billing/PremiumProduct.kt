package com.andy.englishcoach.billing

/**
 * Type of commercial product.
 * Distinguishes between recurring subscriptions and one-time non-consumable purchases.
 */
enum class PremiumProductType {
    SUBSCRIPTION,
    ONE_TIME
}

/**
 * Commercial products for EnglishCoach Premium.
 * Follows iOS EnglishCoach production pricing and product tiers.
 */
enum class PremiumProduct(
    val productId: String,
    val title: String,
    val displayPrice: String,
    val billingPeriod: String,
    val type: PremiumProductType,
    val hasFreeTrial: Boolean = false,
    val freeTrialDays: Int = 0,
    val description: String
) {
    MONTHLY(
        productId = "com.andy.englishcoach.premium.monthly",
        title = "月繳方案",
        displayPrice = "NT$90",
        billingPeriod = "/ 月",
        type = PremiumProductType.SUBSCRIPTION,
        description = "彈性自主，隨時可取消"
    ),
    ANNUAL(
        productId = "com.andy.englishcoach.premium.annual",
        title = "年繳方案",
        displayPrice = "NT$690",
        billingPeriod = "/ 年",
        type = PremiumProductType.SUBSCRIPTION,
        hasFreeTrial = true,
        freeTrialDays = 7,
        description = "享有 7 天免費試用"
    ),
    LIFETIME(
        productId = "com.andy.englishcoach.premium.lifetime",
        title = "終身方案",
        displayPrice = "NT$1,290",
        billingPeriod = "一次性購買",
        type = PremiumProductType.ONE_TIME,
        description = "一次買斷，永久享有全部功能"
    );

    val isSubscription: Boolean get() = type == PremiumProductType.SUBSCRIPTION
    val isOneTime: Boolean get() = type == PremiumProductType.ONE_TIME

    val productInfo: PremiumProductInfo
        get() = PremiumProductInfo(
            product = this,
            productId = productId,
            title = title,
            displayPrice = displayPrice,
            billingPeriod = billingPeriod,
            type = type,
            hasFreeTrial = hasFreeTrial,
            freeTrialDays = freeTrialDays,
            description = description,
            currencyCode = "TWD",
            offerToken = null
        )
}

/**
 * Product presentation model for Paywall and UI displays.
 */
data class PremiumProductInfo(
    val product: PremiumProduct,
    val productId: String,
    val title: String,
    val displayPrice: String,
    val billingPeriod: String,
    val type: PremiumProductType,
    val hasFreeTrial: Boolean = false,
    val freeTrialDays: Int = 0,
    val description: String = "",
    val currencyCode: String = "TWD",
    val offerToken: String? = null
) {
    val isSubscription: Boolean get() = type == PremiumProductType.SUBSCRIPTION
    val isOneTime: Boolean get() = type == PremiumProductType.ONE_TIME
    val localizedPrice: String get() = displayPrice
    val trialDays: Int get() = freeTrialDays
}
