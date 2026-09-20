package com.andy.englishcoach.data.repository

import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.model.quiz.QuizQuestion
import com.andy.englishcoach.data.preference.DailyLearningPreferences
import com.andy.englishcoach.data.sm2.SM2Engine
import com.andy.englishcoach.data.sm2.SM2State
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Random

/**
 * Repository responsible for Quiz generation, answer evaluation, session commitment,
 * and SM-2 spaced repetition updates following EnglishCoach iOS rules.
 */
class QuizRepository(
    private val database: EnglishCoachDatabase,
    private val preferences: DailyLearningPreferences,
    private val dailyLearningRepository: DailyLearningRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    companion object {
        const val QUIZ_QUESTIONS_COUNT = 10
        const val DISTRACTORS_PER_QUESTION = 3
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    fun getTodayDateString(): String {
        return LocalDate.now(clock).format(DATE_FORMATTER)
    }

    fun getUserTargetLevel(): ToeicTarget {
        return dailyLearningRepository.getUserTargetLevel()
    }

    fun isTodayQuizCompleted(): Boolean {
        val today = getTodayDateString()
        return preferences.getQuizCompletedDate() == today
    }

    fun markTodayQuizCompleted() {
        val today = getTodayDateString()
        preferences.setQuizCompletedDate(today)
    }

    /**
     * Generates a quiz from the today words of the specified target level.
     * Preserves target isolation and creates 4 options per question (1 correct + 3 distractors).
     */
    fun generateQuiz(
        target: ToeicTarget = getUserTargetLevel(),
        limit: Int = QUIZ_QUESTIONS_COUNT,
        randomSeed: Long? = null
    ): List<QuizQuestion> {
        val todayWords = dailyLearningRepository.getTodayWords(target, limit)
        if (todayWords.isEmpty()) {
            return emptyList()
        }

        val random = if (randomSeed != null) Random(randomSeed) else Random()
        val questions = mutableListOf<QuizQuestion>()

        for ((index, word) in todayWords.withIndex()) {
            val correctOption = word.translation

            // Query distractors preferentially from the same target level
            val levelDistractors = database.vocabularyDao()
                .getRandomDistractorsByLevel(correctOption, target.rawLevel, DISTRACTORS_PER_QUESTION)
                .toMutableList()

            // Fallback across all levels if needed (e.g., small custom dataset)
            if (levelDistractors.size < DISTRACTORS_PER_QUESTION) {
                val needed = DISTRACTORS_PER_QUESTION - levelDistractors.size
                val globalDistractors = database.vocabularyDao()
                    .getRandomDistractors(correctOption, needed)
                    .filter { it !in levelDistractors }
                levelDistractors.addAll(globalDistractors)
            }

            // Fill with placeholders if DB is smaller than 4 distinct translations
            while (levelDistractors.size < DISTRACTORS_PER_QUESTION) {
                levelDistractors.add("備選答案 ${levelDistractors.size + 1}")
            }

            val optionsList = (levelDistractors.take(DISTRACTORS_PER_QUESTION) + correctOption).toMutableList()
            // Shuffle options
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
     * Atomically commits the completed quiz session to the Room database.
     * 1. Updates correct/wrong count
     * 2. Runs SM2Engine calculation (q=4 if correct, q=1 if wrong)
     * 3. Updates learned = true, learned_date = today, easiness_factor, interval_days, repetition_count, next_review_date
     * 4. Marks today's quiz as completed
     *
     * Rule: In-memory session tracking during questions. Only written to database upon full completion.
     */
    fun commitQuizSession(results: List<QuizAnswer>): Boolean {
        if (results.isEmpty()) return false

        val today = getTodayDateString()
        val reviewDate = LocalDate.parse(today)

        database.runInTransaction {
            for (answer in results) {
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

                val updatedEntity = WordProgressEntity(
                    word = wordText,
                    learned = true,
                    learnedDate = existing?.learnedDate ?: today,
                    correctCount = (existing?.correctCount ?: 0) + (if (answer.isCorrect) 1 else 0),
                    wrongCount = (existing?.wrongCount ?: 0) + (if (answer.isCorrect) 0 else 1),
                    easinessFactor = sm2Result.easinessFactor,
                    intervalDays = sm2Result.intervalDays,
                    repetitionCount = sm2Result.repetitionCount,
                    nextReviewDate = sm2Result.nextReviewDate
                )

                database.wordProgressDao().insertOrUpdate(updatedEntity)
            }

            markTodayQuizCompleted()
            preferences.recordPracticeQuestions(today, results.size)
        }

        return true
    }
}
