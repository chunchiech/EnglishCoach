package com.andy.englishcoach.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.preference.InMemoryDailyLearningPreferences
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.data.repository.ReviewRepository
import com.andy.englishcoach.ui.learning.DailyLearningViewModel
import com.andy.englishcoach.ui.quiz.QuizViewModel
import com.andy.englishcoach.ui.review.ReviewViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Verification test suite for STEP 12-3 UI Polish.
 * Covers A through J scenarios for Learning, Quiz, and Review workflows.
 */
@RunWith(RobolectricTestRunner::class)
class UiPolishIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: EnglishCoachDatabase
    private lateinit var preferences: InMemoryDailyLearningPreferences
    private lateinit var learningRepo: DailyLearningRepository
    private lateinit var quizRepo: QuizRepository
    private lateinit var reviewRepo: ReviewRepository

    private val fixedZone = ZoneId.of("UTC")
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-21T09:00:00Z"), fixedZone)

    private fun createWord(id: Long, word: String, translation: String, phonetic: String, level: String): VocabularyEntity {
        return VocabularyEntity(
            id = id,
            word = word,
            phonetic = phonetic,
            translation = translation,
            example = "Example for $word",
            exampleTranslation = "例句 $translation",
            level = level,
            difficulty = 1,
            topic = "General",
            subtopic = "Daily",
            examTags = "TOEIC",
            partOfSpeech = "n."
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EnglishCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = InMemoryDailyLearningPreferences()
        learningRepo = DailyLearningRepository(db, preferences, fixedClock)
        quizRepo = QuizRepository(db, preferences, learningRepo, fixedClock)
        reviewRepo = ReviewRepository(db, preferences, fixedClock)

        val sampleWords = listOf(
            createWord(1, "apple", "蘋果", "/ˈæp.əl/", "toeic_basic"),
            createWord(2, "banana", "香蕉", "/bəˈnæn.ə/", "toeic_basic"),
            createWord(3, "negotiate", "談判", "/nəˈɡoʊ.ʃi.eɪt/", "toeic_advanced"),
            createWord(4, "conglomerate", "企業集團", "/kənˈɡlɑː.mɚ.ət/", "toeic_gold")
        )
        db.vocabularyDao().insertAll(sampleWords)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testA_dailyLearning_targetBadgeAndInitialState() {
        val vm = DailyLearningViewModel(learningRepo, null)
        val state = vm.uiState.value
        assertEquals(ToeicTarget.BASIC, state.target)
        assertEquals("550+ 基礎", state.target.displayName)
    }

    @Test
    fun testB_wordCard_flipToggleState() {
        val vm = DailyLearningViewModel(learningRepo, null)
        assertFalse(vm.uiState.value.isCardFlipped)

        vm.onFlipToMeaning()
        assertTrue(vm.uiState.value.isCardFlipped)

        vm.onCardClicked()
        assertFalse(vm.uiState.value.isCardFlipped)
    }

    @Test
    fun testC_quiz_questionGenerationAndSelection() {
        val questions = quizRepo.generateQuiz(ToeicTarget.BASIC, limit = 2)
        assertFalse(questions.isEmpty())
        assertEquals(2, questions.size)
        assertEquals(4, questions[0].options.size)
        assertTrue(questions[0].options.contains(questions[0].correctOption))
    }

    @Test
    fun testD_quizResult_scorecardCalculation() {
        val q = quizRepo.generateQuiz(ToeicTarget.BASIC, 2)
        val answers = listOf(
            QuizAnswer(questionIndex = 0, word = q[0].word, chosenOption = q[0].correctOption, isCorrect = true),
            QuizAnswer(questionIndex = 1, word = q[1].word, chosenOption = "wrong", isCorrect = false)
        )
        quizRepo.commitQuizSession(answers)

        val completed = quizRepo.isTodayQuizCompleted()
        assertTrue(completed)
        assertEquals(2, preferences.getDailyPracticeCount())
    }

    @Test
    fun testE_reviewCenter_listDisplayAndWrongCountBadge() {
        // Mark negotiate with 2 wrong counts
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = "negotiate", learned = true, wrongCount = 2, nextReviewDate = "2026-09-22")
        )
        learningRepo.setUserTargetLevel(ToeicTarget.ADVANCED)

        val reviewItems = reviewRepo.getReviewWordItems(targetLevel = ToeicTarget.ADVANCED)
        assertEquals(1, reviewItems.size)
        assertEquals(2, reviewItems[0].wrongCount)
        assertEquals(ToeicTarget.ADVANCED, reviewItems[0].word.target)
    }

    @Test
    fun testF_reviewQuiz_choicesAndAnswerFlow() {
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = "apple", learned = true, wrongCount = 1, nextReviewDate = "2026-09-22")
        )
        val reviewQuestions = reviewRepo.generateReviewQuiz(targetLevel = ToeicTarget.BASIC)
        assertEquals(1, reviewQuestions.size)
        assertEquals(4, reviewQuestions[0].options.size)
    }

    @Test
    fun testG_reviewResult_resolutionFlow() {
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = "apple", learned = true, wrongCount = 1, nextReviewDate = "2026-09-22")
        )
        val questions = reviewRepo.generateReviewQuiz(targetLevel = ToeicTarget.BASIC)
        val answer = QuizAnswer(questionIndex = 0, word = questions[0].word, chosenOption = questions[0].correctOption, isCorrect = true)
        val committed = reviewRepo.commitReviewQuizSession(listOf(answer))

        assertTrue(committed)
        val updated = db.wordProgressDao().getProgress("apple")
        assertNotNull(updated)
        assertEquals(0, updated?.wrongCount) // Decremented
    }

    @Test
    fun testH_targetIsolation_strictFiltering() {
        val basicWords = learningRepo.getTodayWords(ToeicTarget.BASIC)
        assertTrue(basicWords.all { it.level == "toeic_basic" })

        val advWords = learningRepo.getTodayWords(ToeicTarget.ADVANCED)
        assertTrue(advWords.all { it.level == "toeic_advanced" })
    }

    @Test
    fun testI_quotaTracking_enforcesLimits() {
        preferences.recordPracticeQuestions("2026-09-21", 10)
        assertTrue(reviewRepo.isDailyLimitReached())
        assertEquals(0, reviewRepo.getRemainingQuota())
    }

    @Test
    fun testJ_backNavigation_stateIntegrity() {
        // Confirm marking learning does not prematurely commit quiz
        learningRepo.markTodayLearningCompleted()
        assertTrue(learningRepo.isTodayLearningCompleted())
        assertFalse(quizRepo.isTodayQuizCompleted())
    }
}
