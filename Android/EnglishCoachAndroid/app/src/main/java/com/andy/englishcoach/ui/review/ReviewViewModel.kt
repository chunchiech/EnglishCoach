package com.andy.englishcoach.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.Word
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.model.quiz.QuizQuestion
import com.andy.englishcoach.data.model.review.ReviewWordItem
import com.andy.englishcoach.data.repository.ReviewRepository
import com.andy.englishcoach.util.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.andy.englishcoach.billing.DefaultBillingRepository
import com.andy.englishcoach.billing.PremiumEntitlementProvider
import com.andy.englishcoach.billing.ReviewQuizGatingPolicy

data class ReviewUiState(
    val isLoading: Boolean = true,
    val reviewWords: List<ReviewWordItem> = emptyList(),
    val selectedWordForDetail: Word? = null,
    val isQuizActive: Boolean = false,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedOption: String? = null,
    val isAnswered: Boolean = false,
    val answers: List<QuizAnswer> = emptyList(),
    val isQuizCompleted: Boolean = false,
    val remainingQuota: Int = 10,
    val isDailyLimitReached: Boolean = false,
    val isPremium: Boolean = false
)

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val ttsManager: TtsManager? = null,
    private val entitlementProvider: PremiumEntitlementProvider = DefaultBillingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadReviewWords()
    }

    /**
     * Loads due review words and updates quota status.
     * Browsing does NOT consume quota.
     */
    fun loadReviewWords(targetLevel: ToeicTarget? = reviewRepository.getUserTargetLevel()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val words = withContext(Dispatchers.IO) {
                reviewRepository.getReviewWordItems(targetLevel = targetLevel)
            }
            val quota = reviewRepository.getRemainingQuota()
            val isPremium = entitlementProvider.isPremium
            val limitReached = if (isPremium) false else reviewRepository.isDailyLimitReached()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    reviewWords = words,
                    remainingQuota = quota,
                    isDailyLimitReached = limitReached,
                    isPremium = isPremium
                )
            }
        }
    }

    fun selectWordForDetail(word: Word?) {
        _uiState.update { it.copy(selectedWordForDetail = word) }
    }

    /**
     * Starts a review quiz session.
     * Generates questions and stores state in-memory only.
     */
    fun startReviewQuiz(targetLevel: ToeicTarget? = reviewRepository.getUserTargetLevel()) {
        val state = _uiState.value
        if (state.reviewWords.isEmpty()) return

        viewModelScope.launch {
            val questions = withContext(Dispatchers.IO) {
                reviewRepository.generateReviewQuiz(
                    targetLevel = targetLevel,
                    limit = 10,
                    enforceQuota = true
                )
            }

            if (questions.isEmpty()) return@launch

            _uiState.update {
                it.copy(
                    isQuizActive = true,
                    quizQuestions = questions,
                    currentQuestionIndex = 0,
                    selectedOption = null,
                    isAnswered = false,
                    answers = emptyList(),
                    isQuizCompleted = false
                )
            }

            // Auto-pronounce first word
            speakWord(questions.first().word.word)
        }
    }

    /**
     * Records answer in-memory. Does NOT commit to database.
     */
    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.isAnswered || state.currentQuestionIndex >= state.quizQuestions.size) return

        val currentQuestion = state.quizQuestions[state.currentQuestionIndex]
        val isCorrect = option == currentQuestion.correctOption

        val answer = QuizAnswer(
            questionIndex = state.currentQuestionIndex,
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

    /**
     * Advances to next question or commits the session upon answering the last question.
     */
    fun nextQuestion() {
        val state = _uiState.value
        if (!state.isAnswered) return

        if (state.currentQuestionIndex < state.quizQuestions.size - 1) {
            val nextIndex = state.currentQuestionIndex + 1
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    selectedOption = null,
                    isAnswered = false
                )
            }
            speakWord(state.quizQuestions[nextIndex].word.word)
        } else {
            // Last question completed: commit atomically to Room database
            commitQuizSession()
        }
    }

    /**
     * Commits all answers to Room in a single transaction.
     */
    private fun commitQuizSession() {
        val answersToCommit = _uiState.value.answers
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                reviewRepository.commitReviewQuizSession(answersToCommit)
            }
            // Reload review words and update quota
            val updatedWords = withContext(Dispatchers.IO) {
                reviewRepository.getReviewWordItems(targetLevel = reviewRepository.getUserTargetLevel())
            }
            val quota = reviewRepository.getRemainingQuota()
            val limitReached = reviewRepository.isDailyLimitReached()

            _uiState.update {
                it.copy(
                    isQuizCompleted = true,
                    reviewWords = updatedWords,
                    remainingQuota = quota,
                    isDailyLimitReached = limitReached
                )
            }
        }
    }

    /**
     * Exits quiz early without committing partial results.
     */
    fun exitQuiz() {
        _uiState.update {
            it.copy(
                isQuizActive = false,
                isQuizCompleted = false,
                quizQuestions = emptyList(),
                answers = emptyList(),
                currentQuestionIndex = 0,
                selectedOption = null,
                isAnswered = false
            )
        }
        loadReviewWords()
    }

    /**
     * Dismisses results screen and returns to review center.
     */
    fun dismissResults() {
        _uiState.update {
            it.copy(
                isQuizActive = false,
                isQuizCompleted = false,
                quizQuestions = emptyList(),
                answers = emptyList(),
                currentQuestionIndex = 0,
                selectedOption = null,
                isAnswered = false
            )
        }
        loadReviewWords()
    }

    fun speakWord(word: String) {
        ttsManager?.speak(word)
    }

    fun speakSentence(sentence: String) {
        ttsManager?.speak(sentence)
    }
}
