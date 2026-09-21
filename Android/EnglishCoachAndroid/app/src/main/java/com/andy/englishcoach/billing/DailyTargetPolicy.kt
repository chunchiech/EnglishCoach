package com.andy.englishcoach.billing

/**
 * Policy governing selectable daily learning targets.
 * Free users are restricted to 10 questions/day.
 * Premium users can select: 5, 10, 20, 30, 50, 100, Unlimited (-1).
 */
sealed interface DailyTargetPolicy {
    val allowedTargets: List<Int>
    val defaultTarget: Int
    fun isTargetAllowed(target: Int): Boolean
    fun coerceTarget(target: Int): Int

    data object Free : DailyTargetPolicy {
        override val allowedTargets: List<Int> = listOf(10)
        override val defaultTarget: Int = 10
        override fun isTargetAllowed(target: Int): Boolean = target == 10
        override fun coerceTarget(target: Int): Int = 10
    }

    data object Premium : DailyTargetPolicy {
        const val UNLIMITED = -1
        override val allowedTargets: List<Int> = listOf(5, 10, 20, 30, 50, 100, UNLIMITED)
        override val defaultTarget: Int = 10
        override fun isTargetAllowed(target: Int): Boolean = target in allowedTargets
        override fun coerceTarget(target: Int): Int = if (isTargetAllowed(target)) target else defaultTarget
    }

    companion object {
        const val UNLIMITED_TARGET = -1

        fun forEntitlement(entitlement: PremiumEntitlement): DailyTargetPolicy {
            return if (entitlement.isPremium) Premium else Free
        }

        fun forIsPremium(isPremium: Boolean): DailyTargetPolicy {
            return if (isPremium) Premium else Free
        }

        fun formatTargetLabel(target: Int): String {
            return if (target == UNLIMITED_TARGET) "無限題 (Unlimited)" else "${target} 題"
        }
    }
}
