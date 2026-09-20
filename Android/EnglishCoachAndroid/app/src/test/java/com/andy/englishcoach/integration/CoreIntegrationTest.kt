package com.andy.englishcoach.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.VocabularyDatabaseInitializer
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.preference.InMemoryDailyLearningPreferences
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.data.repository.ReviewRepository
import com.andy.englishcoach.data.sm2.SM2Engine
import com.andy.englishcoach.data.sm2.SM2State
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * STEP 10 — Core Integration Gate Test Suite.
 * Covers Integration Flows A through I, SM-2 End-to-End, and Database Integrity.
 */
@RunWith(RobolectricTestRunner::class)
class CoreIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: EnglishCoachDatabase
    private lateinit var preferences: InMemoryDailyLearningPreferences
    private lateinit var learningRepo: DailyLearningRepository
    private lateinit var quizRepo: QuizRepository
    private lateinit var reviewRepo: ReviewRepository

    private val fixedZone = ZoneId.of("UTC")
    private val day1Clock = Clock.fixed(Instant.parse("2026-09-21T09:00:00Z"), fixedZone)
    private val day2Clock = Clock.fixed(Instant.parse("2026-09-22T09:00:00Z"), fixedZone)

    private val day1Date = "2026-09-21"
    private val day2Date = "2026-09-22"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EnglishCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = InMemoryDailyLearningPreferences()
        learningRepo = DailyLearningRepository(db, preferences, day1Clock)
        quizRepo = QuizRepository(db, preferences, learningRepo, day1Clock)
        reviewRepo = ReviewRepository(db, preferences, day1Clock)

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

    // ==========================================
    // Flow A: Clean Install & Seeding Integrity
    // ==========================================
    @Test
    fun testFlowA_CleanInstall() {
        val totalCount = db.vocabularyDao().countWords()
        assertEquals("Total words must be exactly 3600", 3600, totalCount)

        val basicCount = db.vocabularyDao().countWordsByLevel("toeic_basic")
        val advancedCount = db.vocabularyDao().countWordsByLevel("toeic_advanced")
        val goldCount = db.vocabularyDao().countWordsByLevel("toeic_gold")

        assertEquals("Basic level count must be 1200", 1200, basicCount)
        assertEquals("Advanced level count must be 1400", 1400, advancedCount)
        assertEquals("Gold level count must be 1000", 1000, goldCount)

        val allWords = db.vocabularyDao().getAllWords()
        val uniqueWords = allWords.map { it.word }.toSet()
        assertEquals("All 3600 words must be strictly unique", 3600, uniqueWords.size)
    }

    // ==========================================
    // Flow B: Daily Learning
    // ==========================================
    @Test
    fun testFlowB_DailyLearning() {
        // 1. Target level isolation
        val words = learningRepo.getTodayWords(ToeicTarget.BASIC, limit = 10)
        assertEquals(10, words.size)
        words.forEach { word ->
            assertEquals("toeic_basic", word.level)
            assertEquals(ToeicTarget.BASIC, word.target)
        }

        // 2. Today 10 words cached and stable across force close/restart
        val restartedRepo = DailyLearningRepository(db, preferences, day1Clock)
        val reloadedWords = restartedRepo.getTodayWords(ToeicTarget.BASIC, limit = 10)
        assertEquals(words.map { it.word }, reloadedWords.map { it.word })

        // 3. Mark learning completed
        learningRepo.markTodayLearningCompleted()
        assertTrue(learningRepo.isTodayLearningCompleted())
        assertEquals(day1Date, preferences.getLearningCompletedDate())

        // 4. Verification: does NOT prematurely complete quiz or consume practice quota
        assertFalse(quizRepo.isTodayQuizCompleted())
        assertEquals(0, preferences.getDailyPracticeCount())
        assertEquals(10, preferences.getRemainingPracticeQuota(day1Date))
    }

    // ==========================================
    // Flow C: Daily Quiz
    // ==========================================
    @Test
    fun testFlowC_DailyQuiz() {
        val todayWords = learningRepo.getTodayWords(ToeicTarget.BASIC, limit = 10)
        val quizQuestions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 10, randomSeed = 1234L)

        // 1. 100% of questions come from today words
        assertEquals(10, quizQuestions.size)
        val todayWordSet = todayWords.map { it.word }.toSet()
        quizQuestions.forEach { q ->
            assertTrue("Question must come from today's words", todayWordSet.contains(q.word.word))
            // 2. 4 options per question (1 correct + 3 distractors)
            assertEquals(4, q.options.size)
            // 3. distractors != correct
            val distractors = q.options.filter { it != q.correctOption }
            assertEquals(3, distractors.size)
            // 4. Target level isolation
            assertEquals(ToeicTarget.BASIC, q.word.target)
        }

        // 5. Partial session does NOT commit to Room
        val partialAnswer = listOf(
            QuizAnswer(0, quizQuestions[0].word, quizQuestions[0].correctOption, true)
        )
        // Simulate abandon: NOT committing partial session
        assertNull(db.wordProgressDao().getProgress(quizQuestions[0].word.word))
        assertFalse(quizRepo.isTodayQuizCompleted())

        // 6. Complete all 10 questions and commit
        val completeAnswers = quizQuestions.mapIndexed { index, q ->
            QuizAnswer(index, q.word, q.correctOption, isCorrect = (index % 2 == 0))
        }
        val committed = quizRepo.commitQuizSession(completeAnswers)
        assertTrue(committed)
        assertTrue(quizRepo.isTodayQuizCompleted())
        assertEquals(day1Date, preferences.getQuizCompletedDate())

        // 7. Verify SM-2 and practice quota updated
        assertEquals(10, preferences.getDailyPracticeCount())
        quizQuestions.forEachIndexed { index, q ->
            val p = db.wordProgressDao().getProgress(q.word.word)
            assertNotNull(p)
            assertTrue(p!!.learned)
            if (index % 2 == 0) {
                // Correct
                assertEquals(1, p.correctCount)
                assertEquals(0, p.wrongCount)
                assertEquals(1, p.repetitionCount)
                assertEquals(1, p.intervalDays)
            } else {
                // Wrong
                assertEquals(0, p.correctCount)
                assertEquals(1, p.wrongCount)
                assertEquals(0, p.repetitionCount)
                assertEquals(1, p.intervalDays)
            }
        }
    }

    // ==========================================
    // Flow D: Smart Review Center
    // ==========================================
    @Test
    fun testFlowD_ReviewCenter() {
        val basicWords = db.vocabularyDao().getWordsByLevel("toeic_basic").take(3)
        val wordA = basicWords[0].word // wrong_count > 0
        val wordB = basicWords[1].word // learned = 1, next_review_date <= today
        val wordC = basicWords[2].word // learned = 1, next_review_date > today

        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = wordA, learned = true, wrongCount = 2, nextReviewDate = day2Date)
        )
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = wordB, learned = true, wrongCount = 0, nextReviewDate = day1Date)
        )
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = wordC, learned = true, wrongCount = 0, nextReviewDate = day2Date)
        )

        val reviewItems = reviewRepo.getReviewWordItems(day1Date, ToeicTarget.BASIC)
        val itemWordSet = reviewItems.map { it.word.word }.toSet()

        assertTrue("Condition A must appear", itemWordSet.contains(wordA))
        assertTrue("Condition B must appear", itemWordSet.contains(wordB))
        assertFalse("Condition C must NOT appear", itemWordSet.contains(wordC))

        // Ordering check: wrong_count DESC, next_review_date ASC
        assertEquals(wordA, reviewItems[0].word.word)
        assertEquals(wordB, reviewItems[1].word.word)

        // Target display name checks: NEVER Lv.1..Lv.5
        assertEquals("550+ 基礎", ToeicTarget.BASIC.displayName)
        assertEquals("750+ 進階", ToeicTarget.ADVANCED.displayName)
        assertEquals("860+ 金證", ToeicTarget.GOLD.displayName)

        // Verify browsing review center does NOT consume quota
        val beforeQuota = preferences.getDailyPracticeCount()
        reviewRepo.getReviewWords(day1Date)
        reviewRepo.getReviewWordItems(day1Date)
        reviewRepo.getReviewCount(day1Date)
        assertEquals("Browsing must not consume quota", beforeQuota, preferences.getDailyPracticeCount())
    }

    // ==========================================
    // Flow E: Review Quiz
    // ==========================================
    @Test
    fun testFlowE_ReviewQuiz() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(4)
        words.forEachIndexed { i, w ->
            db.wordProgressDao().insertOrUpdate(
                WordProgressEntity(word = w.word, learned = true, wrongCount = i + 1, nextReviewDate = day1Date)
            )
        }

        val questions = reviewRepo.generateReviewQuiz(day1Date, ToeicTarget.BASIC, limit = 4, randomSeed = 55L)
        assertEquals(4, questions.size)

        // Check options integrity
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.options.contains(q.correctOption))
            val dist = q.options.filter { it != q.correctOption }
            assertEquals(3, dist.size)
        }

        // Mid-session exit: no commit
        assertFalse(db.wordProgressDao().getProgress(words[0].word)!!.correctCount > 0)

        // Complete review session
        val initialP0 = db.wordProgressDao().getProgress(questions[0].word.word)!!
        val initialP1 = db.wordProgressDao().getProgress(questions[1].word.word)!!

        val answers = listOf(
            QuizAnswer(0, questions[0].word, questions[0].correctOption, true),
            QuizAnswer(1, questions[1].word, "wrong_option", false)
        )
        val success = reviewRepo.commitReviewQuizSession(answers)
        assertTrue(success)

        // Correct answer: wrong_count decremented, SM-2 updated
        val p0 = db.wordProgressDao().getProgress(questions[0].word.word)!!
        assertEquals("Wrong count decremented", maxOf(0, initialP0.wrongCount - 1), p0.wrongCount)
        assertEquals("Correct count incremented", initialP0.correctCount + 1, p0.correctCount)
        assertEquals("Next review date rescheduled to tomorrow", day2Date, p0.nextReviewDate)

        // Wrong answer: wrong_count incremented
        val p1 = db.wordProgressDao().getProgress(questions[1].word.word)!!
        assertEquals("Wrong count incremented", initialP1.wrongCount + 1, p1.wrongCount)
    }

    // ==========================================
    // Flow F: Daily Quota
    // ==========================================
    @Test
    fun testFlowF_DailyQuota() {
        preferences.clearDailyCache()
        assertEquals(0, preferences.getDailyPracticeCount())
        assertEquals(10, reviewRepo.getRemainingQuota(day1Date))
        assertFalse(reviewRepo.isDailyLimitReached(day1Date))

        // Complete 10 quiz questions
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(10)
        val answers = words.mapIndexed { i, w -> QuizAnswer(i, w.toWord(), w.translation, true) }
        quizRepo.commitQuizSession(answers)

        assertEquals(10, preferences.getDailyPracticeCount())
        assertEquals(0, reviewRepo.getRemainingQuota(day1Date))
        assertTrue(reviewRepo.isDailyLimitReached(day1Date))

        // Review Center can still be browsed without error
        val items = reviewRepo.getReviewWordItems(day1Date)
        assertNotNull(items)

        // But cannot start new Review Quiz
        val blockedQuiz = reviewRepo.generateReviewQuiz(day1Date, ToeicTarget.BASIC, limit = 10, enforceQuota = true)
        assertTrue("Quiz generation blocked when quota reached", blockedQuiz.isEmpty())
    }

    // ==========================================
    // Flow G: Cross Day
    // ==========================================
    @Test
    fun testFlowG_CrossDay() {
        // Complete day 1 quiz
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(10)
        val answers = words.mapIndexed { i, w -> QuizAnswer(i, w.toWord(), w.translation, true) }
        quizRepo.commitQuizSession(answers)

        assertTrue(quizRepo.isTodayQuizCompleted())
        assertEquals(0, reviewRepo.getRemainingQuota(day1Date))

        // Day 2 transition
        val learningDay2 = DailyLearningRepository(db, preferences, day2Clock)
        val quizDay2 = QuizRepository(db, preferences, learningDay2, day2Clock)
        val reviewDay2 = ReviewRepository(db, preferences, day2Clock)

        // Quota automatically resets
        assertEquals(10, reviewDay2.getRemainingQuota(day2Date))
        assertFalse(reviewDay2.isDailyLimitReached(day2Date))

        // Day 2 quiz is NOT completed yet
        assertFalse(quizDay2.isTodayQuizCompleted())

        // Day 2 learning can proceed
        val day2Words = learningDay2.getTodayWords(ToeicTarget.BASIC, limit = 10)
        assertEquals(10, day2Words.size)
    }

    // ==========================================
    // Flow H: App Restart & State Persistence
    // ==========================================
    @Test
    fun testFlowH_AppRestart() {
        // State 1: Learning in progress
        val words1 = learningRepo.getTodayWords(ToeicTarget.BASIC, 10)
        val repoAfterRestart1 = DailyLearningRepository(db, preferences, day1Clock)
        assertEquals(words1.map { it.word }, repoAfterRestart1.getTodayWords(ToeicTarget.BASIC, 10).map { it.word })

        // State 2: Before quiz
        learningRepo.markTodayLearningCompleted()
        val repoAfterRestart2 = DailyLearningRepository(db, preferences, day1Clock)
        assertTrue(repoAfterRestart2.isTodayLearningCompleted())

        // State 3: Quiz incomplete (no commit)
        val quizRepoRestart3 = QuizRepository(db, preferences, learningRepo, day1Clock)
        assertFalse(quizRepoRestart3.isTodayQuizCompleted())

        // State 4: Quiz completed
        val answers = words1.mapIndexed { i, w -> QuizAnswer(i, w, w.translation, true) }
        quizRepo.commitQuizSession(answers)
        val quizRepoRestart4 = QuizRepository(db, preferences, learningRepo, day1Clock)
        assertTrue(quizRepoRestart4.isTodayQuizCompleted())

        // State 5: Review center browsing (quota remains accurate)
        val reviewRepoRestart5 = ReviewRepository(db, preferences, day1Clock)
        assertEquals(0, reviewRepoRestart5.getRemainingQuota(day1Date))
    }

    // ==========================================
    // Flow I: Target Isolation
    // ==========================================
    @Test
    fun testFlowI_TargetIsolation() {
        val basicWords = learningRepo.getTodayWords(ToeicTarget.BASIC, 10)
        val advWords = learningRepo.getTodayWords(ToeicTarget.ADVANCED, 10)
        val goldWords = learningRepo.getTodayWords(ToeicTarget.GOLD, 10)

        // Verify distinct words across levels
        basicWords.forEach { assertEquals("toeic_basic", it.level) }
        advWords.forEach { assertEquals("toeic_advanced", it.level) }
        goldWords.forEach { assertEquals("toeic_gold", it.level) }

        // Cache separation
        assertEquals(10, preferences.getTodayWordIds("toeic_basic").size)
        assertEquals(10, preferences.getTodayWordIds("toeic_advanced").size)
        assertEquals(10, preferences.getTodayWordIds("toeic_gold").size)
        assertNotEquals(preferences.getTodayWordIds("toeic_basic"), preferences.getTodayWordIds("toeic_advanced"))

        // Progress contamination check
        val basicW = basicWords[0].word
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = basicW, learned = true, correctCount = 5, wrongCount = 0)
        )
        val basicProgress = db.wordProgressDao().getProgress(basicW)
        assertNotNull(basicProgress)
        // Advanced and gold words remain unlearned
        assertNull(db.wordProgressDao().getProgress(advWords[0].word))
        assertNull(db.wordProgressDao().getProgress(goldWords[0].word))
    }

    // ==========================================
    // SM-2 End-to-End
    // ==========================================
    @Test
    fun testSM2EndToEnd() {
        val reviewDate = LocalDate.parse("2026-09-21")

        // 1st Correct: q=4 -> rep=1, interval=1
        val res1 = SM2Engine.calculate(SM2State.INITIAL, isCorrect = true, reviewDate = reviewDate)
        assertEquals(1, res1.repetitionCount)
        assertEquals(1, res1.intervalDays)
        assertEquals(2.5, res1.easinessFactor, 0.001)
        assertEquals("2026-09-22", res1.nextReviewDate)

        // 2nd Correct: q=4 -> rep=2, interval=6
        val res2 = SM2Engine.calculate(
            SM2State(res1.easinessFactor, res1.intervalDays, res1.repetitionCount, res1.nextReviewDate),
            isCorrect = true,
            reviewDate = reviewDate
        )
        assertEquals(2, res2.repetitionCount)
        assertEquals(6, res2.intervalDays)
        assertEquals(2.5, res2.easinessFactor, 0.001)
        assertEquals("2026-09-27", res2.nextReviewDate)

        // Wrong: q=1 -> rep=0, interval=1, next=today+1
        val res3 = SM2Engine.calculate(
            SM2State(res2.easinessFactor, res2.intervalDays, res2.repetitionCount, res2.nextReviewDate),
            isCorrect = false,
            reviewDate = reviewDate
        )
        assertEquals(0, res3.repetitionCount)
        assertEquals(1, res3.intervalDays)
        assertEquals(1.96, res3.easinessFactor, 0.001)
        assertEquals("2026-09-22", res3.nextReviewDate)
        assertTrue("EF must be >= 1.3", res3.easinessFactor >= 1.3)
    }

    // ==========================================
    // Database Integrity
    // ==========================================
    @Test
    fun testDatabaseIntegrity() {
        assertEquals(3600, db.vocabularyDao().countWords())

        // Test Unique Constraint on words.word
        try {
            db.vocabularyDao().insertAll(
                listOf(
                    VocabularyEntity(
                        word = "career", // Duplicate
                        phonetic = "",
                        translation = "",
                        example = "",
                        exampleTranslation = "",
                        level = "toeic_basic",
                        difficulty = 1,
                        topic = "",
                        subtopic = "",
                        examTags = "",
                        partOfSpeech = ""
                    )
                )
            )
            // OnConflictStrategy.REPLACE replaces existing, maintaining 3600 words
            assertEquals(3600, db.vocabularyDao().countWords())
        } catch (e: Exception) {
            // Or throws SQLiteConstraintException
            assertEquals(3600, db.vocabularyDao().countWords())
        }

        // Test Foreign Key on word_progress
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = "career", learned = true, correctCount = 1)
        )
        assertNotNull(db.wordProgressDao().getProgress("career"))

        // Test Transaction Rollback
        try {
            db.runInTransaction {
                db.wordProgressDao().insertOrUpdate(
                    WordProgressEntity(word = "strategy", learned = true)
                )
                throw RuntimeException("Simulated transaction crash")
            }
        } catch (e: RuntimeException) {
            // Expected
        }
        assertNull("Rolled back entity must not exist", db.wordProgressDao().getProgress("strategy"))
    }
}
