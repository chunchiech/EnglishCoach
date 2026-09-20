package com.andy.englishcoach.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andy.englishcoach.data.database.entity.VocabularyEntity

/**
 * Data Access Object for vocabulary words in EnglishCoach.
 * Target isolation is strictly governed by 'level' rather than 'difficulty'.
 */
@Dao
interface VocabularyDao {

    @Query("SELECT * FROM words ORDER BY id ASC")
    fun getAllWords(): List<VocabularyEntity>

    @Query("SELECT * FROM words WHERE word = :word LIMIT 1")
    fun getWordByWord(word: String): VocabularyEntity?

    @Query("SELECT * FROM words WHERE level = :level ORDER BY id ASC")
    fun getWordsByLevel(level: String): List<VocabularyEntity>

    @Query("SELECT COUNT(*) FROM words")
    fun countWords(): Int

    @Query("SELECT COUNT(*) FROM words WHERE level = :level")
    fun countWordsByLevel(level: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(words: List<VocabularyEntity>)

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    fun getWordsByIds(ids: List<Long>): List<VocabularyEntity>

    @Query("SELECT * FROM words WHERE word IN (:words)")
    fun getWordsByWords(words: List<String>): List<VocabularyEntity>

    @Query("""
        SELECT words.* FROM words
        INNER JOIN word_progress ON words.word = word_progress.word
        WHERE word_progress.learned = 1
          AND word_progress.next_review_date <= :date
          AND words.level = :level
        ORDER BY word_progress.next_review_date ASC, word_progress.wrong_count DESC
    """)
    fun getDueReviewWords(date: String, level: String): List<VocabularyEntity>

    @Query("""
        SELECT words.* FROM words
        INNER JOIN word_progress ON words.word = word_progress.word
        WHERE word_progress.wrong_count > 0
           OR (word_progress.learned = 1 AND word_progress.next_review_date <= :date)
        ORDER BY word_progress.wrong_count DESC, word_progress.next_review_date ASC
    """)
    fun getAllReviewWords(date: String): List<VocabularyEntity>

    @Query("""
        SELECT words.* FROM words
        INNER JOIN word_progress ON words.word = word_progress.word
        WHERE (word_progress.wrong_count > 0 OR (word_progress.learned = 1 AND word_progress.next_review_date <= :date))
          AND words.level = :level
        ORDER BY word_progress.wrong_count DESC, word_progress.next_review_date ASC
    """)
    fun getReviewWordsByLevel(date: String, level: String): List<VocabularyEntity>

    @Query("""
        SELECT COUNT(*) FROM words
        INNER JOIN word_progress ON words.word = word_progress.word
        WHERE word_progress.wrong_count > 0
           OR (word_progress.learned = 1 AND word_progress.next_review_date <= :date)
    """)
    fun getReviewCount(date: String): Int

    @Query("""
        SELECT COUNT(*) FROM words
        INNER JOIN word_progress ON words.word = word_progress.word
        WHERE (word_progress.wrong_count > 0 OR (word_progress.learned = 1 AND word_progress.next_review_date <= :date))
          AND words.level = :level
    """)
    fun getReviewCountByLevel(date: String, level: String): Int


    @Query("""
        SELECT words.* FROM words
        LEFT JOIN word_progress ON words.word = word_progress.word
        WHERE (word_progress.learned IS NULL OR word_progress.learned = 0)
          AND words.level = :level
        ORDER BY words.id ASC
    """)
    fun getUnlearnedWords(level: String): List<VocabularyEntity>

    @Query("SELECT DISTINCT translation FROM words WHERE translation != :correctTranslation AND level = :level ORDER BY RANDOM() LIMIT :count")
    fun getRandomDistractorsByLevel(correctTranslation: String, level: String, count: Int = 3): List<String>

    @Query("SELECT DISTINCT translation FROM words WHERE translation != :correctTranslation ORDER BY RANDOM() LIMIT :count")
    fun getRandomDistractors(correctTranslation: String, count: Int = 3): List<String>

    @Query("DELETE FROM words")
    fun clearAll()
}
