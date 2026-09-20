package com.andy.englishcoach.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andy.englishcoach.data.database.entity.WordProgressEntity

/**
 * Data Access Object for word learning progress and SM-2 review scheduling.
 * Follows EnglishCoach iOS review queries and progress accounting.
 */
@Dao
interface WordProgressDao {

    @Query("SELECT * FROM word_progress WHERE word = :word LIMIT 1")
    fun getProgress(word: String): WordProgressEntity?

    @Query("SELECT * FROM word_progress WHERE word IN (:words)")
    fun getProgresses(words: List<String>): List<WordProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(progress: WordProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(progresses: List<WordProgressEntity>)

    @Query("SELECT * FROM word_progress WHERE learned = 1 AND next_review_date <= :date ORDER BY next_review_date ASC")
    fun getWordsDueForReview(date: String): List<WordProgressEntity>

    @Query("SELECT COUNT(*) FROM word_progress WHERE learned = 1 AND next_review_date <= :date")
    fun getReviewCount(date: String): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE learned = 1")
    fun getLearnedCount(): Int

    @Query("SELECT * FROM word_progress WHERE wrong_count > 0 ORDER BY wrong_count DESC")
    fun getWrongWords(): List<WordProgressEntity>

    @Query("""
        UPDATE word_progress
        SET learned = 1,
            learned_date = COALESCE(learned_date, :today),
            next_review_date = COALESCE(next_review_date, :nextReviewDate),
            interval_days = CASE WHEN interval_days = 0 THEN 1 ELSE interval_days END,
            repetition_count = CASE WHEN repetition_count = 0 THEN 1 ELSE repetition_count END
        WHERE word = :word
    """)
    fun updateLearnedFields(word: String, today: String, nextReviewDate: String): Int

    @androidx.room.Transaction
    fun markAsLearned(word: String, today: String, nextReviewDate: String) {
        val rows = updateLearnedFields(word, today, nextReviewDate)
        if (rows == 0) {
            insertOrUpdate(
                WordProgressEntity(
                    word = word,
                    learned = true,
                    learnedDate = today,
                    nextReviewDate = nextReviewDate,
                    intervalDays = 1,
                    repetitionCount = 1,
                    correctCount = 0,
                    wrongCount = 0,
                    easinessFactor = 2.5
                )
            )
        }
    }

    @Query("DELETE FROM word_progress")
    fun clearAll()
}
