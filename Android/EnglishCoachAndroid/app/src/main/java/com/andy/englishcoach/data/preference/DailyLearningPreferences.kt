package com.andy.englishcoach.data.preference

import android.content.Context
import android.content.SharedPreferences

/**
 * Storage abstraction for Daily Learning & Quiz caching, date tracking, and user level preferences.
 * Follows iOS EnglishCoach naming and key schemas.
 */
interface DailyLearningPreferences {
    fun getTodayWordsDate(level: String): String?
    fun setTodayWordsDate(level: String, date: String)
    fun getTodayWordIds(level: String): List<Long>
    fun setTodayWordIds(level: String, ids: List<Long>)
    fun getLearningCompletedDate(): String?
    fun setLearningCompletedDate(date: String)
    fun getQuizCompletedDate(): String?
    fun setQuizCompletedDate(date: String)
    fun getUserLevel(): String
    fun setUserLevel(level: String)
    fun clearDailyCache(level: String? = null)
    fun getDailyPracticeDate(): String?
    fun setDailyPracticeDate(date: String)
    fun getDailyPracticeCount(): Int
    fun setDailyPracticeCount(count: Int)
    fun getRemainingPracticeQuota(date: String, dailyLimit: Int = 10): Int
    fun recordPracticeQuestions(date: String, count: Int)
    fun getDailyQuizDate(): String?
    fun setDailyQuizDate(date: String)
    fun getDailyQuizCount(): Int
    fun setDailyQuizCount(count: Int)
    fun recordQuizQuestions(date: String, count: Int)
}

/**
 * SharedPreferences implementation for Android.
 */
class SharedPreferencesDailyLearningPreferences(
    private val prefs: SharedPreferences
) : DailyLearningPreferences {

    companion object {
        const val PREFS_NAME = "englishcoach_preferences"
        const val KEY_DATE_PREFIX = "englishcoach_today_words_date_"
        const val KEY_IDS_PREFIX = "englishcoach_today_words_ids_"
        const val KEY_LEARNING_COMPLETED_DATE = "today_learning_completed_date"
        const val KEY_QUIZ_COMPLETED_DATE = "today_quiz_completed_date"
        const val KEY_DAILY_PRACTICE_DATE = "daily_practice_date"
        const val KEY_DAILY_PRACTICE_COUNT = "daily_practice_completed_count"
        const val KEY_DAILY_QUIZ_DATE = "daily_quiz_date"
        const val KEY_DAILY_QUIZ_COUNT = "daily_quiz_completed_count"
        const val KEY_USER_LEVEL = "user_level"
        const val DEFAULT_USER_LEVEL = "toeic_basic"
        const val DEFAULT_MAX_FREE_DAILY_QUESTIONS = 10

        fun create(context: Context): SharedPreferencesDailyLearningPreferences {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return SharedPreferencesDailyLearningPreferences(sharedPrefs)
        }
    }

    override fun getTodayWordsDate(level: String): String? {
        return prefs.getString("${KEY_DATE_PREFIX}$level", null)
    }

    override fun setTodayWordsDate(level: String, date: String) {
        prefs.edit().putString("${KEY_DATE_PREFIX}$level", date).apply()
    }

    override fun getTodayWordIds(level: String): List<Long> {
        val raw = prefs.getString("${KEY_IDS_PREFIX}$level", null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
    }

    override fun setTodayWordIds(level: String, ids: List<Long>) {
        val raw = ids.joinToString(",")
        prefs.edit().putString("${KEY_IDS_PREFIX}$level", raw).apply()
    }

    override fun getLearningCompletedDate(): String? {
        return prefs.getString(KEY_LEARNING_COMPLETED_DATE, null)
    }

    override fun setLearningCompletedDate(date: String) {
        prefs.edit().putString(KEY_LEARNING_COMPLETED_DATE, date).apply()
    }

    override fun getQuizCompletedDate(): String? {
        return prefs.getString(KEY_QUIZ_COMPLETED_DATE, null)
    }

    override fun setQuizCompletedDate(date: String) {
        prefs.edit().putString(KEY_QUIZ_COMPLETED_DATE, date).apply()
    }

    override fun getUserLevel(): String {
        return prefs.getString(KEY_USER_LEVEL, DEFAULT_USER_LEVEL) ?: DEFAULT_USER_LEVEL
    }

    override fun setUserLevel(level: String) {
        prefs.edit().putString(KEY_USER_LEVEL, level).apply()
    }

    override fun getDailyPracticeDate(): String? {
        return prefs.getString(KEY_DAILY_PRACTICE_DATE, null)
    }

    override fun setDailyPracticeDate(date: String) {
        prefs.edit().putString(KEY_DAILY_PRACTICE_DATE, date).apply()
    }

    override fun getDailyPracticeCount(): Int {
        return prefs.getInt(KEY_DAILY_PRACTICE_COUNT, 0)
    }

    override fun setDailyPracticeCount(count: Int) {
        prefs.edit().putInt(KEY_DAILY_PRACTICE_COUNT, count).apply()
    }

    override fun getRemainingPracticeQuota(date: String, dailyLimit: Int): Int {
        val savedDate = getDailyPracticeDate()
        if (savedDate != date) {
            return dailyLimit
        }
        val count = getDailyPracticeCount()
        return maxOf(0, dailyLimit - count)
    }

    override fun recordPracticeQuestions(date: String, count: Int) {
        val savedDate = getDailyPracticeDate()
        val currentCount = if (savedDate != date) 0 else getDailyPracticeCount()
        prefs.edit()
            .putString(KEY_DAILY_PRACTICE_DATE, date)
            .putInt(KEY_DAILY_PRACTICE_COUNT, currentCount + count)
            .apply()
    }

    override fun getDailyQuizDate(): String? {
        return prefs.getString(KEY_DAILY_QUIZ_DATE, null)
    }

    override fun setDailyQuizDate(date: String) {
        prefs.edit().putString(KEY_DAILY_QUIZ_DATE, date).apply()
    }

    override fun getDailyQuizCount(): Int {
        return prefs.getInt(KEY_DAILY_QUIZ_COUNT, 0)
    }

    override fun setDailyQuizCount(count: Int) {
        prefs.edit().putInt(KEY_DAILY_QUIZ_COUNT, count).apply()
    }

    override fun recordQuizQuestions(date: String, count: Int) {
        val savedDate = getDailyQuizDate()
        val currentCount = if (savedDate != date) 0 else getDailyQuizCount()
        prefs.edit()
            .putString(KEY_DAILY_QUIZ_DATE, date)
            .putInt(KEY_DAILY_QUIZ_COUNT, currentCount + count)
            .apply()
    }

    override fun clearDailyCache(level: String?) {
        val editor = prefs.edit()
        if (level != null) {
            editor.remove("${KEY_DATE_PREFIX}$level")
            editor.remove("${KEY_IDS_PREFIX}$level")
        } else {
            editor.remove(KEY_LEARNING_COMPLETED_DATE)
            editor.remove(KEY_QUIZ_COMPLETED_DATE)
            editor.remove(KEY_DAILY_PRACTICE_DATE)
            editor.remove(KEY_DAILY_PRACTICE_COUNT)
            editor.remove(KEY_DAILY_QUIZ_DATE)
            editor.remove(KEY_DAILY_QUIZ_COUNT)
            val allKeys = prefs.all.keys
            for (key in allKeys) {
                if (key.startsWith(KEY_DATE_PREFIX) || key.startsWith(KEY_IDS_PREFIX)) {
                    editor.remove(key)
                }
            }
        }
        editor.apply()
    }
}

/**
 * In-memory implementation of preferences for fast, standalone unit testing.
 */
class InMemoryDailyLearningPreferences(
    private var userLevel: String = SharedPreferencesDailyLearningPreferences.DEFAULT_USER_LEVEL
) : DailyLearningPreferences {

    private val dates = mutableMapOf<String, String>()
    private val wordIds = mutableMapOf<String, List<Long>>()
    private var learningCompletedDate: String? = null
    private var quizCompletedDate: String? = null
    private var dailyPracticeDate: String? = null
    private var dailyPracticeCount: Int = 0
    private var dailyQuizDate: String? = null
    private var dailyQuizCount: Int = 0

    override fun getTodayWordsDate(level: String): String? = dates[level]

    override fun setTodayWordsDate(level: String, date: String) {
        dates[level] = date
    }

    override fun getTodayWordIds(level: String): List<Long> = wordIds[level] ?: emptyList()

    override fun setTodayWordIds(level: String, ids: List<Long>) {
        wordIds[level] = ids
    }

    override fun getLearningCompletedDate(): String? = learningCompletedDate

    override fun setLearningCompletedDate(date: String) {
        learningCompletedDate = date
    }

    override fun getQuizCompletedDate(): String? = quizCompletedDate

    override fun setQuizCompletedDate(date: String) {
        quizCompletedDate = date
    }

    override fun getUserLevel(): String = userLevel

    override fun setUserLevel(level: String) {
        userLevel = level
    }

    override fun getDailyPracticeDate(): String? = dailyPracticeDate

    override fun setDailyPracticeDate(date: String) {
        dailyPracticeDate = date
    }

    override fun getDailyPracticeCount(): Int = dailyPracticeCount

    override fun setDailyPracticeCount(count: Int) {
        dailyPracticeCount = count
    }

    override fun getRemainingPracticeQuota(date: String, dailyLimit: Int): Int {
        if (dailyPracticeDate != date) return dailyLimit
        return maxOf(0, dailyLimit - dailyPracticeCount)
    }

    override fun recordPracticeQuestions(date: String, count: Int) {
        if (dailyPracticeDate != date) {
            dailyPracticeDate = date
            dailyPracticeCount = count
        } else {
            dailyPracticeCount += count
        }
    }

    override fun getDailyQuizDate(): String? = dailyQuizDate

    override fun setDailyQuizDate(date: String) {
        dailyQuizDate = date
    }

    override fun getDailyQuizCount(): Int = dailyQuizCount

    override fun setDailyQuizCount(count: Int) {
        dailyQuizCount = count
    }

    override fun recordQuizQuestions(date: String, count: Int) {
        if (dailyQuizDate != date) {
            dailyQuizDate = date
            dailyQuizCount = count
        } else {
            dailyQuizCount += count
        }
    }

    override fun clearDailyCache(level: String?) {
        if (level != null) {
            dates.remove(level)
            wordIds.remove(level)
        } else {
            dates.clear()
            wordIds.clear()
            learningCompletedDate = null
            quizCompletedDate = null
            dailyPracticeDate = null
            dailyPracticeCount = 0
            dailyQuizDate = null
            dailyQuizCount = 0
        }
    }
}
