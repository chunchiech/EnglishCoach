package com.andy.englishcoach.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.onboarding.data.OnboardingPreferences
import com.andy.englishcoach.onboarding.model.AssessmentScoringPolicy
import com.andy.englishcoach.onboarding.model.LearningScenario
import com.andy.englishcoach.onboarding.model.PlacementQuestion
import com.andy.englishcoach.onboarding.model.ReminderTimeOption
import com.andy.englishcoach.util.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel coordinating the 5-step onboarding lifecycle:
 * Step 1: Daily Reminder Setting
 * Step 2: 20-Question Level Assessment (or skip to default)
 * Step 3: Target TOEIC Goal Selection
 * Step 4: Learning Context Selection (Priority ordered)
 * Step 5: Onboarding Completion & State Persistence
 *
 * CRITICAL ISOLATION GUARANTEES:
 * - Does NOT mutate QuizRepository, DailyPractice quota, or Review records.
 * - Does NOT alter SM-2 algorithm variables.
 */
class OnboardingViewModel(
    private val onboardingPreferences: OnboardingPreferences,
    private val learningRepository: DailyLearningRepository,
    private val ttsManager: TtsManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            selectedScenarioIds = onboardingPreferences.getSelectedLearningContexts(),
            selectedReminderOptionId = findOptionIdForTime(
                onboardingPreferences.getReminderHour(),
                onboardingPreferences.getReminderMinute()
            ),
            isReminderEnabled = onboardingPreferences.isReminderEnabled()
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectReminderOption(optionId: String) {
        _uiState.value = _uiState.value.copy(selectedReminderOptionId = optionId)
    }

    fun enableReminder(
        onScheduleAlarm: ((hour: Int, minute: Int) -> Unit)? = null,
        onProceed: () -> Unit
    ) {
        val selected = ReminderTimeOption.PRESET_OPTIONS.firstOrNull {
            it.id == _uiState.value.selectedReminderOptionId
        } ?: ReminderTimeOption.PRESET_OPTIONS[2]

        _uiState.value = _uiState.value.copy(isReminderProcessing = true)

        onboardingPreferences.setReminderEnabled(true)
        onboardingPreferences.setReminderTime(selected.hour, selected.minute)
        onScheduleAlarm?.invoke(selected.hour, selected.minute)

        _uiState.value = _uiState.value.copy(
            isReminderProcessing = false,
            isReminderEnabled = true,
            currentStep = OnboardingStep.ASSESSMENT_INTRO
        )
        onProceed()
    }

    fun skipReminder(
        onCancelAlarm: (() -> Unit)? = null,
        onProceed: () -> Unit
    ) {
        onboardingPreferences.setReminderEnabled(false)
        onCancelAlarm?.invoke()

        _uiState.value = _uiState.value.copy(
            isReminderEnabled = false,
            currentStep = OnboardingStep.ASSESSMENT_INTRO
        )
        onProceed()
    }

    fun startAssessment() {
        _uiState.value = _uiState.value.copy(
            currentStep = OnboardingStep.ASSESSMENT_QUIZ,
            currentQuestionIndex = 0,
            selectedOption = null,
            correctCount = 0,
            isAssessmentFinished = false
        )
    }

    fun skipAssessmentToDefaultTarget() {
        val defaultTarget = ToeicTarget.BASIC
        onboardingPreferences.setRecommendedTargetLevel(defaultTarget)
        _uiState.value = _uiState.value.copy(
            currentStep = OnboardingStep.TARGET_SELECTION,
            hasCompletedAssessmentQuiz = false,
            recommendedTarget = defaultTarget,
            selectedTarget = defaultTarget
        )
    }

    fun answerAssessmentQuestion(option: String) {
        val current = _uiState.value
        if (current.selectedOption != null) return // Already answered

        val question = current.currentQuestion ?: return
        val isCorrect = option == question.correctAnswer
        val newCorrectCount = if (isCorrect) current.correctCount + 1 else current.correctCount

        _uiState.value = current.copy(
            selectedOption = option,
            correctCount = newCorrectCount
        )

        viewModelScope.launch {
            delay(350)
            advanceAssessmentQuestion(newCorrectCount)
        }
    }

    private fun advanceAssessmentQuestion(newCorrectCount: Int) {
        val current = _uiState.value
        val nextIndex = current.currentQuestionIndex + 1

        if (nextIndex < current.questions.size) {
            _uiState.value = current.copy(
                currentQuestionIndex = nextIndex,
                selectedOption = null,
                correctCount = newCorrectCount
            )
        } else {
            // Assessment Finished
            val recommended = AssessmentScoringPolicy.calculateRecommendedLevel(newCorrectCount)
            onboardingPreferences.setAssessmentScore(newCorrectCount)
            onboardingPreferences.setRecommendedTargetLevel(recommended)

            _uiState.value = current.copy(
                currentStep = OnboardingStep.ASSESSMENT_RESULT,
                isAssessmentFinished = true,
                hasCompletedAssessmentQuiz = true,
                correctCount = newCorrectCount,
                recommendedTarget = recommended,
                selectedTarget = recommended
            )
        }
    }

    fun proceedFromAssessmentResult() {
        _uiState.value = _uiState.value.copy(
            currentStep = OnboardingStep.TARGET_SELECTION
        )
    }

    fun selectTarget(target: ToeicTarget) {
        _uiState.value = _uiState.value.copy(selectedTarget = target)
    }

    fun proceedToLearningContext() {
        _uiState.value = _uiState.value.copy(
            currentStep = OnboardingStep.LEARNING_CONTEXT
        )
    }

    fun toggleScenario(id: String) {
        val currentList = _uiState.value.selectedScenarioIds.toMutableList()
        val index = currentList.indexOf(id)
        if (index >= 0) {
            // Keep at least one scenario
            if (currentList.size > 1) {
                currentList.removeAt(index)
            }
        } else {
            currentList.add(id)
        }
        _uiState.value = _uiState.value.copy(selectedScenarioIds = currentList)
        onboardingPreferences.setSelectedLearningContexts(currentList)
    }

    fun moveScenarioUp(id: String) {
        val currentList = _uiState.value.selectedScenarioIds.toMutableList()
        val index = currentList.indexOf(id)
        if (index > 0) {
            val temp = currentList[index]
            currentList[index] = currentList[index - 1]
            currentList[index - 1] = temp
            _uiState.value = _uiState.value.copy(selectedScenarioIds = currentList)
            onboardingPreferences.setSelectedLearningContexts(currentList)
        }
    }

    fun moveScenarioDown(id: String) {
        val currentList = _uiState.value.selectedScenarioIds.toMutableList()
        val index = currentList.indexOf(id)
        if (index >= 0 && index < currentList.size - 1) {
            val temp = currentList[index]
            currentList[index] = currentList[index + 1]
            currentList[index + 1] = temp
            _uiState.value = _uiState.value.copy(selectedScenarioIds = currentList)
            onboardingPreferences.setSelectedLearningContexts(currentList)
        }
    }

    fun completeOnboarding(onFinish: () -> Unit) {
        val target = _uiState.value.selectedTarget
        val scenarios = _uiState.value.selectedScenarioIds

        // 1. Persist Target Level into DailyLearningRepository / Preferences
        learningRepository.setUserTargetLevel(target)

        // 2. Persist Ordered Scenarios & Completion state
        onboardingPreferences.setSelectedLearningContexts(scenarios)
        onboardingPreferences.setOnboardingCompleted(true)

        // 3. Callback to navigate to Dashboard
        onFinish()
    }

    fun handleBackPress(): Boolean {
        val current = _uiState.value
        return when (current.currentStep) {
            OnboardingStep.REMINDER -> false // Let system exit
            OnboardingStep.ASSESSMENT_INTRO -> {
                _uiState.value = current.copy(currentStep = OnboardingStep.REMINDER)
                true
            }
            OnboardingStep.ASSESSMENT_QUIZ -> {
                // Cancel quiz back to intro
                _uiState.value = current.copy(
                    currentStep = OnboardingStep.ASSESSMENT_INTRO,
                    selectedOption = null,
                    currentQuestionIndex = 0,
                    correctCount = 0
                )
                true
            }
            OnboardingStep.ASSESSMENT_RESULT -> {
                _uiState.value = current.copy(currentStep = OnboardingStep.ASSESSMENT_INTRO)
                true
            }
            OnboardingStep.TARGET_SELECTION -> {
                val previous = if (current.hasCompletedAssessmentQuiz) {
                    OnboardingStep.ASSESSMENT_RESULT
                } else {
                    OnboardingStep.ASSESSMENT_INTRO
                }
                _uiState.value = current.copy(currentStep = previous)
                true
            }
            OnboardingStep.LEARNING_CONTEXT -> {
                _uiState.value = current.copy(currentStep = OnboardingStep.TARGET_SELECTION)
                true
            }
        }
    }

    fun speakWord(word: String) {
        ttsManager?.speak(word)
    }

    private fun findOptionIdForTime(hour: Int, minute: Int): String {
        return ReminderTimeOption.PRESET_OPTIONS.firstOrNull {
            it.hour == hour && it.minute == minute
        }?.id ?: ReminderTimeOption.DEFAULT_SELECTED_OPTION_ID
    }
}
