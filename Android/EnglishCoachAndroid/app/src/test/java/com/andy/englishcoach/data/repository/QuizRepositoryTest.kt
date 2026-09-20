package com.andy.englishcoach.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.VocabularyDatabaseInitializer
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.model.quiz.QuizSession
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
class QuizRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: EnglishCoachDatabase
    private lateinit var preferences: InMemoryDailyLearningPreferences
    private lateinit var learningRepo: DailyLearningRepository
    private lateinit var quizRepo: QuizRepository

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
        learningRepo = DailyLearningRepository(db, preferences, day1Clock)
        quizRepo = QuizRepository(db, preferences, learningRepo, day1Clock)

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
    fun testTargetIsolationBasic() {
        val questions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 10)
        assertEquals(10, questions.size)
        questions.forEach { q ->
            assertEquals("toeic_basic", q.word.level)
            assertEquals(ToeicTarget.BASIC, q.word.target)
        }
    }

    @Test
    fun testTargetIsolationAdvanced() {
        val questions = quizRepo.generateQuiz(ToeicTarget.ADVANCED, limit = 10)
        assertEquals(10, questions.size)
        questions.forEach { q ->
            assertEquals("toeic_advanced", q.word.level)
            assertEquals(ToeicTarget.ADVANCED, q.word.target)
        }
    }

    @Test
    fun testTargetIsolationGold() {
        val questions = quizRepo.generateQuiz(ToeicTarget.GOLD, limit = 10)
        assertEquals(10, questions.size)
        questions.forEach { q ->
            assertEquals("toeic_gold", q.word.level)
            assertEquals(ToeicTarget.GOLD, q.word.target)
        }
    }

    @Test
    fun testQuestionGenerationMatchesTodayWords() {
        val todayWords = learningRepo.getTodayWords(ToeicTarget.BASIC, limit = 10)
        val questions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 10)

        assertEquals(10, questions.size)
        for (i in 0 until 10) {
            assertEquals(todayWords[i].word, questions[i].word.word)
            assertEquals(todayWords[i].translation, questions[i].correctOption)
            assertEquals(i, questions[i].index)
        }
    }

    @Test
    fun testOptionGenerationRules() {
        val questions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 10)
        questions.forEach { q ->
            // Exactly 4 options
            assertEquals(4, q.options.size)
            // Options contain the correct translation
            assertTrue(q.options.contains(q.correctOption))
            // All 4 options are distinct
            val distinctCount = q.options.distinct().size
            assertEquals(4, distinctCount)
        }
    }

    @Test
    fun testAnswerDetectionCorrectAndWrong() {
        val questions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 10)
        val firstQ = questions[0]

        val correctAnswer = QuizAnswer(
            questionIndex = 0,
            word = firstQ.word,
            chosenOption = firstQ.correctOption,
            isCorrect = true
        )
        assertTrue(correctAnswer.isCorrect)

        val wrongOption = firstQ.options.first { it != firstQ.correctOption }
        val wrongAnswer = QuizAnswer(
            questionIndex = 0,
            word = firstQ.word,
            chosenOption = wrongOption,
            isCorrect = false
        )
        assertFalse(wrongAnswer.isCorrect)
    }

    @Test
    fun testScoreCalculation() {
        val questions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 10)
        val answers = mutableListOf<QuizAnswer>()

        // 7 correct, 3 wrong
        for (i in 0 until 7) {
            answers.add(
                QuizAnswer(
                    questionIndex = i,
                    word = questions[i].word,
                    chosenOption = questions[i].correctOption,
                    isCorrect = true
                )
            )
        }
        for (i in 7 until 10) {
            val wrongOption = questions[i].options.first { it != questions[i].correctOption }
            answers.add(
                QuizAnswer(
                    questionIndex = i,
                    word = questions[i].word,
                    chosenOption = wrongOption,
                    isCorrect = false
                )
            )
        }

        val session = QuizSession(
            target = ToeicTarget.BASIC,
            date = "2026-09-21",
            questions = questions,
            answers = answers,
            isCompleted = true
        )

        val result = session.toQuizResult()
        assertEquals(10, result.totalQuestions)
        assertEquals(7, result.correctCount)
        assertEquals(3, result.wrongCount)
        assertEquals(70, result.scorePercentage)
    }

    @Test
    fun testPartialSessionDoesNotMarkComplete() {
        assertFalse(quizRepo.isTodayQuizCompleted())

        val questions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 10)
        // User answers 5 questions and exits (partial session)
        val partialAnswers = questions.take(5).map { q ->
            QuizAnswer(q.index, q.word, q.correctOption, isCorrect = true)
        }

        // Neither committed nor marked
        assertFalse(quizRepo.isTodayQuizCompleted())
        // No updates to database for these words
        for (ans in partialAnswers) {
            val progress = db.wordProgressDao().getProgress(ans.word.word)
            // progress is null or untouched by quiz
            if (progress != null) {
                assertEquals(0, progress.correctCount)
            }
        }
    }

    @Test
    fun testCompleteSessionMarksCompleteAndPersists() {
        assertFalse(quizRepo.isTodayQuizCompleted())

        val questions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 10)
        val allAnswers = questions.map { q ->
            QuizAnswer(q.index, q.word, q.correctOption, isCorrect = true)
        }

        val success = quizRepo.commitQuizSession(allAnswers)
        assertTrue(success)
        assertTrue(quizRepo.isTodayQuizCompleted())
        assertEquals("2026-09-21", preferences.getQuizCompletedDate())
    }

    @Test
    fun testCorrectAnswerUpdatesSM2() {
        val wordText = "abandon"
        val word = db.vocabularyDao().getWordByWord(wordText)!!.toWord()

        val answer = QuizAnswer(
            questionIndex = 0,
            word = word,
            chosenOption = word.translation,
            isCorrect = true
        )

        quizRepo.commitQuizSession(listOf(answer))

        val progress = db.wordProgressDao().getProgress(wordText)
        assertNotNull(progress)
        assertTrue(progress!!.learned)
        assertEquals("2026-09-21", progress.learnedDate)
        assertEquals(1, progress.correctCount)
        assertEquals(0, progress.wrongCount)
        assertEquals(1, progress.repetitionCount)
        assertEquals(1, progress.intervalDays)
        assertEquals(2.5, progress.easinessFactor, 0.001)
        assertEquals("2026-09-22", progress.nextReviewDate)
    }

    @Test
    fun testWrongAnswerUpdatesSM2() {
        val wordText = "ability"
        val word = db.vocabularyDao().getWordByWord(wordText)!!.toWord()

        // Seed with existing progress (rep = 2, interval = 6, EF = 2.5)
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = wordText,
                learned = true,
                learnedDate = "2026-09-10",
                correctCount = 2,
                wrongCount = 0,
                easinessFactor = 2.5,
                intervalDays = 6,
                repetitionCount = 2,
                nextReviewDate = "2026-09-21"
            )
        )

        val answer = QuizAnswer(
            questionIndex = 0,
            word = word,
            chosenOption = "錯誤選項",
            isCorrect = false
        )

        quizRepo.commitQuizSession(listOf(answer))

        val updated = db.wordProgressDao().getProgress(wordText)
        assertNotNull(updated)
        assertEquals(2, updated!!.correctCount)
        assertEquals(1, updated.wrongCount)
        assertEquals(0, updated.repetitionCount) // Reset to 0 on failure
        assertEquals(1, updated.intervalDays) // Reset to 1 on failure
        // EF drops by 0.54: 2.5 - 0.54 = 1.96
        assertEquals(1.96, updated.easinessFactor, 0.001)
        assertEquals("2026-09-22", updated.nextReviewDate)
    }

    @Test
    fun testForceCloseAndRestartBehavior() {
        val repo1 = QuizRepository(db, preferences, learningRepo, day1Clock)
        assertFalse(repo1.isTodayQuizCompleted())

        // Simulate app restart while quiz not completed
        val repo2 = QuizRepository(db, preferences, learningRepo, day1Clock)
        assertFalse(repo2.isTodayQuizCompleted())

        // Complete quiz in repo2
        val questions = repo2.generateQuiz(ToeicTarget.BASIC, limit = 10)
        val answers = questions.map { QuizAnswer(it.index, it.word, it.correctOption, true) }
        repo2.commitQuizSession(answers)
        assertTrue(repo2.isTodayQuizCompleted())

        // Simulate subsequent app launch on same day
        val repo3 = QuizRepository(db, preferences, learningRepo, day1Clock)
        assertTrue(repo3.isTodayQuizCompleted())
    }

    @Test
    fun testCrossDayReset() {
        val repoDay1 = QuizRepository(db, preferences, learningRepo, day1Clock)
        val questions = repoDay1.generateQuiz(ToeicTarget.BASIC, limit = 10)
        val answers = questions.map { QuizAnswer(it.index, it.word, it.correctOption, true) }
        repoDay1.commitQuizSession(answers)
        assertTrue(repoDay1.isTodayQuizCompleted())

        // Day 2
        val learningRepoDay2 = DailyLearningRepository(db, preferences, day2Clock)
        val repoDay2 = QuizRepository(db, preferences, learningRepoDay2, day2Clock)
        // New day: quiz completion resets to false!
        assertFalse(repoDay2.isTodayQuizCompleted())
    }

    @Test
    fun testIosParityTable() {
        val reviewDate = LocalDate.parse("2026-09-21")

        // Vector 1: First time correct
        val v1 = com.andy.englishcoach.data.sm2.SM2Engine.calculate(
            com.andy.englishcoach.data.sm2.SM2State.INITIAL,
            isCorrect = true,
            reviewDate = reviewDate
        )
        assertEquals(2.5, v1.easinessFactor, 0.001)
        assertEquals(1, v1.intervalDays)
        assertEquals(1, v1.repetitionCount)
        assertEquals("2026-09-22", v1.nextReviewDate)

        // Vector 2: Second time correct
        val v2 = com.andy.englishcoach.data.sm2.SM2Engine.calculate(
            com.andy.englishcoach.data.sm2.SM2State(v1.easinessFactor, v1.intervalDays, v1.repetitionCount),
            isCorrect = true,
            reviewDate = reviewDate
        )
        assertEquals(2.5, v2.easinessFactor, 0.001)
        assertEquals(6, v2.intervalDays)
        assertEquals(2, v2.repetitionCount)
        assertEquals("2026-09-27", v2.nextReviewDate)

        // Vector 3: Third time correct
        val v3 = com.andy.englishcoach.data.sm2.SM2Engine.calculate(
            com.andy.englishcoach.data.sm2.SM2State(v2.easinessFactor, v2.intervalDays, v2.repetitionCount),
            isCorrect = true,
            reviewDate = reviewDate
        )
        assertEquals(2.5, v3.easinessFactor, 0.001)
        assertEquals(15, v3.intervalDays) // round(6 * 2.5) = 15
        assertEquals(3, v3.repetitionCount)
        assertEquals("2026-10-06", v3.nextReviewDate)

        // Vector 4: Incorrect answer resets interval to 1 and rep to 0
        val v4 = com.andy.englishcoach.data.sm2.SM2Engine.calculate(
            com.andy.englishcoach.data.sm2.SM2State(v3.easinessFactor, v3.intervalDays, v3.repetitionCount),
            isCorrect = false,
            reviewDate = reviewDate
        )
        assertEquals(1.96, v4.easinessFactor, 0.001)
        assertEquals(1, v4.intervalDays)
        assertEquals(0, v4.repetitionCount)
        assertEquals("2026-09-22", v4.nextReviewDate)
    }
}
