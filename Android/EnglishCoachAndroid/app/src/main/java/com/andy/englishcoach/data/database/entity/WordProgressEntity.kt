package com.andy.englishcoach.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents the learning progress and SM-2 spaced repetition state for a vocabulary word.
 * Corresponds to learning state fields in EnglishCoach iOS DatabaseManager.
 */
@Entity(
    tableName = "word_progress",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyEntity::class,
            parentColumns = ["word"],
            childColumns = ["word"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["word"], unique = true),
        Index(value = ["next_review_date"]),
        Index(value = ["learned"])
    ]
)
data class WordProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "learned")
    val learned: Boolean = false,

    @ColumnInfo(name = "learned_date")
    val learnedDate: String? = null,

    @ColumnInfo(name = "correct_count")
    val correctCount: Int = 0,

    @ColumnInfo(name = "wrong_count")
    val wrongCount: Int = 0,

    @ColumnInfo(name = "easiness_factor")
    val easinessFactor: Double = 2.5,

    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 0,

    @ColumnInfo(name = "repetition_count")
    val repetitionCount: Int = 0,

    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: String? = null
)
