package com.andy.englishcoach.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.andy.englishcoach.onboarding.data.OnboardingPreferences

/**
 * ViewModel managing the state and actions for the Settings and Personalization screen.
 */
class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences,
    private val learningPreferences: DailyLearningPreferences,
    private val versionName: String = "1.0",
    private val entitlementProvider: PremiumEntitlementProvider = DefaultBillingRepository(),
    private val onboardingPreferences: OnboardingPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(versionName = versionName))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            entitlementProvider.entitlement.collect {
                refresh()
            }
        }
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
        val entitlement = entitlementProvider.entitlement.value
        val isPremium = entitlement.isPremium
        val policy = DailyTargetPolicy.forIsPremium(isPremium)
        val currentDailyTarget = policy.coerceTarget(settingsPreferences.getDailyTarget())
        val scenariosText = onboardingPreferences?.getScenarioDisplayText()
        val selectedScenarios = onboardingPreferences?.getSelectedLearningContexts() ?: emptyList()
        val reminderEnabled = onboardingPreferences?.isReminderEnabled() ?: false
        val reminderHour = onboardingPreferences?.getReminderHour() ?: 19
        val reminderMinute = onboardingPreferences?.getReminderMinute() ?: 0
        val reminderTimeStr = String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute)

        _uiState.update {
            it.copy(
                displayName = settingsPreferences.getDisplayName(),
                avatarEmoji = settingsPreferences.getAvatarEmoji(),
                hapticFeedbackEnabled = settingsPreferences.isHapticFeedbackEnabled(),
                targetLevel = userLevel,
                todayCompletedCount = practiceCount,
                dailyTarget = currentDailyTarget,
                remainingFreeQuestions = remaining,
                versionName = versionName,
                isPremium = isPremium,
                currentEntitlement = entitlement,
                dailyTargetPolicy = policy,
                learningScenariosText = scenariosText,
                selectedScenarioIds = selectedScenarios,
                isReminderEnabled = reminderEnabled,
                reminderTimeText = reminderTimeStr
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

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = learningPreferences.getDailyPracticeDate()
        val practiceCount = if (savedDate == todayStr) learningPreferences.getDailyPracticeCount() else 0

        if (target == DailyTargetPolicy.UNLIMITED_TARGET || target > practiceCount) {
            learningPreferences.setLearningCompletedDate("")
            learningPreferences.setQuizCompletedDate("")
        }

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

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        settingsPreferences.setHapticFeedbackEnabled(enabled)
        _uiState.update { it.copy(hapticFeedbackEnabled = enabled) }
    }

    fun setEditProfileSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showEditProfileSheet = visible) }
    }

    fun setLearningScenariosSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showLearningScenariosSheet = visible) }
    }

    fun toggleScenario(id: String) {
        val updated = onboardingPreferences?.toggleScenario(id) ?: return
        _uiState.update {
            it.copy(
                selectedScenarioIds = updated,
                learningScenariosText = onboardingPreferences.getScenarioDisplayText()
            )
        }
    }

    fun moveScenarioUp(id: String) {
        val updated = onboardingPreferences?.moveScenarioUp(id) ?: return
        _uiState.update {
            it.copy(
                selectedScenarioIds = updated,
                learningScenariosText = onboardingPreferences.getScenarioDisplayText()
            )
        }
    }

    fun moveScenarioDown(id: String) {
        val updated = onboardingPreferences?.moveScenarioDown(id) ?: return
        _uiState.update {
            it.copy(
                selectedScenarioIds = updated,
                learningScenariosText = onboardingPreferences.getScenarioDisplayText()
            )
        }
    }

    fun setReminderTimeDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showReminderTimeDialog = visible) }
    }

    fun updateReminderTime(
        hour: Int,
        minute: Int,
        onScheduleAlarm: ((Int, Int) -> Unit)? = null
    ) {
        onboardingPreferences?.setReminderTime(hour, minute)
        if (_uiState.value.isReminderEnabled) {
            onScheduleAlarm?.invoke(hour, minute)
        }
        setReminderTimeDialogVisible(false)
        refresh()
    }

    fun toggleReminder(
        enabled: Boolean,
        onScheduleAlarm: ((Int, Int) -> Unit)? = null,
        onCancelAlarm: (() -> Unit)? = null
    ) {
        onboardingPreferences?.setReminderEnabled(enabled)
        if (enabled) {
            val hour = onboardingPreferences?.getReminderHour() ?: 19
            val minute = onboardingPreferences?.getReminderMinute() ?: 0
            onScheduleAlarm?.invoke(hour, minute)
        } else {
            onCancelAlarm?.invoke()
        }
        refresh()
    }
}
