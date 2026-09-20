package com.andy.englishcoach.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.VocabularyDatabaseInitializer
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.InMemoryDailyLearningPreferences
import com.andy.englishcoach.data.preference.SharedPreferencesDailyLearningPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class DailyLearningRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: EnglishCoachDatabase
    private lateinit var preferences: InMemoryDailyLearningPreferences
    private val fixedZone = ZoneId.of("UTC")
    private val day1Clock = Clock.fixed(Instant.parse("2026-09-21T09:00:00Z"), fixedZone)
    private val day2Clock = Clock.fixed(Instant.parse("2026-09-22T09:00:00Z"), fixedZone)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EnglishCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = InMemoryDailyLearningPreferences()

        val possiblePaths = listOf(
            File("src/main/assets/toeic_3600.csv"),
            File("app/src/main/assets/toeic_3600.csv"),
            File("../app/src/main/assets/toeic_3600.csv")
        )
        val csvFile = possiblePaths.firstOrNull { it.exists() }
            ?: throw IllegalStateException("toeic_3600.csv not found")

        FileInputStream(csvFile).use { stream ->
            VocabularyDatabaseInitializer.populateFromStream(db, stream)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testBasicTargetIsolation() {
        val repo = DailyLearningRepository(db, preferences, day1Clock)
        val words = repo.getTodayWords(ToeicTarget.BASIC, limit = 10)

        assertEquals(10, words.size)
        words.forEach { word ->
            assertEquals("toeic_basic", word.level)
            assertEquals(ToeicTarget.BASIC, word.target)
        }
    }

    @Test
    fun testAdvancedTargetIsolation() {
        val repo = DailyLearningRepository(db, preferences, day1Clock)
        val words = repo.getTodayWords(ToeicTarget.ADVANCED, limit = 10)

        assertEquals(10, words.size)
        words.forEach { word ->
            assertEquals("toeic_advanced", word.level)
            assertEquals(ToeicTarget.ADVANCED, word.target)
        }
    }

    @Test
    fun testGoldTargetIsolation() {
        val repo = DailyLearningRepository(db, preferences, day1Clock)
        val words = repo.getTodayWords(ToeicTarget.GOLD, limit = 10)

        assertEquals(10, words.size)
        words.forEach { word ->
            assertEquals("toeic_gold", word.level)
            assertEquals(ToeicTarget.GOLD, word.target)
        }
    }

    @Test
    fun testSameDayCachingStability() {
        val repo = DailyLearningRepository(db, preferences, day1Clock)

        val firstCallWords = repo.getTodayWords(ToeicTarget.BASIC, limit = 10)
        val secondCallWords = repo.getTodayWords(ToeicTarget.BASIC, limit = 10)

        assertEquals(10, firstCallWords.size)
        assertEquals(firstCallWords.size, secondCallWords.size)

        // Strict ordering check
        for (i in firstCallWords.indices) {
            assertEquals(firstCallWords[i].word, secondCallWords[i].word)
            assertEquals(firstCallWords[i].translation, secondCallWords[i].translation)
        }
    }

    @Test
    fun testAppRestartSimulation() {
        val repo1 = DailyLearningRepository(db, preferences, day1Clock)
        val initialWords = repo1.getTodayWords(ToeicTarget.BASIC, limit = 10)

        // Simulate app restart by creating a new repository instance pointing to the same preferences and db
        val repo2 = DailyLearningRepository(db, preferences, day1Clock)
        val wordsAfterRestart = repo2.getTodayWords(ToeicTarget.BASIC, limit = 10)

        assertEquals(10, wordsAfterRestart.size)
        for (i in initialWords.indices) {
            assertEquals(initialWords[i].word, wordsAfterRestart[i].word)
        }
    }

    @Test
    fun testCrossDayAutomaticRefresh() {
        val repoDay1 = DailyLearningRepository(db, preferences, day1Clock)
        val day1Words = repoDay1.getTodayWords(ToeicTarget.BASIC, limit = 10)
        assertEquals(10, day1Words.size)

        // Mark day 1 words as learned
        day1Words.forEach { word ->
            repoDay1.markWordAsLearned(word.word)
        }

        // Advance to Day 2
        val repoDay2 = DailyLearningRepository(db, preferences, day2Clock)
        val day2Words = repoDay2.getTodayWords(ToeicTarget.BASIC, limit = 10)
        assertEquals(10, day2Words.size)

        // Day 2 should not repeat unlearned words if new unlearned words exist
        // (Since day 1 words were scheduled for Day 2 as due reviews, let's verify how due reviews are handled)
        val day1WordSet = day1Words.map { it.word }.toSet()
        // Day 1 words have next_review_date = day1 + 1 = 2026-09-22, which is today on Day 2!
        // So Day 2 should prioritize them as Due Reviews!
        assertTrue(day2Words.any { it.word in day1WordSet })
    }

    @Test
    fun testMarkWordAsLearnedSemantics() {
        val repo = DailyLearningRepository(db, preferences, day1Clock)
        val testWord = "ability"

        repo.markWordAsLearned(testWord)

        val progress = db.wordProgressDao().getProgress(testWord)
        assertNotNull(progress)
        assertTrue(progress!!.learned)
        assertEquals("2026-09-21", progress.learnedDate)
        assertEquals("2026-09-22", progress.nextReviewDate)
        assertEquals(1, progress.intervalDays)
        assertEquals(1, progress.repetitionCount)
        assertEquals(2.5, progress.easinessFactor, 0.001) // SM-2 factor unchanged
    }

    @Test
    fun testDueReviewWordsPriority() {
        val repo = DailyLearningRepository(db, preferences, day1Clock)
        // Manually insert an overdue review word in toeic_basic
        val dueWord = "abandon" // basic word
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = dueWord,
                learned = true,
                learnedDate = "2026-09-15",
                nextReviewDate = "2026-09-20", // Overdue for 2026-09-21
                intervalDays = 3,
                repetitionCount = 2,
                wrongCount = 2,
                easinessFactor = 2.4
            )
        )

        val words = repo.getTodayWords(ToeicTarget.BASIC, limit = 10)
        assertEquals(10, words.size)
        // Overdue word must be the first word returned
        assertEquals(dueWord, words[0].word)
    }

    @Test
    fun testTierCompletedBoundaryNoCrossTierLeakage() {
        // Clear all words and insert only 2 basic words and 5 gold words
        db.clearAllTables()
        val dao = db.vocabularyDao()
        val words = listOf(
            com.andy.englishcoach.data.database.entity.VocabularyEntity(
                id = 1,
                word = "basic1",
                phonetic = "/b1/",
                translation = "基礎1",
                example = "ex1",
                exampleTranslation = "例1",
                level = "toeic_basic",
                difficulty = 1,
                topic = "General",
                subtopic = "Daily",
                examTags = "TOEIC",
                partOfSpeech = "n."
            ),
            com.andy.englishcoach.data.database.entity.VocabularyEntity(
                id = 2,
                word = "basic2",
                phonetic = "/b2/",
                translation = "基礎2",
                example = "ex2",
                exampleTranslation = "例2",
                level = "toeic_basic",
                difficulty = 1,
                topic = "General",
                subtopic = "Daily",
                examTags = "TOEIC",
                partOfSpeech = "v."
            ),
            com.andy.englishcoach.data.database.entity.VocabularyEntity(
                id = 3,
                word = "gold1",
                phonetic = "/g1/",
                translation = "金色1",
                example = "gex1",
                exampleTranslation = "金例1",
                level = "toeic_gold",
                difficulty = 4,
                topic = "Finance",
                subtopic = "Banking",
                examTags = "TOEIC",
                partOfSpeech = "n."
            )
        )
        dao.insertAll(words)

        val repo = DailyLearningRepository(db, preferences, day1Clock)

        // Mark all basic words as learned with future review dates (not due)
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = "basic1",
                learned = true,
                learnedDate = "2026-09-21",
                nextReviewDate = "2026-09-30",
                intervalDays = 9,
                repetitionCount = 3
            )
        )
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = "basic2",
                learned = true,
                learnedDate = "2026-09-21",
                nextReviewDate = "2026-09-30",
                intervalDays = 9,
                repetitionCount = 3
            )
        )

        // When requesting BASIC words, none are unlearned and none are due
        val basicWords = repo.getTodayWords(ToeicTarget.BASIC, limit = 10)
        // Must return empty list, and NOT pull "gold1"!
        assertTrue(basicWords.isEmpty())

        // Gold words must still be accessible when requesting GOLD
        val goldWords = repo.getTodayWords(ToeicTarget.GOLD, limit = 10)
        assertEquals(1, goldWords.size)
        assertEquals("gold1", goldWords[0].word)
    }

    @Test
    fun testSharedPreferencesImplementation() {
        val sharedPreferences = context.getSharedPreferences("test_daily_prefs", Context.MODE_PRIVATE)
        val spPrefs = SharedPreferencesDailyLearningPreferences(sharedPreferences)

        spPrefs.setTodayWordsDate("toeic_basic", "2026-09-21")
        assertEquals("2026-09-21", spPrefs.getTodayWordsDate("toeic_basic"))

        val ids = listOf(101L, 202L, 303L)
        spPrefs.setTodayWordIds("toeic_basic", ids)
        val loadedIds = spPrefs.getTodayWordIds("toeic_basic")
        assertEquals(ids, loadedIds)

        spPrefs.setUserLevel("toeic_advanced")
        assertEquals("toeic_advanced", spPrefs.getUserLevel())

        spPrefs.setLearningCompletedDate("2026-09-21")
        assertEquals("2026-09-21", spPrefs.getLearningCompletedDate())

        spPrefs.clearDailyCache("toeic_basic")
        assertEquals(null, spPrefs.getTodayWordsDate("toeic_basic"))
        assertTrue(spPrefs.getTodayWordIds("toeic_basic").isEmpty())
    }

    @Test
    fun testLearningCompletionStatus() {
        val repo = DailyLearningRepository(db, preferences, day1Clock)

        assertFalse(repo.isTodayLearningCompleted())
        repo.markTodayLearningCompleted()
        assertTrue(repo.isTodayLearningCompleted())

        // On Day 2, learning completion should be false until marked
        val repoDay2 = DailyLearningRepository(db, preferences, day2Clock)
        assertFalse(repoDay2.isTodayLearningCompleted())
    }
}
