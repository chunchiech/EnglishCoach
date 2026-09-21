package com.andy.englishcoach.data.model

/**
 * Domain model representing a daily learning session.
 */
data class DailyLearningSession(
    val target: ToeicTarget,
    val date: String,
    val words: List<Word>,
    val isCompleted: Boolean
)

/**
 * UI State for the Daily Learning screen.
 */
data class DailyLearningUiState(
    val target: ToeicTarget = ToeicTarget.BASIC,
    val words: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false,
    val isTierCompleted: Boolean = false,
    val isPremium: Boolean = false
) {
    val currentWord: Word?
        get() = if (currentIndex in words.indices) words[currentIndex] else null

    val totalWords: Int
        get() = words.size

    val isFirstCard: Boolean
        get() = currentIndex == 0

    val isLastCard: Boolean
        get() = words.isNotEmpty() && currentIndex == words.size - 1

    val progressFraction: Float
        get() = if (words.isEmpty()) 0f else (currentIndex + 1).toFloat() / words.size.toFloat()

    val completedPercentage: Int
        get() = if (words.isEmpty()) 0 else ((currentIndex.toFloat() / words.size.toFloat()) * 100).toInt()
}
