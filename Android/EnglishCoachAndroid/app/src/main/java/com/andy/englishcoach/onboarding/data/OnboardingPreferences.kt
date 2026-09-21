package com.andy.englishcoach.onboarding.data

import android.content.Context
import android.content.SharedPreferences
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.onboarding.model.LearningScenario

/**
 * Storage abstraction for Onboarding completion state, reminder time,
 * placement assessment results, and ordered learning contexts.
 * Source of truth: iOS PersonalizedOnboardingPreferences & NotificationOnboardingView.
 */
interface OnboardingPreferences {
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)

    fun isReminderEnabled(): Boolean
    fun setReminderEnabled(enabled: Boolean)

    fun getReminderHour(): Int
    fun getReminderMinute(): Int
    fun setReminderTime(hour: Int, minute: Int)

    fun isAssessmentCompleted(): Boolean
    fun setAssessmentCompleted(completed: Boolean)

    fun getAssessmentScore(): Int
    fun setAssessmentScore(score: Int)

    fun getRecommendedTargetLevel(): ToeicTarget
    fun setRecommendedTargetLevel(target: ToeicTarget)

    /**
     * Preserves strict priority order of selected learning scenarios.
     * Index 0 = Priority 1, Index 1 = Priority 2, etc.
     */
    fun getSelectedLearningContexts(): List<String>
    fun setSelectedLearningContexts(contexts: List<String>)

    fun toggleScenario(id: String): List<String>
    fun moveScenarioUp(id: String): List<String>
    fun moveScenarioDown(id: String): List<String>
    fun getScenarioDisplayText(): String?
}

/**
 * SharedPreferences implementation for Android.
 */
class SharedPreferencesOnboardingPreferences(
    private val prefs: SharedPreferences
) : OnboardingPreferences {

    companion object {
        const val PREFS_NAME = "englishcoach_preferences"

        const val KEY_ONBOARDING_COMPLETED = "hasCompletedPersonalizedOnboarding"
        const val KEY_NOTIFICATION_ONBOARDING_COMPLETED = "hasCompletedNotificationOnboarding"
        const val KEY_SELECTED_SCENARIOS = "userSelectedLearningScenarios"
        const val KEY_TARGET_SCORE = "userToeicTargetScore"
        const val KEY_PLACEMENT_SCORE = "userPlacementTestScore"
        const val KEY_RECOMMENDED_LEVEL = "userRecommendedStartingLevel"

        const val KEY_REMINDER_ENABLED = "daily_reminder_enabled"
        const val KEY_REMINDER_HOUR = "daily_reminder_hour"
        const val KEY_REMINDER_MINUTE = "daily_reminder_minute"

        const val DEFAULT_REMINDER_HOUR = 19
        const val DEFAULT_REMINDER_MINUTE = 0

        fun create(context: Context): SharedPreferencesOnboardingPreferences {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return SharedPreferencesOnboardingPreferences(sharedPrefs)
        }
    }

    override fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
            .putBoolean(KEY_NOTIFICATION_ONBOARDING_COMPLETED, completed)
            .apply()
    }

    override fun isReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, false)
    }

    override fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
    }

    override fun getReminderHour(): Int {
        return prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
    }

    override fun getReminderMinute(): Int {
        return prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE)
    }

    override fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }

    override fun isAssessmentCompleted(): Boolean {
        return prefs.contains(KEY_PLACEMENT_SCORE)
    }

    override fun setAssessmentCompleted(completed: Boolean) {
        if (!completed) {
            prefs.edit().remove(KEY_PLACEMENT_SCORE).apply()
        }
    }

    override fun getAssessmentScore(): Int {
        return prefs.getInt(KEY_PLACEMENT_SCORE, 0)
    }

    override fun setAssessmentScore(score: Int) {
        prefs.edit().putInt(KEY_PLACEMENT_SCORE, score).apply()
    }

    override fun getRecommendedTargetLevel(): ToeicTarget {
        val raw = prefs.getString(KEY_RECOMMENDED_LEVEL, null) ?: return ToeicTarget.BASIC
        return ToeicTarget.from(raw)
    }

    override fun setRecommendedTargetLevel(target: ToeicTarget) {
        prefs.edit()
            .putString(KEY_RECOMMENDED_LEVEL, target.rawLevel)
            .putString(KEY_TARGET_SCORE, target.targetScore)
            .apply()
    }

    override fun getSelectedLearningContexts(): List<String> {
        val raw = prefs.getString(KEY_SELECTED_SCENARIOS, null)
        if (raw.isNullOrBlank()) {
            return LearningScenario.DEFAULT_SELECTED_SCENARIO_IDS
        }
        val items = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return if (items.isEmpty()) LearningScenario.DEFAULT_SELECTED_SCENARIO_IDS else items
    }

    override fun setSelectedLearningContexts(contexts: List<String>) {
        val serialized = contexts.filter { it.isNotBlank() }.joinToString(",")
        prefs.edit().putString(KEY_SELECTED_SCENARIOS, serialized).apply()
    }

    override fun toggleScenario(id: String): List<String> {
        val current = getSelectedLearningContexts().toMutableList()
        val index = current.indexOf(id)
        if (index >= 0) {
            if (current.size > 1) {
                current.removeAt(index)
            }
        } else {
            current.add(id)
        }
        setSelectedLearningContexts(current)
        return current
    }

    override fun moveScenarioUp(id: String): List<String> {
        val current = getSelectedLearningContexts().toMutableList()
        val index = current.indexOf(id)
        if (index > 0) {
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            setSelectedLearningContexts(current)
        }
        return current
    }

    override fun moveScenarioDown(id: String): List<String> {
        val current = getSelectedLearningContexts().toMutableList()
        val index = current.indexOf(id)
        if (index >= 0 && index < current.size - 1) {
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            setSelectedLearningContexts(current)
        }
        return current
    }

    override fun getScenarioDisplayText(): String? {
        val ids = getSelectedLearningContexts()
        if (ids.isEmpty()) return null

        val map = LearningScenario.AVAILABLE_SCENARIOS.associate { it.id to it.cleanTitle }
        val names = ids.mapNotNull { map[it] }
        if (names.isEmpty()) return null

        return when (names.size) {
            1 -> "🎯 學習情境：${names[0]}"
            2 -> "🎯 學習情境：${names[0]} · ${names[1]}"
            else -> {
                val remaining = names.size - 2
                "🎯 學習情境：${names[0]} · ${names[1]} +$remaining"
            }
        }
    }
}

/**
 * In-memory implementation for deterministic unit testing.
 */
class InMemoryOnboardingPreferences : OnboardingPreferences {
    private var onboardingCompleted: Boolean = false
    private var reminderEnabled: Boolean = false
    private var reminderHour: Int = SharedPreferencesOnboardingPreferences.DEFAULT_REMINDER_HOUR
    private var reminderMinute: Int = SharedPreferencesOnboardingPreferences.DEFAULT_REMINDER_MINUTE
    private var assessmentCompleted: Boolean = false
    private var assessmentScore: Int = 0
    private var recommendedTargetLevel: ToeicTarget = ToeicTarget.BASIC
    private var selectedContexts: List<String> = LearningScenario.DEFAULT_SELECTED_SCENARIO_IDS

    override fun isOnboardingCompleted(): Boolean = onboardingCompleted
    override fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted = completed
    }

    override fun isReminderEnabled(): Boolean = reminderEnabled
    override fun setReminderEnabled(enabled: Boolean) {
        reminderEnabled = enabled
    }

    override fun getReminderHour(): Int = reminderHour
    override fun getReminderMinute(): Int = reminderMinute
    override fun setReminderTime(hour: Int, minute: Int) {
        reminderHour = hour
        reminderMinute = minute
    }

    override fun isAssessmentCompleted(): Boolean = assessmentCompleted
    override fun setAssessmentCompleted(completed: Boolean) {
        assessmentCompleted = completed
    }

    override fun getAssessmentScore(): Int = assessmentScore
    override fun setAssessmentScore(score: Int) {
        assessmentScore = score
        assessmentCompleted = true
    }

    override fun getRecommendedTargetLevel(): ToeicTarget = recommendedTargetLevel
    override fun setRecommendedTargetLevel(target: ToeicTarget) {
        recommendedTargetLevel = target
    }

    override fun getSelectedLearningContexts(): List<String> = selectedContexts
    override fun setSelectedLearningContexts(contexts: List<String>) {
        selectedContexts = contexts.filter { it.isNotBlank() }.ifEmpty {
            LearningScenario.DEFAULT_SELECTED_SCENARIO_IDS
        }
    }

    override fun toggleScenario(id: String): List<String> {
        val current = selectedContexts.toMutableList()
        val index = current.indexOf(id)
        if (index >= 0) {
            if (current.size > 1) {
                current.removeAt(index)
            }
        } else {
            current.add(id)
        }
        selectedContexts = current
        return current
    }

    override fun moveScenarioUp(id: String): List<String> {
        val current = selectedContexts.toMutableList()
        val index = current.indexOf(id)
        if (index > 0) {
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            selectedContexts = current
        }
        return current
    }

    override fun moveScenarioDown(id: String): List<String> {
        val current = selectedContexts.toMutableList()
        val index = current.indexOf(id)
        if (index >= 0 && index < current.size - 1) {
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            selectedContexts = current
        }
        return current
    }

    override fun getScenarioDisplayText(): String? {
        val ids = selectedContexts
        if (ids.isEmpty()) return null

        val map = LearningScenario.AVAILABLE_SCENARIOS.associate { it.id to it.cleanTitle }
        val names = ids.mapNotNull { map[it] }
        if (names.isEmpty()) return null

        return when (names.size) {
            1 -> "🎯 學習情境：${names[0]}"
            2 -> "🎯 學習情境：${names[0]} · ${names[1]}"
            else -> {
                val remaining = names.size - 2
                "🎯 學習情境：${names[0]} · ${names[1]} +$remaining"
            }
        }
    }
}
