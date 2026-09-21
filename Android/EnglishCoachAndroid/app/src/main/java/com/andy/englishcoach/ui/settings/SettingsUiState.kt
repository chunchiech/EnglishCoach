package com.andy.englishcoach.ui.settings

import com.andy.englishcoach.billing.DailyTargetPolicy
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.SharedPreferencesDailyLearningPreferences
import com.andy.englishcoach.data.preference.SharedPreferencesSettingsPreferences

/**
 * UI State for the Settings / Product Shell screen.
 */
data class SettingsUiState(
    val displayName: String = SharedPreferencesSettingsPreferences.DEFAULT_DISPLAY_NAME,
    val avatarEmoji: String = SharedPreferencesSettingsPreferences.DEFAULT_AVATAR_EMOJI,
    val autoReadEnabled: Boolean = SharedPreferencesSettingsPreferences.DEFAULT_AUTO_READ_TTS,
    val hapticFeedbackEnabled: Boolean = SharedPreferencesSettingsPreferences.DEFAULT_HAPTIC_FEEDBACK,
    val targetLevel: ToeicTarget = ToeicTarget.BASIC,
    val todayCompletedCount: Int = 0,
    val dailyTarget: Int = SharedPreferencesDailyLearningPreferences.DEFAULT_MAX_FREE_DAILY_QUESTIONS,
    val remainingFreeQuestions: Int = SharedPreferencesDailyLearningPreferences.DEFAULT_MAX_FREE_DAILY_QUESTIONS,
    val versionName: String = "1.0",
    val availableEmojis: List<String> = SharedPreferencesSettingsPreferences.AVAILABLE_AVATAR_EMOJIS,
    val showEditProfileSheet: Boolean = false,
    val isPremium: Boolean = false,
    val dailyTargetPolicy: DailyTargetPolicy = DailyTargetPolicy.Free,
    val availableDailyTargets: List<Int> = listOf(5, 10, 20, 30, 50, 100, DailyTargetPolicy.UNLIMITED_TARGET),
    val showDailyTargetSheet: Boolean = false,
    val learningScenariosText: String? = null,
    val selectedScenarioIds: List<String> = emptyList(),
    val isReminderEnabled: Boolean = false,
    val reminderTimeText: String = "19:00",
    val showLearningScenariosSheet: Boolean = false,
    val showReminderTimeDialog: Boolean = false
)
