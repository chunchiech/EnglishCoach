package com.andy.englishcoach.ui.dashboard

import com.andy.englishcoach.billing.DailyTargetPolicy
import com.andy.englishcoach.data.model.ToeicTarget

enum class DashboardCtaAction {
    START_LEARNING,
    START_QUIZ,
    NAVIGATE_REVIEW,
    COMPLETED
}

data class DashboardUiState(
    val greeting: String = "早安",
    val targetLevel: ToeicTarget = ToeicTarget.BASIC,
    val todayProgress: Float = 0f,
    val todayQuizCompletedCount: Int = 0,
    val maxQuizCount: Int = 10,
    val dailyPracticeQuotaUsed: Int = 0,
    val maxPracticeQuota: Int = 10,
    val isDailyLimitReached: Boolean = false,
    val isLearningCompleted: Boolean = false,
    val isQuizCompleted: Boolean = false,
    val ctaTitle: String = "開始今日練習",
    val ctaAction: DashboardCtaAction = DashboardCtaAction.START_LEARNING,
    val learnedWords: Int = 0,
    val totalWords: Int = 0,
    val accuracy: Double = 0.0,
    val reviewCount: Int = 0,
    val isPremium: Boolean = false,
    val currentEntitlement: com.andy.englishcoach.billing.PremiumEntitlement = com.andy.englishcoach.billing.PremiumEntitlement.Free,
    val isLoading: Boolean = false,
    val dailyTarget: Int = 10,
    val showDailyTargetSheet: Boolean = false,
    val availableDailyTargets: List<Int> = DailyTargetPolicy.Premium.allowedTargets
) {
    val isUnlimitedTarget: Boolean get() = dailyTarget == DailyTargetPolicy.UNLIMITED_TARGET
    val dailyTargetText: String
        get() = if (isUnlimitedTarget) "不限" else "${dailyTarget} 題"
}
