package com.andy.englishcoach.data.model.review

import com.andy.englishcoach.data.model.Word

/**
 * Model representing a word due for review in ReviewCenterScreen.
 * Includes progress information such as wrongCount and nextReviewDate.
 */
data class ReviewWordItem(
    val word: Word,
    val wrongCount: Int = 0,
    val nextReviewDate: String? = null,
    val learned: Boolean = true
)
