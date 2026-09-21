package com.andy.englishcoach.ui.onboarding

import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.onboarding.model.LearningScenario
import com.andy.englishcoach.onboarding.model.PlacementQuestion
import com.andy.englishcoach.onboarding.model.ReminderTimeOption

enum class OnboardingStep {
    REMINDER,
    ASSESSMENT_INTRO,
    ASSESSMENT_QUIZ,
    ASSESSMENT_RESULT,
    TARGET_SELECTION,
    LEARNING_CONTEXT
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.REMINDER,
    // Step 1: Reminder
    val selectedReminderOptionId: String = ReminderTimeOption.DEFAULT_SELECTED_OPTION_ID,
    val isReminderProcessing: Boolean = false,
    val isReminderEnabled: Boolean = false,

    // Step 2: Assessment
    val questions: List<PlacementQuestion> = PlacementQuestion.TEST_QUESTIONS,
    val currentQuestionIndex: Int = 0,
    val selectedOption: String? = null,
    val correctCount: Int = 0,
    val isAssessmentFinished: Boolean = false,
    val recommendedTarget: ToeicTarget = ToeicTarget.BASIC,
    val hasCompletedAssessmentQuiz: Boolean = false,

    // Step 3: Target Selection
    val selectedTarget: ToeicTarget = ToeicTarget.BASIC,

    // Step 4: Learning Contexts
    val availableScenarios: List<LearningScenario> = LearningScenario.AVAILABLE_SCENARIOS,
    val selectedScenarioIds: List<String> = LearningScenario.DEFAULT_SELECTED_SCENARIO_IDS
) {
    val currentQuestion: PlacementQuestion?
        get() = questions.getOrNull(currentQuestionIndex)

    val progressPercent: Int
        get() = if (questions.isNotEmpty()) {
            ((currentQuestionIndex + 1) * 100) / questions.size
        } else 0
}
