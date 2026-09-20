package com.andy.englishcoach.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.VocabularyDatabaseInitializer
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.preference.InMemoryDailyLearningPreferences
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
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class ReviewRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: EnglishCoachDatabase
    private lateinit var preferences: InMemoryDailyLearningPreferences
    private lateinit var reviewRepo: ReviewRepository

    private val fixedZone = ZoneId.of("UTC")
    private val day1Clock = Clock.fixed(Instant.parse("2026-09-21T09:00:00Z"), fixedZone)
    private val day2Clock = Clock.fixed(Instant.parse("2026-09-22T09:00:00Z"), fixedZone)

    private val today = "2026-09-21"
    private val tomorrow = "2026-09-22"
    private val yesterday = "2026-09-20"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EnglishCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = InMemoryDailyLearningPreferences()
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

    // A. Review query
    @Test
    fun testReviewQuery() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(4)

        // word0: wrong_count > 0, next_review_date = tomorrow (future) -> should be included
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = words[0].word,
                learned = true,
                wrongCount = 2,
                nextReviewDate = tomorrow
            )
        )

        // word1: learned = 1, next_review_date = today, wrong_count = 0 -> should be included
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = words[1].word,
                learned = true,
                wrongCount = 0,
                nextReviewDate = today
            )
        )

        // word2: learned = 1, next_review_date = tomorrow (future), wrong_count = 0 -> EXCLUDED
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = words[2].word,
                learned = true,
                wrongCount = 0,
                nextReviewDate = tomorrow
            )
        )

        // word3: learned = 0, wrong_count = 0 -> EXCLUDED
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = words[3].word,
                learned = false,
                wrongCount = 0,
                nextReviewDate = null
            )
        )

        val reviewWords = reviewRepo.getReviewWords(today)
        assertEquals(2, reviewWords.size)
        val wordSet = reviewWords.map { it.word }.toSet()
        assertTrue(wordSet.contains(words[0].word))
        assertTrue(wordSet.contains(words[1].word))
        assertFalse(wordSet.contains(words[2].word))
        assertFalse(wordSet.contains(words[3].word))
    }

    // B. Review count
    @Test
    fun testReviewCount() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(3)
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = words[0].word, learned = true, wrongCount = 1, nextReviewDate = tomorrow)
        )
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = words[1].word, learned = true, wrongCount = 0, nextReviewDate = today)
        )

        val count = reviewRepo.getReviewCount(today)
        val list = reviewRepo.getReviewWords(today)
        assertEquals(2, count)
        assertEquals(count, list.size)
    }

    // C. Only learned words (unlearned words with wrong_count = 0 are never in review list)
    @Test
    fun testOnlyLearnedWords() {
        val word = db.vocabularyDao().getWordsByLevel("toeic_basic").first()
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = word.word, learned = false, wrongCount = 0, nextReviewDate = today)
        )
        val reviewWords = reviewRepo.getReviewWords(today)
        assertTrue(reviewWords.none { it.word == word.word })
    }

    // D. Only due words (learned words with next_review_date <= today are included)
    @Test
    fun testOnlyDueWords() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(2)
        // yesterday (due)
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = words[0].word, learned = true, wrongCount = 0, nextReviewDate = yesterday)
        )
        // today (due)
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = words[1].word, learned = true, wrongCount = 0, nextReviewDate = today)
        )

        val reviewWords = reviewRepo.getReviewWords(today)
        assertEquals(2, reviewWords.size)
    }

    // E. Future review date excluded
    @Test
    fun testFutureReviewDateExcluded() {
        val word = db.vocabularyDao().getWordsByLevel("toeic_basic").first()
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = word.word, learned = true, wrongCount = 0, nextReviewDate = tomorrow)
        )
        val reviewWords = reviewRepo.getReviewWords(today)
        assertTrue(reviewWords.isEmpty())
        assertEquals(0, reviewRepo.getReviewCount(today))
    }

    // F. Target isolation
    @Test
    fun testTargetIsolation() {
        val basicWord = db.vocabularyDao().getWordsByLevel("toeic_basic").first()
        val advancedWord = db.vocabularyDao().getWordsByLevel("toeic_advanced").first()
        val goldWord = db.vocabularyDao().getWordsByLevel("toeic_gold").first()

        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = basicWord.word, learned = true, wrongCount = 1, nextReviewDate = today)
        )
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = advancedWord.word, learned = true, wrongCount = 1, nextReviewDate = today)
        )
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = goldWord.word, learned = true, wrongCount = 1, nextReviewDate = today)
        )

        val basicReviews = reviewRepo.getReviewWords(today, ToeicTarget.BASIC)
        assertEquals(1, basicReviews.size)
        assertEquals("toeic_basic", basicReviews.first().level)

        val advReviews = reviewRepo.getReviewWords(today, ToeicTarget.ADVANCED)
        assertEquals(1, advReviews.size)
        assertEquals("toeic_advanced", advReviews.first().level)

        val goldReviews = reviewRepo.getReviewWords(today, ToeicTarget.GOLD)
        assertEquals(1, goldReviews.size)
        assertEquals("toeic_gold", goldReviews.first().level)
    }

    // G. Review word ordering (wrong_count DESC, next_review_date ASC)
    @Test
    fun testReviewWordOrdering() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(3)
        // word0: wrong_count = 1, next_review_date = today
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = words[0].word, learned = true, wrongCount = 1, nextReviewDate = today)
        )
        // word1: wrong_count = 3, next_review_date = tomorrow
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = words[1].word, learned = true, wrongCount = 3, nextReviewDate = tomorrow)
        )
        // word2: wrong_count = 0, next_review_date = yesterday
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = words[2].word, learned = true, wrongCount = 0, nextReviewDate = yesterday)
        )

        val reviewWords = reviewRepo.getReviewWords(today)
        assertEquals(3, reviewWords.size)
        // Order must be: word1 (wrong_count 3), word0 (wrong_count 1), word2 (wrong_count 0)
        assertEquals(words[1].word, reviewWords[0].word)
        assertEquals(words[0].word, reviewWords[1].word)
        assertEquals(words[2].word, reviewWords[2].word)
    }

    // H. Review quiz generation
    @Test
    fun testReviewQuizGeneration() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(5)
        words.forEach { w ->
            db.wordProgressDao().insertOrUpdate(
                WordProgressEntity(word = w.word, learned = true, wrongCount = 1, nextReviewDate = today)
            )
        }

        val questions = reviewRepo.generateReviewQuiz(today, limit = 5, randomSeed = 42L)
        assertEquals(5, questions.size)
        val expectedWords = words.map { it.word }.toSet()
        questions.forEach { q ->
            assertTrue(expectedWords.contains(q.word.word))
        }
    }

    // I. 4 options per question
    @Test
    fun testFourOptionsPerQuestion() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(3)
        words.forEach { w ->
            db.wordProgressDao().insertOrUpdate(
                WordProgressEntity(word = w.word, learned = true, wrongCount = 1, nextReviewDate = today)
            )
        }

        val questions = reviewRepo.generateReviewQuiz(today, limit = 3, randomSeed = 100L)
        assertEquals(3, questions.size)
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue("Options must contain correct translation", q.options.contains(q.correctOption))
            assertEquals(q.word.translation, q.correctOption)
        }
    }

    // J. Correct / wrong detection
    @Test
    fun testCorrectWrongDetection() {
        val word = db.vocabularyDao().getWordsByLevel("toeic_basic").first()
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = word.word, learned = true, wrongCount = 1, nextReviewDate = today)
        )

        val questions = reviewRepo.generateReviewQuiz(today, limit = 1, randomSeed = 1L)
        val q = questions.first()

        val correctAnswer = QuizAnswer(questionIndex = 0, word = q.word, chosenOption = q.correctOption, isCorrect = true)
        assertTrue(correctAnswer.isCorrect)

        val wrongOption = q.options.first { it != q.correctOption }
        val wrongAnswer = QuizAnswer(questionIndex = 0, word = q.word, chosenOption = wrongOption, isCorrect = false)
        assertFalse(wrongAnswer.isCorrect)
    }

    // K. Partial session does not commit
    @Test
    fun testPartialSessionDoesNotCommit() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(3)
        words.forEach { w ->
            db.wordProgressDao().insertOrUpdate(
                WordProgressEntity(
                    word = w.word,
                    learned = true,
                    correctCount = 0,
                    wrongCount = 2,
                    nextReviewDate = today
                )
            )
        }

        val questions = reviewRepo.generateReviewQuiz(today, limit = 3, randomSeed = 123L)
        assertEquals(3, questions.size)

        // Answering only question 0, then quitting without commitReviewQuizSession
        val answer0 = QuizAnswer(0, questions[0].word, questions[0].correctOption, true)
        // User abandons session: NO commitReviewQuizSession call!

        // Verify DB unchanged: word0 still has correctCount = 0, wrongCount = 2
        val p0 = db.wordProgressDao().getProgress(questions[0].word.word)
        assertNotNull(p0)
        assertEquals(0, p0!!.correctCount)
        assertEquals(2, p0.wrongCount)
    }

    // L. Complete session commits
    @Test
    fun testCompleteSessionCommits() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(2)
        words.forEach { w ->
            db.wordProgressDao().insertOrUpdate(
                WordProgressEntity(
                    word = w.word,
                    learned = true,
                    correctCount = 1,
                    wrongCount = 2,
                    nextReviewDate = today
                )
            )
        }

        val questions = reviewRepo.generateReviewQuiz(today, limit = 2, randomSeed = 99L)
        val answers = listOf(
            QuizAnswer(0, questions[0].word, questions[0].correctOption, true),
            QuizAnswer(1, questions[1].word, "wrong_option", false)
        )

        val result = reviewRepo.commitReviewQuizSession(answers)
        assertTrue(result)

        val p0 = db.wordProgressDao().getProgress(questions[0].word.word)!!
        assertEquals("Correct answer increments correctCount", 2, p0.correctCount)
        assertEquals("Correct answer in review decrements wrongCount", 1, p0.wrongCount)

        val p1 = db.wordProgressDao().getProgress(questions[1].word.word)!!
        assertEquals("Wrong answer keeps correctCount", 1, p1.correctCount)
        assertEquals("Wrong answer increments wrongCount", 3, p1.wrongCount)
    }

    // M. SM-2 update
    @Test
    fun testSM2Update() {
        val word = db.vocabularyDao().getWordsByLevel("toeic_basic").first()
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = word.word,
                learned = true,
                easinessFactor = 2.5,
                intervalDays = 0,
                repetitionCount = 0,
                correctCount = 0,
                wrongCount = 1,
                nextReviewDate = today
            )
        )

        val answer = QuizAnswer(0, word.toWord(), word.translation, isCorrect = true)
        reviewRepo.commitReviewQuizSession(listOf(answer))

        val updated = db.wordProgressDao().getProgress(word.word)!!
        assertEquals("Repetition increases by 1", 1, updated.repetitionCount)
        assertEquals("Interval becomes 1", 1, updated.intervalDays)
        assertEquals(2.5, updated.easinessFactor, 0.001)
        assertEquals("Next review date is tomorrow", tomorrow, updated.nextReviewDate)
        assertEquals(0, updated.wrongCount)
        assertEquals(1, updated.correctCount)
    }

    // N. Quota behavior
    @Test
    fun testQuotaBehavior() {
        val words = db.vocabularyDao().getWordsByLevel("toeic_basic").take(5)
        words.forEach { w ->
            db.wordProgressDao().insertOrUpdate(
                WordProgressEntity(word = w.word, learned = true, wrongCount = 1, nextReviewDate = today)
            )
        }

        // 1. Browsing does NOT consume quota
        val initialQuota = reviewRepo.getRemainingQuota(today)
        assertEquals(10, initialQuota)
        reviewRepo.getReviewWords(today)
        reviewRepo.getReviewCount(today)
        reviewRepo.getReviewWordItems(today)
        assertEquals(10, reviewRepo.getRemainingQuota(today))

        // 2. Committing quiz consumes quota
        val questions = reviewRepo.generateReviewQuiz(today, limit = 5, randomSeed = 1L)
        val answers = questions.map { QuizAnswer(it.index, it.word, it.correctOption, true) }
        reviewRepo.commitReviewQuizSession(answers)

        assertEquals(5, reviewRepo.getRemainingQuota(today))
        assertFalse(reviewRepo.isDailyLimitReached(today))

        // 3. Complete remaining quota
        reviewRepo.commitReviewQuizSession(answers)
        assertEquals(0, reviewRepo.getRemainingQuota(today))
        assertTrue(reviewRepo.isDailyLimitReached(today))

        // 4. When quota reached, generateReviewQuiz returns empty list
        val blockedQuestions = reviewRepo.generateReviewQuiz(today, limit = 5)
        assertTrue(blockedQuestions.isEmpty())
    }

    // O. Empty review state
    @Test
    fun testEmptyReviewState() {
        // No words marked as due or wrong
        val list = reviewRepo.getReviewWords(today)
        val count = reviewRepo.getReviewCount(today)
        val items = reviewRepo.getReviewWordItems(today)
        val quiz = reviewRepo.generateReviewQuiz(today)

        assertTrue(list.isEmpty())
        assertEquals(0, count)
        assertTrue(items.isEmpty())
        assertTrue(quiz.isEmpty())
    }

    // P. Cross-day behavior
    @Test
    fun testCrossDayBehavior() {
        val word = db.vocabularyDao().getWordsByLevel("toeic_basic").first()
        // Scheduled for tomorrow (day 2: 2026-09-22)
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = word.word, learned = true, wrongCount = 0, nextReviewDate = tomorrow)
        )

        // On day 1 (today: 2026-09-21), word is NOT due
        val day1Reviews = reviewRepo.getReviewWords(today)
        assertTrue(day1Reviews.isEmpty())

        // Consume day 1 quota
        preferences.recordPracticeQuestions(today, 10)
        assertEquals(0, reviewRepo.getRemainingQuota(today))

        // Switch clock to day 2 (2026-09-22)
        val day2Repo = ReviewRepository(db, preferences, day2Clock)
        val day2Reviews = day2Repo.getReviewWords(tomorrow)
        assertEquals(1, day2Reviews.size)
        assertEquals(word.word, day2Reviews.first().word)

        // Quota automatically resets on day 2
        assertEquals(10, day2Repo.getRemainingQuota(tomorrow))
        assertFalse(day2Repo.isDailyLimitReached(tomorrow))
    }

    // Q. App restart persistence
    @Test
    fun testAppRestartPersistence() {
        val word = db.vocabularyDao().getWordsByLevel("toeic_basic").first()
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = word.word, learned = true, wrongCount = 2, nextReviewDate = today)
        )

        val q = reviewRepo.generateReviewQuiz(today, limit = 1).first()
        reviewRepo.commitReviewQuizSession(listOf(QuizAnswer(0, q.word, q.correctOption, true)))

        // Create new repository instance simulating app reload
        val newRepo = ReviewRepository(db, preferences, day1Clock)
        val p = db.wordProgressDao().getProgress(word.word)
        assertNotNull(p)
        assertEquals(1, p!!.correctCount)
        assertEquals(1, p.wrongCount)
        assertEquals(tomorrow, p.nextReviewDate)
        assertEquals(9, newRepo.getRemainingQuota(today))
    }

    // R. iOS parity vectors
    @Test
    fun testIOSParityVectors() {
        val word = db.vocabularyDao().getWordsByLevel("toeic_basic").first()

        // Vector 1: Initial state -> correct response (q=4)
        // EF: 2.5 + (0.1 - (5-4)*(0.08 + (5-4)*0.02)) = 2.5 + (0.1 - 1*0.1) = 2.5
        // Rep: 1, Interval: 1, nextReview: today + 1 day
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = word.word,
                learned = true,
                easinessFactor = 2.5,
                intervalDays = 0,
                repetitionCount = 0,
                nextReviewDate = today
            )
        )
        reviewRepo.commitReviewQuizSession(listOf(QuizAnswer(0, word.toWord(), word.translation, isCorrect = true)))
        var p = db.wordProgressDao().getProgress(word.word)!!
        assertEquals(2.5, p.easinessFactor, 0.001)
        assertEquals(1, p.repetitionCount)
        assertEquals(1, p.intervalDays)
        assertEquals(tomorrow, p.nextReviewDate)

        // Vector 2: Repetition 1 -> correct response (q=4)
        // Rep: 2, Interval: 6, nextReview: today + 6 days
        reviewRepo.commitReviewQuizSession(listOf(QuizAnswer(0, word.toWord(), word.translation, isCorrect = true)))
        p = db.wordProgressDao().getProgress(word.word)!!
        assertEquals(2.5, p.easinessFactor, 0.001)
        assertEquals(2, p.repetitionCount)
        assertEquals(6, p.intervalDays)
        assertEquals("2026-09-27", p.nextReviewDate)

        // Vector 3: Repetition 2 -> wrong response (q=1)
        // EF: 2.5 + (0.1 - (5-1)*(0.08 + (5-1)*0.02)) = 2.5 + (0.1 - 4*(0.16)) = 2.5 + (0.1 - 0.64) = 1.96
        // Rep: 0, Interval: 1, nextReview: today + 1 day
        reviewRepo.commitReviewQuizSession(listOf(QuizAnswer(0, word.toWord(), "incorrect_translation", isCorrect = false)))
        p = db.wordProgressDao().getProgress(word.word)!!
        assertEquals(1.96, p.easinessFactor, 0.001)
        assertEquals(0, p.repetitionCount)
        assertEquals(1, p.intervalDays)
        assertEquals(tomorrow, p.nextReviewDate)
    }
}
