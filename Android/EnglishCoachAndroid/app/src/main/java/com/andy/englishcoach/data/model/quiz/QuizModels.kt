package com.andy.englishcoach.data.model.quiz

import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.Word

/**
 * Represents a single question in the Quiz.
 */
data class QuizQuestion(
    val index: Int,
    val word: Word,
    val options: List<String>,
    val correctOption: String
)

/**
 * Record of a user's answer to a quiz question.
 */
data class QuizAnswer(
    val questionIndex: Int,
    val word: Word,
    val chosenOption: String,
    val isCorrect: Boolean
)

/**
 * Final result of a completed quiz session.
 */
data class QuizResult(
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val scorePercentage: Int,
    val answers: List<QuizAnswer>
)

/**
 * Full state representation of an active or completed quiz session.
 */
data class QuizSession(
    val target: ToeicTarget,
    val date: String,
    val questions: List<QuizQuestion>,
    val answers: List<QuizAnswer> = emptyList(),
    val isCompleted: Boolean = false
) {
    val totalQuestions: Int
        get() = questions.size

    val answeredCount: Int
        get() = answers.size

    val correctCount: Int
        get() = answers.count { it.isCorrect }

    val wrongCount: Int
        get() = answers.count { !it.isCorrect }

    val scorePercentage: Int
        get() = if (questions.isEmpty()) 0 else ((correctCount.toDouble() / questions.size.toDouble()) * 100).toInt()

    fun toQuizResult(): QuizResult {
        return QuizResult(
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            wrongCount = wrongCount,
            scorePercentage = scorePercentage,
            answers = answers
        )
    }
}

/**
 * UI State for the Quiz Screen.
 */
data class QuizUiState(
    val target: ToeicTarget = ToeicTarget.BASIC,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOption: String? = null,
    val isAnswered: Boolean = false,
    val answers: List<QuizAnswer> = emptyList(),
    val isLoading: Boolean = true,
    val isSessionCompleted: Boolean = false,
    val result: QuizResult? = null
) {
    val currentQuestion: QuizQuestion?
        get() = if (currentIndex in questions.indices) questions[currentIndex] else null

    val totalQuestions: Int
        get() = questions.size

    val correctAnswersCount: Int
        get() = answers.count { it.isCorrect }

    val wrongAnswersCount: Int
        get() = answers.count { !it.isCorrect }

    val isLastQuestion: Boolean
        get() = questions.isNotEmpty() && currentIndex == questions.size - 1

    val progressFraction: Float
        get() = if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size.toFloat()
}
