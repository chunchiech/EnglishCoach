package com.andy.englishcoach.billing

/**
 * Policy governing access to Review Center quizzes.
 * Free: Can take review quiz while daily quota remains. Quota exhausted -> Paywall.
 * Premium: Unlimited access to review quizzes regardless of quota.
 */
sealed interface ReviewQuizGatingPolicy {
    fun canStartReviewQuiz(remainingQuota: Int, reviewWordCount: Int): Boolean
    fun shouldShowPaywallOnReviewQuiz(remainingQuota: Int, reviewWordCount: Int): Boolean

    data object Free : ReviewQuizGatingPolicy {
        override fun canStartReviewQuiz(remainingQuota: Int, reviewWordCount: Int): Boolean {
            return remainingQuota > 0 && reviewWordCount > 0
        }

        override fun shouldShowPaywallOnReviewQuiz(remainingQuota: Int, reviewWordCount: Int): Boolean {
            return remainingQuota <= 0 && reviewWordCount > 0
        }
    }

    data object Premium : ReviewQuizGatingPolicy {
        override fun canStartReviewQuiz(remainingQuota: Int, reviewWordCount: Int): Boolean {
            return reviewWordCount > 0
        }

        override fun shouldShowPaywallOnReviewQuiz(remainingQuota: Int, reviewWordCount: Int): Boolean {
            return false
        }
    }

    companion object {
        fun forEntitlement(entitlement: PremiumEntitlement): ReviewQuizGatingPolicy {
            return if (entitlement.isPremium) Premium else Free
        }

        fun forIsPremium(isPremium: Boolean): ReviewQuizGatingPolicy {
            return if (isPremium) Premium else Free
        }
    }
}
