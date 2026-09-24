package com.andy.englishcoach.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.englishcoach.billing.DailyTargetPolicy
import com.andy.englishcoach.billing.DefaultBillingRepository
import com.andy.englishcoach.billing.PremiumEntitlementProvider
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.DailyLearningPreferences
import com.andy.englishcoach.data.preference.InMemorySettingsPreferences
import com.andy.englishcoach.data.preference.SettingsPreferences
import com.andy.englishcoach.data.preference.SharedPreferencesDailyLearningPreferences
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.data.repository.ReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalTime

/**
 * ViewModel for the EnglishCoach Home / Dashboard Hub.
 * Coordinates target level selection, daily goal status, quota tracking,
 * learning statistics, and review center integration.
 * Source of truth: iOS DashboardView.swift & DailyPracticeManager.swift.
 */
class DashboardViewModel(
    private val learningRepository: DailyLearningRepository,
    private val quizRepository: QuizRepository,
    private val reviewRepository: ReviewRepository,
    private val preferences: DailyLearningPreferences,
    private val database: EnglishCoachDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val entitlementProvider: PremiumEntitlementProvider = DefaultBillingRepository(),
    private val settingsPreferences: SettingsPreferences = InMemorySettingsPreferences()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setTargetLevel(target: ToeicTarget) {
        learningRepository.setUserTargetLevel(target)
        refresh()
    }

    fun selectDailyTarget(target: Int): Boolean {
        val isPremium = entitlementProvider.isPremium
        val policy = DailyTargetPolicy.forIsPremium(isPremium)
        if (!policy.isTargetAllowed(target)) {
            return false
        }
        settingsPreferences.setDailyTarget(target)
        refresh()
        return true
    }

    fun setDailyTargetSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showDailyTargetSheet = visible) }
    }

    fun refresh() {
        viewModelScope.launch {
            val newState = withContext(Dispatchers.IO) {
                calculateDashboardState()
            }
            _uiState.update { current ->
                newState.copy(showDailyTargetSheet = current.showDailyTargetSheet)
            }
        }
    }

    fun refreshSync(): DashboardUiState {
        val newState = calculateDashboardState()
        _uiState.update { current ->
            newState.copy(showDailyTargetSheet = current.showDailyTargetSheet)
        }
        return newState
    }

    fun calculateDashboardState(): DashboardUiState {
        val target = learningRepository.getUserTargetLevel()
        val today = learningRepository.getTodayDateString()

        // Greeting based on current time
        val hour = LocalTime.now(clock).hour
        val greeting = when {
            hour < 12 -> "早安"
            hour < 18 -> "午安"
            else -> "晚安"
        }

        val isPremium = entitlementProvider.isPremium
        val policy = DailyTargetPolicy.forIsPremium(isPremium)
        val dailyTarget = policy.coerceTarget(settingsPreferences.getDailyTarget())
        val isUnlimited = dailyTarget == DailyTargetPolicy.UNLIMITED_TARGET
        val maxPracticeQuota = SharedPreferencesDailyLearningPreferences.DEFAULT_MAX_FREE_DAILY_QUESTIONS

        val isLearningCompleted = learningRepository.isTodayLearningCompleted()
        val isQuizCompleted = quizRepository.isTodayQuizCompleted()
        val todayWords = learningRepository.getTodayWords(target = target)
        val reviewCount = reviewRepository.getReviewCount(date = today, targetLevel = target)

        // Quota & Quiz counts
        val practiceDate = preferences.getDailyPracticeDate()
        val dailyPracticeQuotaUsed = if (practiceDate == today) preferences.getDailyPracticeCount() else 0
        val isDailyLimitReached = if (isPremium) {
            if (isUnlimited) false else dailyPracticeQuotaUsed >= dailyTarget
        } else {
            dailyPracticeQuotaUsed >= maxPracticeQuota
        }

        val todayQuizCompletedCount = if (isQuizCompleted) {
            if (isUnlimited) dailyPracticeQuotaUsed.coerceAtLeast(10) else dailyTarget
        } else {
            0
        }

        val maxQuizCount = if (isUnlimited) DailyTargetPolicy.UNLIMITED_TARGET else dailyTarget

        val todayProgress = when {
            isUnlimited -> if (dailyPracticeQuotaUsed > 0 || isQuizCompleted) 1f else 0f
            dailyTarget > 0 -> (dailyPracticeQuotaUsed.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }

        // Vocabulary & Progress Statistics
        val totalWords = database.vocabularyDao().countWordsByLevel(target.rawLevel)
        val unlearnedCount = database.vocabularyDao().getUnlearnedWords(target.rawLevel).size
        val learnedWords = maxOf(0, totalWords - unlearnedCount)

        var totalCorrect = 0
        var totalWrong = 0
        try {
            database.openHelper.readableDatabase.query("SELECT SUM(correct_count), SUM(wrong_count) FROM word_progress").use { cursor ->
                if (cursor.moveToFirst()) {
                    totalCorrect = cursor.getInt(0)
                    totalWrong = cursor.getInt(1)
                }
            }
        } catch (_: Exception) {
            // Table may be empty or uninitialized in test scenarios
        }
        val totalAttempts = totalCorrect + totalWrong
        val accuracy = if (totalAttempts > 0) (totalCorrect.toDouble() / totalAttempts.toDouble()) * 100.0 else 0.0

        // CTA Button Title & Action (strictly aligned with iOS lines 203-218 & 220-238)
        val isLearningDoneAwaitingQuiz = isLearningCompleted && !isQuizCompleted
        val (ctaTitle, ctaAction) = when {
            isLearningDoneAwaitingQuiz -> {
                "開始今日測驗 ➜" to DashboardCtaAction.START_QUIZ
            }
            isDailyLimitReached || (isLearningCompleted && isQuizCompleted) -> {
                "🎉 今日學習已完成" to DashboardCtaAction.COMPLETED
            }
            todayWords.isEmpty() && learnedWords > 0 -> {
                if (reviewCount > 0) {
                    "本目標已完成 · 前往複習 ➜" to DashboardCtaAction.NAVIGATE_REVIEW
                } else {
                    "🎉 本目標單字已全部學完" to DashboardCtaAction.COMPLETED
                }
            }
            todayProgress > 0f -> {
                "繼續今日學習 ➜" to DashboardCtaAction.START_LEARNING
            }
            else -> {
                "開始今日練習" to DashboardCtaAction.START_LEARNING
            }
        }

        return DashboardUiState(
            greeting = greeting,
            targetLevel = target,
            todayProgress = todayProgress,
            todayQuizCompletedCount = todayQuizCompletedCount,
            maxQuizCount = maxQuizCount,
            dailyPracticeQuotaUsed = dailyPracticeQuotaUsed,
            maxPracticeQuota = maxPracticeQuota,
            isDailyLimitReached = isDailyLimitReached,
            isLearningCompleted = isLearningCompleted,
            isQuizCompleted = isQuizCompleted,
            ctaTitle = ctaTitle,
            ctaAction = ctaAction,
            learnedWords = learnedWords,
            totalWords = totalWords,
            accuracy = accuracy,
            reviewCount = reviewCount,
            isPremium = isPremium,
            isLoading = false,
            dailyTarget = dailyTarget
        )
    }
}
