package com.andy.englishcoach.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.database.dao.VocabularyDao
import com.andy.englishcoach.data.database.dao.WordProgressDao
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.sm2.SM2Engine
import com.andy.englishcoach.data.sm2.SM2State
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: EnglishCoachDatabase
    private lateinit var vocabDao: VocabularyDao
    private lateinit var progressDao: WordProgressDao
    private lateinit var csvFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EnglishCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vocabDao = db.vocabularyDao()
        progressDao = db.wordProgressDao()

        val possiblePaths = listOf(
            File("src/main/assets/toeic_3600.csv"),
            File("app/src/main/assets/toeic_3600.csv"),
            File("../app/src/main/assets/toeic_3600.csv")
        )
        csvFile = possiblePaths.firstOrNull { it.exists() }
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
    fun testCountWordsEquals3600() {
        val totalCount = vocabDao.countWords()
        assertEquals("Total vocabulary count in Room must be exactly 3,600", 3600, totalCount)
    }

    @Test
    fun testLevelDistribution() {
        val basicCount = vocabDao.countWordsByLevel("toeic_basic")
        val advancedCount = vocabDao.countWordsByLevel("toeic_advanced")
        val goldCount = vocabDao.countWordsByLevel("toeic_gold")

        assertEquals("toeic_basic count must be 1,200", 1200, basicCount)
        assertEquals("toeic_advanced count must be 1,400", 1400, advancedCount)
        assertEquals("toeic_gold count must be 1,000", 1000, goldCount)
        assertEquals(3600, basicCount + advancedCount + goldCount)
    }

    @Test
    fun testDuplicateCountIsZero() {
        val allWords = vocabDao.getAllWords()
        val duplicates = allWords.groupBy { it.word }.filter { it.value.size > 1 }
        assertTrue("Duplicate words found in database: ${duplicates.keys}", duplicates.isEmpty())
        assertEquals(3600, allWords.map { it.word }.toSet().size)
    }

    @Test
    fun testGetWordsByLevelBasic() {
        val basicWords = vocabDao.getWordsByLevel("toeic_basic")
        assertEquals(1200, basicWords.size)
        for (w in basicWords) {
            assertEquals("toeic_basic", w.level)
            assertEquals(ToeicTarget.BASIC, w.target)
        }
    }

    @Test
    fun testGetWordsByLevelAdvanced() {
        val advancedWords = vocabDao.getWordsByLevel("toeic_advanced")
        assertEquals(1400, advancedWords.size)
        for (w in advancedWords) {
            assertEquals("toeic_advanced", w.level)
            assertEquals(ToeicTarget.ADVANCED, w.target)
        }
    }

    @Test
    fun testGetWordsByLevelGold() {
        val goldWords = vocabDao.getWordsByLevel("toeic_gold")
        assertEquals(1000, goldWords.size)
        for (w in goldWords) {
            assertEquals("toeic_gold", w.level)
            assertEquals(ToeicTarget.GOLD, w.target)
        }
    }

    @Test
    fun testRandomSampleFromEachLevelHasComplete11Fields() {
        val basicSample = vocabDao.getWordsByLevel("toeic_basic").take(5)
        val advancedSample = vocabDao.getWordsByLevel("toeic_advanced").take(5)
        val goldSample = vocabDao.getWordsByLevel("toeic_gold").take(5)

        val samples = basicSample + advancedSample + goldSample
        assertEquals(15, samples.size)

        for (w in samples) {
            assertTrue("ID must be positive", w.id > 0)
            assertTrue("Word must not be blank", w.word.isNotBlank())
            assertTrue("Phonetic must start with '/'", w.phonetic.startsWith("/"))
            assertTrue("Phonetic must end with '/'", w.phonetic.endsWith("/"))
            assertTrue("Translation must not be blank", w.translation.isNotBlank())
            assertTrue("Example must not be blank", w.example.isNotBlank())
            assertTrue("Example translation must not be blank", w.exampleTranslation.isNotBlank())
            assertTrue("Level must be valid", w.level in listOf("toeic_basic", "toeic_advanced", "toeic_gold"))
            assertTrue("Difficulty must be in range 1..5", w.difficulty in 1..5)
            assertTrue("Topic must not be blank", w.topic.isNotBlank())
            assertTrue("Subtopic must not be blank", w.subtopic.isNotBlank())
            assertTrue("Exam tags must not be blank", w.examTags.isNotBlank())
            assertTrue("Part of speech must not be blank", w.partOfSpeech.isNotBlank())
        }
    }

    @Test
    fun testWordProgressInitialStateAndCreation() {
        // Initial state: brand new word has no progress record
        val initialProgress = progressDao.getProgress("carry")
        assertNull("Brand new word must not have progress record initially", initialProgress)

        // Create new progress record for word "carry"
        val newProgress = WordProgressEntity(
            word = "carry",
            learned = true,
            learnedDate = "2026-09-21",
            correctCount = 1,
            wrongCount = 0,
            easinessFactor = 2.5,
            intervalDays = 1,
            repetitionCount = 1,
            nextReviewDate = "2026-09-22"
        )
        progressDao.insertOrUpdate(newProgress)

        val retrieved = progressDao.getProgress("carry")
        assertNotNull(retrieved)
        assertEquals("carry", retrieved?.word)
        assertEquals(true, retrieved?.learned)
        assertEquals("2026-09-21", retrieved?.learnedDate)
        assertEquals(1, retrieved?.correctCount)
        assertEquals(0, retrieved?.wrongCount)
        assertEquals(2.5, retrieved?.easinessFactor ?: 0.0, 0.0001)
        assertEquals(1, retrieved?.intervalDays)
        assertEquals(1, retrieved?.repetitionCount)
        assertEquals("2026-09-22", retrieved?.nextReviewDate)
    }

    @Test
    fun testWordProgressUpdateWithSM2() {
        val today = LocalDate.of(2026, 9, 21)

        // Step 1: Initial learning
        var sm2Result = SM2Engine.calculate(SM2State.INITIAL, isCorrect = true, reviewDate = today)
        var progress = WordProgressEntity(
            word = "fare",
            learned = true,
            learnedDate = today.toString(),
            correctCount = 1,
            wrongCount = 0,
            easinessFactor = sm2Result.easinessFactor,
            intervalDays = sm2Result.intervalDays,
            repetitionCount = sm2Result.repetitionCount,
            nextReviewDate = sm2Result.nextReviewDate
        )
        progressDao.insertOrUpdate(progress)

        var saved = progressDao.getProgress("fare")
        assertNotNull(saved)
        assertEquals(1, saved?.intervalDays)
        assertEquals(1, saved?.repetitionCount)
        assertEquals("2026-09-22", saved?.nextReviewDate)

        // Step 2: Next day review: correct answer
        val nextReviewDay = LocalDate.of(2026, 9, 22)
        val currentState = SM2State(saved!!.easinessFactor, saved.intervalDays, saved.repetitionCount, saved.nextReviewDate)
        sm2Result = SM2Engine.calculate(currentState, isCorrect = true, reviewDate = nextReviewDay)

        progress = saved.copy(
            correctCount = saved.correctCount + 1,
            easinessFactor = sm2Result.easinessFactor,
            intervalDays = sm2Result.intervalDays,
            repetitionCount = sm2Result.repetitionCount,
            nextReviewDate = sm2Result.nextReviewDate
        )
        progressDao.insertOrUpdate(progress)

        saved = progressDao.getProgress("fare")
        assertNotNull(saved)
        assertEquals(2, saved?.correctCount)
        assertEquals(6, saved?.intervalDays)
        assertEquals(2, saved?.repetitionCount)
        assertEquals("2026-09-28", saved?.nextReviewDate)

        // Step 3: Subsequent review: wrong answer (q=1, resets interval=1, rep=0)
        val wrongDay = LocalDate.of(2026, 9, 28)
        val stateBeforeWrong = SM2State(saved!!.easinessFactor, saved.intervalDays, saved.repetitionCount, saved.nextReviewDate)
        sm2Result = SM2Engine.calculate(stateBeforeWrong, isCorrect = false, reviewDate = wrongDay)

        progress = saved.copy(
            wrongCount = saved.wrongCount + 1,
            easinessFactor = sm2Result.easinessFactor,
            intervalDays = sm2Result.intervalDays,
            repetitionCount = sm2Result.repetitionCount,
            nextReviewDate = sm2Result.nextReviewDate
        )
        progressDao.insertOrUpdate(progress)

        saved = progressDao.getProgress("fare")
        assertNotNull(saved)
        assertEquals(2, saved?.correctCount)
        assertEquals(1, saved?.wrongCount)
        assertEquals(1, saved?.intervalDays)
        assertEquals(0, saved?.repetitionCount)
        assertEquals(1.96, saved?.easinessFactor ?: 0.0, 0.0001)
        assertEquals("2026-09-29", saved?.nextReviewDate)
    }

    @Test
    fun testDueReviewQueriesAndTargetStateFiltering() {
        val todayStr = "2026-09-21"

        // Word 1: Due (past date)
        progressDao.insertOrUpdate(
            WordProgressEntity(word = "carry", learned = true, learnedDate = "2026-09-18", nextReviewDate = "2026-09-19", wrongCount = 0)
        )
        // Word 2: Due (today)
        progressDao.insertOrUpdate(
            WordProgressEntity(word = "fare", learned = true, learnedDate = "2026-09-20", nextReviewDate = "2026-09-21", wrongCount = 1)
        )
        // Word 3: Not due yet (future)
        progressDao.insertOrUpdate(
            WordProgressEntity(word = "link", learned = true, learnedDate = "2026-09-21", nextReviewDate = "2026-09-27", wrongCount = 0)
        )
        // Word 4: Not learned yet (even if nextReviewDate set, learned must be true)
        progressDao.insertOrUpdate(
            WordProgressEntity(word = "venue", learned = false, nextReviewDate = "2026-09-20", wrongCount = 0)
        )

        val dueWords = progressDao.getWordsDueForReview(todayStr)
        val dueWordNames = dueWords.map { it.word }

        assertEquals(2, dueWords.size)
        assertTrue("carry (past) must be due", dueWordNames.contains("carry"))
        assertTrue("fare (today) must be due", dueWordNames.contains("fare"))
        assertTrue("link (future) must NOT be due", !dueWordNames.contains("link"))
        assertTrue("venue (unlearned) must NOT be due", !dueWordNames.contains("venue"))

        assertEquals(2, progressDao.getReviewCount(todayStr))
        assertEquals(3, progressDao.getLearnedCount())

        val wrongWords = progressDao.getWrongWords()
        assertEquals(1, wrongWords.size)
        assertEquals("fare", wrongWords.first().word)
    }

    @Test
    fun testTransactionAndReopenPersistence() {
        val testDbFile = File(context.cacheDir, "test_reopen.db")
        if (testDbFile.exists()) testDbFile.delete()

        // 1. First open: populate vocabulary and progress
        var persistentDb = Room.databaseBuilder(context, EnglishCoachDatabase::class.java, testDbFile.absolutePath)
            .allowMainThreadQueries()
            .build()

        FileInputStream(csvFile).use { stream ->
            VocabularyDatabaseInitializer.populateFromStream(persistentDb, stream)
        }
        assertEquals(3600, persistentDb.vocabularyDao().countWords())

        persistentDb.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = "carry",
                learned = true,
                learnedDate = "2026-09-21",
                correctCount = 3,
                wrongCount = 1,
                easinessFactor = 2.5,
                intervalDays = 6,
                repetitionCount = 2,
                nextReviewDate = "2026-09-27"
            )
        )

        persistentDb.close()

        // 2. Second open: verify vocabulary and progress persist across connection reopening
        persistentDb = Room.databaseBuilder(context, EnglishCoachDatabase::class.java, testDbFile.absolutePath)
            .allowMainThreadQueries()
            .build()

        assertEquals(3600, persistentDb.vocabularyDao().countWords())
        assertEquals(1200, persistentDb.vocabularyDao().countWordsByLevel("toeic_basic"))
        assertEquals(1400, persistentDb.vocabularyDao().countWordsByLevel("toeic_advanced"))
        assertEquals(1000, persistentDb.vocabularyDao().countWordsByLevel("toeic_gold"))

        val word = persistentDb.vocabularyDao().getWordByWord("carry")
        assertNotNull(word)
        assertEquals("toeic_basic", word?.level)

        val progress = persistentDb.wordProgressDao().getProgress("carry")
        assertNotNull(progress)
        assertEquals(true, progress?.learned)
        assertEquals(3, progress?.correctCount)
        assertEquals(1, progress?.wrongCount)
        assertEquals(6, progress?.intervalDays)
        assertEquals(2, progress?.repetitionCount)
        assertEquals("2026-09-27", progress?.nextReviewDate)

        persistentDb.close()
        testDbFile.delete()
    }
}
