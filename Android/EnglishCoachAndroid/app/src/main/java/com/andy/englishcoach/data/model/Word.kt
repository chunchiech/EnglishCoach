package com.andy.englishcoach.data.model

/**
 * Represents a single vocabulary word in EnglishCoach.
 */
data class Word(
    val word: String,
    val phonetic: String,
    val translation: String,
    val example: String,
    val exampleTranslation: String,
    val level: String,
    val difficulty: Int,
    val topic: String,
    val subtopic: String,
    val examTags: String,
    val partOfSpeech: String
) {
    val target: ToeicTarget
        get() = ToeicTarget.from(level)
}
