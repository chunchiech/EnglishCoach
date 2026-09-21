package com.andy.englishcoach.ui.settings

import androidx.lifecycle.ViewModel
import com.andy.englishcoach.billing.DailyTargetPolicy
import com.andy.englishcoach.billing.DefaultBillingRepository
import com.andy.englishcoach.billing.PremiumEntitlementProvider
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.DailyLearningPreferences
import com.andy.englishcoach.data.preference.SettingsPreferences
import com.andy.englishcoach.data.preference.SharedPreferencesDailyLearningPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel managing the state and actions for the Settings and Personalization screen.
 */
class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences,
    private val learningPreferences: DailyLearningPreferences,
    private val versionName: String = "1.0",
    private val entitlementProvider: PremiumEntitlementProvider = DefaultBillingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(versionName = versionName))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val userLevel = ToeicTarget.from(learningPreferences.getUserLevel())
        val savedDate = learningPreferences.getDailyPracticeDate()
        val practiceCount = if (savedDate == todayStr) learningPreferences.getDailyPracticeCount() else 0
        val remaining = learningPreferences.getRemainingPracticeQuota(
            todayStr,
            SharedPreferencesDailyLearningPreferences.DEFAULT_MAX_FREE_DAILY_QUESTIONS
        )
        val isPremium = entitlementProvider.isPremium
        val policy = DailyTargetPolicy.forIsPremium(isPremium)
        val currentDailyTarget = policy.coerceTarget(settingsPreferences.getDailyTarget())

        _uiState.update {
            it.copy(
                displayName = settingsPreferences.getDisplayName(),
                avatarEmoji = settingsPreferences.getAvatarEmoji(),
                autoReadEnabled = settingsPreferences.isAutoReadEnabled(),
                hapticFeedbackEnabled = settingsPreferences.isHapticFeedbackEnabled(),
                targetLevel = userLevel,
                todayCompletedCount = practiceCount,
                dailyTarget = currentDailyTarget,
                remainingFreeQuestions = remaining,
                versionName = versionName,
                isPremium = isPremium,
                dailyTargetPolicy = policy
            )
        }
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

    fun updateProfile(name: String, emoji: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            settingsPreferences.setDisplayName(trimmed)
        }
        settingsPreferences.setAvatarEmoji(emoji)
        refresh()
    }

    fun setAutoReadEnabled(enabled: Boolean) {
        settingsPreferences.setAutoReadEnabled(enabled)
        _uiState.update { it.copy(autoReadEnabled = enabled) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        settingsPreferences.setHapticFeedbackEnabled(enabled)
        _uiState.update { it.copy(hapticFeedbackEnabled = enabled) }
    }

    fun setEditProfileSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showEditProfileSheet = visible) }
    }
}
