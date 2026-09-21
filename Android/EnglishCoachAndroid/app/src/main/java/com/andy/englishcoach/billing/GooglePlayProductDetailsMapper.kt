package com.andy.englishcoach.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

/**
 * Intermediate data representations decoupling Google Play API details
 * from domain mapping logic, allowing deterministic unit testing.
 */
data class GooglePlayProductData(
    val productId: String,
    val productType: String,
    val title: String = "",
    val description: String = "",
    val subscriptionOffers: List<GooglePlaySubscriptionOfferData> = emptyList(),
    val oneTimeOffer: GooglePlayOneTimeOfferData? = null
)

data class GooglePlaySubscriptionOfferData(
    val basePlanId: String = "",
    val offerId: String? = null,
    val offerToken: String,
    val pricingPhases: List<GooglePlayPricingPhaseData> = emptyList()
)

data class GooglePlayPricingPhaseData(
    val formattedPrice: String,
    val priceCurrencyCode: String,
    val priceAmountMicros: Long,
    val billingPeriod: String,
    val recurrenceMode: Int = 1
)

data class GooglePlayOneTimeOfferData(
    val formattedPrice: String,
    val priceCurrencyCode: String,
    val priceAmountMicros: Long
)

/**
 * Deterministic mapper converting Google Play Billing ProductDetails into domain models.
 * Preserves localized pricing, currency, billing periods, and 7-day free trial offer tokens.
 */
object GooglePlayProductDetailsMapper {

    /**
     * Maps Google Play ProductDetails into [PremiumProductInfo].
     */
    fun map(productDetails: ProductDetails): PremiumProductInfo? {
        return mapProductData(fromProductDetails(productDetails))
    }

    /**
     * Adapts real ProductDetails into intermediate testable data.
     */
    fun fromProductDetails(details: ProductDetails): GooglePlayProductData {
        val subscriptionOffers = details.subscriptionOfferDetails?.map { offer ->
            GooglePlaySubscriptionOfferData(
                basePlanId = offer.basePlanId,
                offerId = offer.offerId,
                offerToken = offer.offerToken,
                pricingPhases = offer.pricingPhases.pricingPhaseList.map { phase ->
                    GooglePlayPricingPhaseData(
                        formattedPrice = phase.formattedPrice,
                        priceCurrencyCode = phase.priceCurrencyCode,
                        priceAmountMicros = phase.priceAmountMicros,
                        billingPeriod = phase.billingPeriod,
                        recurrenceMode = phase.recurrenceMode
                    )
                }
            )
        }.orEmpty()

        val oneTimeOffer = details.oneTimePurchaseOfferDetails?.let { oneTime ->
            GooglePlayOneTimeOfferData(
                formattedPrice = oneTime.formattedPrice,
                priceCurrencyCode = oneTime.priceCurrencyCode,
                priceAmountMicros = oneTime.priceAmountMicros
            )
        }

        return GooglePlayProductData(
            productId = details.productId,
            productType = details.productType,
            title = details.title,
            description = details.description,
            subscriptionOffers = subscriptionOffers,
            oneTimeOffer = oneTimeOffer
        )
    }

    /**
     * Maps product data to domain [PremiumProductInfo].
     */
    fun mapProductData(data: GooglePlayProductData): PremiumProductInfo? {
        val product = PremiumProduct.entries.firstOrNull { it.productId == data.productId }
            ?: return null

        return when (data.productType) {
            BillingClient.ProductType.SUBS -> mapSubscriptionData(data, product)
            BillingClient.ProductType.INAPP -> mapOneTimeData(data, product)
            else -> null
        }
    }

    /**
     * Maps subscription product data, inspecting subscriptionOffers for pricing phases and trials.
     */
    fun mapSubscriptionData(
        data: GooglePlayProductData,
        product: PremiumProduct
    ): PremiumProductInfo {
        val offers = data.subscriptionOffers

        // 1. Priority: look for an offer containing a free trial phase (price = 0)
        val trialOffer = offers.firstOrNull { offer ->
            offer.pricingPhases.any { it.priceAmountMicros == 0L }
        }

        if (trialOffer != null) {
            val trialPhase = trialOffer.pricingPhases.firstOrNull { it.priceAmountMicros == 0L }
            val recurringPhase = trialOffer.pricingPhases.firstOrNull { it.priceAmountMicros > 0L }
            val trialDays = parseIsoPeriodToDays(trialPhase?.billingPeriod) ?: product.freeTrialDays

            return PremiumProductInfo(
                product = product,
                productId = data.productId,
                title = product.title,
                displayPrice = recurringPhase?.formattedPrice ?: product.displayPrice,
                billingPeriod = product.billingPeriod,
                type = PremiumProductType.SUBSCRIPTION,
                hasFreeTrial = true,
                freeTrialDays = trialDays,
                description = product.description,
                currencyCode = recurringPhase?.priceCurrencyCode ?: "TWD",
                offerToken = trialOffer.offerToken
            )
        }

        // 2. Standard base plan without trial
        val defaultOffer = offers.firstOrNull()
        val standardPhase = defaultOffer?.pricingPhases?.firstOrNull()

        return PremiumProductInfo(
            product = product,
            productId = data.productId,
            title = product.title,
            displayPrice = standardPhase?.formattedPrice ?: product.displayPrice,
            billingPeriod = product.billingPeriod,
            type = PremiumProductType.SUBSCRIPTION,
            hasFreeTrial = false,
            freeTrialDays = 0,
            description = product.description,
            currencyCode = standardPhase?.priceCurrencyCode ?: "TWD",
            offerToken = defaultOffer?.offerToken
        )
    }

    /**
     * Maps one-time in-app product data (Lifetime).
     */
    fun mapOneTimeData(
        data: GooglePlayProductData,
        product: PremiumProduct
    ): PremiumProductInfo {
        val oneTimeOffer = data.oneTimeOffer

        return PremiumProductInfo(
            product = product,
            productId = data.productId,
            title = product.title,
            displayPrice = oneTimeOffer?.formattedPrice ?: product.displayPrice,
            billingPeriod = product.billingPeriod,
            type = PremiumProductType.ONE_TIME,
            hasFreeTrial = false,
            freeTrialDays = 0,
            description = product.description,
            currencyCode = oneTimeOffer?.priceCurrencyCode ?: "TWD",
            offerToken = null
        )
    }

    /**
     * Parses ISO-8601 duration format into days (e.g. "P7D" -> 7, "P1W" -> 7, "P1M" -> 30).
     */
    fun parseIsoPeriodToDays(isoPeriod: String?): Int? {
        if (isoPeriod.isNullOrBlank()) return null
        return try {
            val upper = isoPeriod.trim().uppercase()
            when {
                upper.startsWith("P") && upper.endsWith("D") -> {
                    upper.removePrefix("P").removeSuffix("D").toIntOrNull()
                }
                upper.startsWith("P") && upper.endsWith("W") -> {
                    (upper.removePrefix("P").removeSuffix("W").toIntOrNull() ?: 1) * 7
                }
                upper.startsWith("P") && upper.endsWith("M") -> {
                    (upper.removePrefix("P").removeSuffix("M").toIntOrNull() ?: 1) * 30
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
