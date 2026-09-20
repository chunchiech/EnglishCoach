package com.andy.englishcoach.data.repository

import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.model.quiz.QuizQuestion
import com.andy.englishcoach.data.model.review.ReviewWordItem
import com.andy.englishcoach.data.preference.DailyLearningPreferences
import com.andy.englishcoach.data.sm2.SM2Engine
import com.andy.englishcoach.data.sm2.SM2State
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Random

/**
 * Repository for EnglishCoach Smart Review Center ("智慧複習中心") and Review Quiz ("複習測驗").
 * Source of Truth: iOS ReviewView.swift and DatabaseManager.swift.
 *
 * Core query condition:
 * wrong_count > 0 OR (learned = 1 AND next_review_date <= today)
 * Order:
 * wrong_count DESC, next_review_date ASC
 *
 * Quota rule:
 * Browsing Review Center words does NOT consume daily practice quota.
 * Starting / committing Review Quiz questions consumes daily quota.
 */
class ReviewRepository(
    private val database: EnglishCoachDatabase,
    private val preferences: DailyLearningPreferences,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    companion object {
        const val DEFAULT_MAX_REVIEW_QUESTIONS = 10
        const val DISTRACTORS_PER_QUESTION = 3
        const val MAX_FREE_DAILY_QUESTIONS = 10
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    fun getTodayDateString(): String {
        return LocalDate.now(clock).format(DATE_FORMATTER)
    }

    fun getUserTargetLevel(): ToeicTarget {
        return ToeicTarget.from(preferences.getUserLevel())
    }

    /**
     * Retrieves review words based on:
     * wrong_count > 0 OR (learned = 1 AND next_review_date <= date)
     */
    fun getReviewWords(
        date: String = getTodayDateString(),
        targetLevel: ToeicTarget? = null
    ): List<VocabularyEntity> {
        return if (targetLevel != null) {
            database.vocabularyDao().getReviewWordsByLevel(date, targetLevel.rawLevel)
        } else {
            database.vocabularyDao().getAllReviewWords(date)
        }
    }

    fun getReviewWords(): List<VocabularyEntity> = getReviewWords(getTodayDateString(), null)

    fun getReviewWordsByLevel(
        date: String = getTodayDateString(),
        level: String
    ): List<VocabularyEntity> {
        return database.vocabularyDao().getReviewWordsByLevel(date, level)
    }

    fun getReviewWordsByLevel(level: String): List<VocabularyEntity> {
        return getReviewWordsByLevel(getTodayDateString(), level)
    }

    fun getReviewCount(
        date: String = getTodayDateString(),
        targetLevel: ToeicTarget? = null
    ): Int {
        return if (targetLevel != null) {
            database.vocabularyDao().getReviewCountByLevel(date, targetLevel.rawLevel)
        } else {
            database.vocabularyDao().getReviewCount(date)
        }
    }

    fun getReviewCount(): Int = getReviewCount(getTodayDateString(), null)

    /**
     * Retrieves review words enriched with learning progress info (wrongCount, nextReviewDate, learned).
     * Browsing does NOT consume quota.
     */
    fun getReviewWordItems(
        date: String = getTodayDateString(),
        targetLevel: ToeicTarget? = null
    ): List<ReviewWordItem> {
        val entities = getReviewWords(date, targetLevel)
        if (entities.isEmpty()) return emptyList()

        val progresses = database.wordProgressDao()
            .getProgresses(entities.map { it.word })
            .associateBy { it.word }

        return entities.map { entity ->
            val progress = progresses[entity.word]
            ReviewWordItem(
                word = entity.toWord(),
                wrongCount = progress?.wrongCount ?: 0,
                nextReviewDate = progress?.nextReviewDate,
                learned = progress?.learned ?: false
            )
        }
    }

    /**
     * Remaining practice quota for the given date.
     */
    fun getRemainingQuota(date: String = getTodayDateString()): Int {
        return preferences.getRemainingPracticeQuota(date, MAX_FREE_DAILY_QUESTIONS)
    }

    /**
     * Checks whether daily practice quota limit is reached.
     */
    fun isDailyLimitReached(date: String = getTodayDateString()): Boolean {
        return getRemainingQuota(date) <= 0
    }

    /**
     * Generates a Review Quiz session with 4-choice questions strictly from due review words.
     * Enforces target isolation by drawing distractors from the same target level.
     * Respects practice quota limit.
     */
    fun generateReviewQuiz(
        date: String = getTodayDateString(),
        targetLevel: ToeicTarget? = null,
        limit: Int = DEFAULT_MAX_REVIEW_QUESTIONS,
        randomSeed: Long? = null,
        enforceQuota: Boolean = true
    ): List<QuizQuestion> {
        val reviewWords = getReviewWords(date, targetLevel)
        if (reviewWords.isEmpty()) {
            return emptyList()
        }

        val remainingQuota = if (enforceQuota) getRemainingQuota(date) else limit
        val effectiveLimit = minOf(limit, minOf(reviewWords.size, remainingQuota))
        if (effectiveLimit <= 0) {
            return emptyList()
        }

        // Shuffle candidate review words
        val selectedWords = if (randomSeed != null) {
            reviewWords.shuffled(kotlin.random.Random(randomSeed)).take(effectiveLimit)
        } else {
            reviewWords.shuffled().take(effectiveLimit)
        }

        val questions = mutableListOf<QuizQuestion>()

        for ((index, entity) in selectedWords.withIndex()) {
            val word = entity.toWord()
            val correctOption = word.translation

            // Target isolation: draw distractors from the word's own target level
            val levelDistractors = database.vocabularyDao()
                .getRandomDistractorsByLevel(correctOption, word.level, DISTRACTORS_PER_QUESTION)
                .toMutableList()

            // Fallback across all levels if needed
            if (levelDistractors.size < DISTRACTORS_PER_QUESTION) {
                val needed = DISTRACTORS_PER_QUESTION - levelDistractors.size
                val globalDistractors = database.vocabularyDao()
                    .getRandomDistractors(correctOption, needed)
                    .filter { it !in levelDistractors }
                levelDistractors.addAll(globalDistractors)
            }

            // Fill placeholders if DB lacks 4 distinct translations
            while (levelDistractors.size < DISTRACTORS_PER_QUESTION) {
                levelDistractors.add("備選答案 ${levelDistractors.size + 1}")
            }

            val optionsList = (levelDistractors.take(DISTRACTORS_PER_QUESTION) + correctOption).toMutableList()
            if (randomSeed != null) {
                optionsList.shuffle(kotlin.random.Random(randomSeed + index))
            } else {
                optionsList.shuffle()
            }

            questions.add(
                QuizQuestion(
                    index = index,
                    word = word,
                    options = optionsList,
                    correctOption = correctOption
                )
            )
        }

        return questions
    }

    /**
     * Atomically commits the completed Review Quiz session to Room within a database transaction.
     * Session safety rule:
     * - Only called when the full quiz session completes.
     * - Correct answer (q=4): correctCount + 1, wrongCount = max(0, wrongCount - 1), updates SM-2.
     * - Wrong answer (q=1): wrongCount + 1, updates SM-2.
     * - Records practice quota.
     */
    fun commitReviewQuizSession(answers: List<QuizAnswer>): Boolean {
        if (answers.isEmpty()) return false

        val today = getTodayDateString()
        val reviewDate = LocalDate.parse(today)

        database.runInTransaction {
            for (answer in answers) {
                val wordText = answer.word.word
                val existing = database.wordProgressDao().getProgress(wordText)

                val currentState = if (existing != null) {
                    SM2State(
                        easinessFactor = existing.easinessFactor,
                        intervalDays = existing.intervalDays,
                        repetitionCount = existing.repetitionCount,
                        nextReviewDate = existing.nextReviewDate
                    )
                } else {
                    SM2State.INITIAL
                }

                val sm2Result = SM2Engine.calculate(
                    currentState = currentState,
                    isCorrect = answer.isCorrect,
                    reviewDate = reviewDate
                )

                val updatedWrongCount = if (answer.isCorrect) {
                    maxOf(0, (existing?.wrongCount ?: 0) - 1)
                } else {
                    (existing?.wrongCount ?: 0) + 1
                }

                val updatedCorrectCount = (existing?.correctCount ?: 0) + (if (answer.isCorrect) 1 else 0)

                val updatedEntity = WordProgressEntity(
                    word = wordText,
                    learned = true,
                    learnedDate = existing?.learnedDate ?: today,
                    correctCount = updatedCorrectCount,
                    wrongCount = updatedWrongCount,
                    easinessFactor = sm2Result.easinessFactor,
                    intervalDays = sm2Result.intervalDays,
                    repetitionCount = sm2Result.repetitionCount,
                    nextReviewDate = sm2Result.nextReviewDate
                )

                database.wordProgressDao().insertOrUpdate(updatedEntity)
            }

            preferences.recordPracticeQuestions(today, answers.size)
        }

        return true
    }
}
