package com.andy.englishcoach.billing

/**
 * Single authority representation of the user's Premium entitlement.
 * MUST be governed by the Billing layer, never solely by local preferences.
 */
sealed interface PremiumEntitlement {
    val isPremium: Boolean

    /**
     * Default standard tier: 10 questions/day quota, standard review center.
     */
    data object Free : PremiumEntitlement {
        override val isPremium: Boolean = false
        override fun toString(): String = "PremiumEntitlement.Free"
    }

    /**
     * Entitled Premium tier: unlocked learning targets, unlimited review quiz.
     */
    data class Premium(
        val product: PremiumProduct,
        val purchaseTimestamp: Long = System.currentTimeMillis(),
        val isLifetime: Boolean = (product == PremiumProduct.LIFETIME)
    ) : PremiumEntitlement {
        override val isPremium: Boolean = true
    }
}
