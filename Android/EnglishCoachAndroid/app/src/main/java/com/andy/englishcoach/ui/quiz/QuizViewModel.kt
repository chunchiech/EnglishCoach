package com.andy.englishcoach.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.model.quiz.QuizResult
import com.andy.englishcoach.data.model.quiz.QuizUiState
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.util.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing the Quiz interaction, option selection, session progress,
 * and result generation.
 */
class QuizViewModel(
    private val repository: QuizRepository,
    private val ttsManager: TtsManager? = null,
    private val settingsPreferences: com.andy.englishcoach.data.preference.SettingsPreferences? = null,
    private val entitlementProvider: com.andy.englishcoach.billing.PremiumEntitlementProvider = com.andy.englishcoach.billing.DefaultBillingRepository(),
    private val preferences: com.andy.englishcoach.data.preference.DailyLearningPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        startQuiz()
    }

    fun startQuiz(target: ToeicTarget? = null) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val selectedTarget = target ?: repository.getUserTargetLevel()
            val isPremium = entitlementProvider.isPremium
            val policy = com.andy.englishcoach.billing.DailyTargetPolicy.forIsPremium(isPremium)
            val currentTarget = policy.coerceTarget(settingsPreferences?.getDailyTarget() ?: 10)
            val effectiveLimit = if (currentTarget == com.andy.englishcoach.billing.DailyTargetPolicy.UNLIMITED_TARGET) {
                com.andy.englishcoach.data.repository.DailyLearningRepository.UNLIMITED_BATCH_SIZE
            } else {
                currentTarget
            }

            val today = repository.getTodayDateString()
            val practiceDate = preferences?.getDailyPracticeDate()
            val todayCompleted = if (practiceDate == today) (preferences?.getDailyPracticeCount() ?: 0) else 0

            val questions = withContext(Dispatchers.IO) {
                repository.generateQuizForSession(
                    target = selectedTarget,
                    totalTarget = effectiveLimit,
                    offset = todayCompleted
                )
            }

            _uiState.update {
                it.copy(
                    target = selectedTarget,
                    questions = questions,
                    currentIndex = 0,
                    selectedOption = null,
                    isAnswered = false,
                    answers = emptyList(),
                    isLoading = false,
                    isSessionCompleted = false,
                    result = null
                )
            }

            if (questions.isNotEmpty()) {
                ttsManager?.speak(questions[0].word.word)
            }
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.isAnswered || state.currentQuestion == null) return

        val currentQuestion = state.currentQuestion!!
        val isCorrect = option == currentQuestion.correctOption
        val answer = QuizAnswer(
            questionIndex = state.currentIndex,
            word = currentQuestion.word,
            chosenOption = option,
            isCorrect = isCorrect
        )

        _uiState.update {
            it.copy(
                selectedOption = option,
                isAnswered = true,
                answers = it.answers + answer
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (!state.isAnswered) return

        if (state.currentIndex < state.questions.size - 1) {
            val nextIndex = state.currentIndex + 1
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    selectedOption = null,
                    isAnswered = false
                )
            }
            ttsManager?.speak(state.questions[nextIndex].word.word)
        } else {
            // Reached end of quiz: commit the session atomically
            val answers = state.answers
            val total = state.questions.size
            val correct = answers.count { it.isCorrect }
            val wrong = total - correct
            val scorePercent = if (total == 0) 0 else ((correct.toDouble() / total.toDouble()) * 100).toInt()

            val finalResult = QuizResult(
                totalQuestions = total,
                correctCount = correct,
                wrongCount = wrong,
                scorePercentage = scorePercent,
                answers = answers
            )

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.commitQuizSession(answers)
                }

                _uiState.update {
                    it.copy(
                        isSessionCompleted = true,
                        result = finalResult
                    )
                }
            }
        }
    }

    fun speakWord(text: String) {
        ttsManager?.speak(text)
    }

    fun speakSentence(text: String) {
        ttsManager?.speak(text)
    }

    class Factory(
        private val repository: QuizRepository,
        private val ttsManager: TtsManager? = null,
        private val settingsPreferences: com.andy.englishcoach.data.preference.SettingsPreferences? = null,
        private val entitlementProvider: com.andy.englishcoach.billing.PremiumEntitlementProvider = com.andy.englishcoach.billing.DefaultBillingRepository(),
        private val preferences: com.andy.englishcoach.data.preference.DailyLearningPreferences? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuizViewModel(repository, ttsManager, settingsPreferences, entitlementProvider, preferences) as T
        }
    }
}
