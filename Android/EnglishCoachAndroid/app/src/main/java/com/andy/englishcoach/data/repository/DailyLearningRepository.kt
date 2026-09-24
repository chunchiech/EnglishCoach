package com.andy.englishcoach.data.repository

import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.model.DailyLearningSession
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.Word
import com.andy.englishcoach.data.preference.DailyLearningPreferences
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository responsible for the Daily Learning core workflow in EnglishCoach Android.
 *
 * Rules:
 * 1. Target Isolation: Words are strictly filtered by 'level' (toeic_basic, toeic_advanced, toeic_gold).
 *    Never rely on 'difficulty'.
 * 2. Stable Daily Caching: On the same calendar day, restarting or re-querying returns the exact same ordered word set.
 * 3. Cross-day Automatic Refresh: When the calendar date changes, a fresh batch of words is selected.
 * 4. Priority Tiers: Tier 1 = Due reviews (SM-2 scheduled for today or earlier); Tier 2 = Unlearned words in target level.
 * 5. Learning State Tracking: Marking a word as learned persists its progress without premature SM-2 rating recalculation.
 */
class DailyLearningRepository(
    private val database: EnglishCoachDatabase,
    private val preferences: DailyLearningPreferences,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    companion object {
        const val DEFAULT_DAILY_TARGET = 10
        const val UNLIMITED_BATCH_SIZE = 50
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    fun getTodayDateString(): String {
        return LocalDate.now(clock).format(DATE_FORMATTER)
    }

    fun getNextReviewDateString(days: Long = 1): String {
        return LocalDate.now(clock).plusDays(days).format(DATE_FORMATTER)
    }

    fun getUserTargetLevel(): ToeicTarget {
        return ToeicTarget.from(preferences.getUserLevel())
    }

    fun setUserTargetLevel(target: ToeicTarget) {
        preferences.setUserLevel(target.rawLevel)
    }

    fun isTodayLearningCompleted(): Boolean {
        val today = getTodayDateString()
        return preferences.getLearningCompletedDate() == today
    }

    fun markTodayLearningCompleted() {
        val today = getTodayDateString()
        preferences.setLearningCompletedDate(today)
    }

    /**
     * Retrieves the daily learning words for the specified [target] level.
     * Guarantees target isolation, same-day caching, and cross-day renewal.
     */
    fun getTodayWords(
        target: ToeicTarget = getUserTargetLevel(),
        limit: Int = DEFAULT_DAILY_TARGET
    ): List<Word> {
        val resolvedLimit = if (limit <= 0 || limit == com.andy.englishcoach.billing.DailyTargetPolicy.UNLIMITED_TARGET) {
            UNLIMITED_BATCH_SIZE
        } else {
            limit
        }
        val today = getTodayDateString()
        val level = target.rawLevel

        val savedDate = preferences.getTodayWordsDate(level)
        val selectedIds = if (savedDate == today) {
            preferences.getTodayWordIds(level)
        } else {
            emptyList()
        }

        var words: MutableList<VocabularyEntity> = mutableListOf()
        if (selectedIds.isNotEmpty()) {
            val fetched = database.vocabularyDao().getWordsByIds(selectedIds)
            // Ensure target level isolation and maintain cached order
            val dict = fetched.filter { it.level == level }.associateBy { it.id }
            words = selectedIds.mapNotNull { dict[it] }.toMutableList()
        }

        // If cached words are fewer than desired target count, select remaining from DB
        if (words.size < resolvedLimit) {
            val selectedIdSet = words.map { it.id }.toMutableSet()

            // Tier 1: Due Review (SM-2 Spaced Repetition) restricted strictly to targetLevel
            val stillNeededForDue = resolvedLimit - words.size
            val dueWords = database.vocabularyDao().getDueReviewWords(today, level)
                .filter { it.id !in selectedIdSet }

            if (dueWords.isNotEmpty()) {
                val dueToAdd = dueWords.take(stillNeededForDue)
                words.addAll(dueToAdd)
                selectedIdSet.addAll(dueToAdd.map { it.id })
            }

            // Tier 2: Unlearned Pool strictly restricted to targetLevel
            if (words.size < resolvedLimit) {
                val remainingSlot = resolvedLimit - words.size
                val unlearnedCandidates = database.vocabularyDao().getUnlearnedWords(level)
                    .filter { it.id !in selectedIdSet }

                if (unlearnedCandidates.isNotEmpty()) {
                    val toAdd = unlearnedCandidates.take(remainingSlot)
                    words.addAll(toAdd)
                    selectedIdSet.addAll(toAdd.map { it.id })
                }
            }

            // Save the stable daily selected word IDs into preferences
            if (words.isNotEmpty()) {
                preferences.setTodayWordsDate(level, today)
                preferences.setTodayWordIds(level, words.map { it.id })
            }
        }

        if (words.size > resolvedLimit) {
            words = words.take(resolvedLimit).toMutableList()
        }

        return words.map { it.toWord() }
    }

    /**
     * Marks a word as learned when viewed in the Daily Learning flow.
     * Updates learned = 1, sets initial next review date (+1 day), and increments rep/interval to 1 if 0.
     * Preserves SM-2 easiness factor without calculating quality rating.
     */
    fun markWordAsLearned(word: String) {
        val today = getTodayDateString()
        val nextReview = getNextReviewDateString(1)
        database.wordProgressDao().markAsLearned(word, today, nextReview)
    }

    /**
     * Builds a complete session representation.
     */
    fun getLearningSession(
        target: ToeicTarget = getUserTargetLevel(),
        limit: Int = DEFAULT_DAILY_TARGET
    ): DailyLearningSession {
        val todayWords = getTodayWords(target, limit)
        return DailyLearningSession(
            target = target,
            date = getTodayDateString(),
            words = todayWords,
            isCompleted = isTodayLearningCompleted()
        )
    }

    fun clearCache(level: String? = null) {
        preferences.clearDailyCache(level)
    }
}
