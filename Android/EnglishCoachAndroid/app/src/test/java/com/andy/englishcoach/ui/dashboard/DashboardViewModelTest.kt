package com.andy.englishcoach.ui.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.database.entity.WordProgressEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.InMemoryDailyLearningPreferences
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.data.repository.ReviewRepository
import com.andy.englishcoach.billing.DailyTargetPolicy
import com.andy.englishcoach.billing.FakeBillingRepository
import com.andy.englishcoach.billing.PremiumEntitlement
import com.andy.englishcoach.billing.PremiumProduct
import com.andy.englishcoach.data.preference.InMemorySettingsPreferences
import com.andy.englishcoach.data.preference.SettingsPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class DashboardViewModelTest {

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

        // Seed sample vocabulary across levels
        val sampleWords = listOf(
            createWord(1, "apple", "蘋果", "/ˈæp.əl/", "toeic_basic"),
            createWord(2, "banana", "香蕉", "/bəˈnæn.ə/", "toeic_basic"),
            createWord(3, "cherry", "櫻桃", "/ˈtʃer.i/", "toeic_basic"),
            createWord(4, "negotiate", "談判", "/nəˈɡoʊ.ʃi.eɪt/", "toeic_advanced"),
            createWord(5, "revenue", "營收", "/ˈrev.ə.nuː/", "toeic_advanced"),
            createWord(6, "conglomerate", "企業集團", "/kənˈɡlɑː.mɚ.ət/", "toeic_gold")
        )
        db.vocabularyDao().insertAll(sampleWords)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun createViewModel(
        isPremium: Boolean = false,
        settingsPrefs: SettingsPreferences = InMemorySettingsPreferences()
    ): DashboardViewModel {
        val fakeBilling = FakeBillingRepository()
        if (isPremium) {
            fakeBilling.setEntitlementForTesting(PremiumEntitlement.Premium(PremiumProduct.ANNUAL))
        }
        return DashboardViewModel(
            learningRepository = learningRepo,
            quizRepository = quizRepo,
            reviewRepository = reviewRepo,
            preferences = preferences,
            database = db,
            clock = fixedClock,
            entitlementProvider = fakeBilling,
            settingsPreferences = settingsPrefs
        )
    }

    @Test
    fun testA_freshInstall_defaultsToDashboardAndBasicTarget() {
        val vm = createViewModel()
        val state = vm.calculateDashboardState()

        assertEquals(ToeicTarget.BASIC, state.targetLevel)
        assertEquals("早安", state.greeting)
        assertEquals(3, state.totalWords)
        assertEquals(0, state.learnedWords)
        assertEquals(0, state.todayQuizCompletedCount)
        assertEquals(0, state.dailyPracticeQuotaUsed)
        assertEquals(0f, state.todayProgress, 0.001f)
        assertEquals("開始今日練習", state.ctaTitle)
        assertEquals(DashboardCtaAction.START_LEARNING, state.ctaAction)
    }

    @Test
    fun testB_targetLevelDefaultsCorrectly() {
        assertEquals("toeic_basic", preferences.getUserLevel())
        assertEquals(ToeicTarget.BASIC, learningRepo.getUserTargetLevel())
    }

    @Test
    fun testC_targetLevelPersistence() {
        val vm = createViewModel()
        vm.setTargetLevel(ToeicTarget.ADVANCED)

        assertEquals("toeic_advanced", preferences.getUserLevel())
        assertEquals(ToeicTarget.ADVANCED, learningRepo.getUserTargetLevel())

        vm.setTargetLevel(ToeicTarget.GOLD)
        assertEquals("toeic_gold", preferences.getUserLevel())
        assertEquals(ToeicTarget.GOLD, learningRepo.getUserTargetLevel())
    }

    @Test
    fun testD_basicAdvancedGoldIsolation() {
        val vm = createViewModel()

        // 1. Basic (3 words)
        val basicState = vm.calculateDashboardState()
        assertEquals(ToeicTarget.BASIC, basicState.targetLevel)
        assertEquals(3, basicState.totalWords)

        // 2. Advanced (2 words)
        vm.setTargetLevel(ToeicTarget.ADVANCED)
        val advState = vm.calculateDashboardState()
        assertEquals(ToeicTarget.ADVANCED, advState.targetLevel)
        assertEquals(2, advState.totalWords)

        // 3. Gold (1 word)
        vm.setTargetLevel(ToeicTarget.GOLD)
        val goldState = vm.calculateDashboardState()
        assertEquals(ToeicTarget.GOLD, goldState.targetLevel)
        assertEquals(1, goldState.totalWords)
    }

    @Test
    fun testE_dailyLearningCTA() {
        val vm = createViewModel()
        val state = vm.calculateDashboardState()

        assertFalse(state.isLearningCompleted)
        assertEquals("開始今日練習", state.ctaTitle)
        assertEquals(DashboardCtaAction.START_LEARNING, state.ctaAction)
    }

    @Test
    fun testF_quizCTA_whenLearningDoneAwaitingQuiz() {
        learningRepo.markTodayLearningCompleted()

        val vm = createViewModel()
        val state = vm.calculateDashboardState()

        assertTrue(state.isLearningCompleted)
        assertFalse(state.isQuizCompleted)
        assertEquals("開始今日測驗 ➜", state.ctaTitle)
        assertEquals(DashboardCtaAction.START_QUIZ, state.ctaAction)
    }

    @Test
    fun testG_completedState_whenBothLearningAndQuizDone() {
        learningRepo.markTodayLearningCompleted()
        quizRepo.markTodayQuizCompleted()
        preferences.recordPracticeQuestions("2026-09-21", 10)

        val vm = createViewModel()
        val state = vm.calculateDashboardState()

        assertTrue(state.isLearningCompleted)
        assertTrue(state.isQuizCompleted)
        assertEquals(10, state.todayQuizCompletedCount)
        assertEquals(10, state.dailyPracticeQuotaUsed)
        assertEquals(1.0f, state.todayProgress, 0.001f)
        assertEquals("🎉 今日學習已完成", state.ctaTitle)
        assertEquals(DashboardCtaAction.COMPLETED, state.ctaAction)
    }

    @Test
    fun testH_reviewCount_strictlyFollowsTargetLevel() {
        // Mark apple (basic) with wrong_count = 1
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = "apple",
                learned = true,
                wrongCount = 1,
                nextReviewDate = "2026-09-22"
            )
        )
        // Mark negotiate (advanced) with wrong_count = 2
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(
                word = "negotiate",
                learned = true,
                wrongCount = 2,
                nextReviewDate = "2026-09-22"
            )
        )

        val vm = createViewModel()

        // In Basic target: only 1 review word (apple)
        val basicState = vm.calculateDashboardState()
        assertEquals(1, basicState.reviewCount)

        // In Advanced target: only 1 review word (negotiate)
        vm.setTargetLevel(ToeicTarget.ADVANCED)
        val advState = vm.calculateDashboardState()
        assertEquals(1, advState.reviewCount)

        // In Gold target: 0 review words
        vm.setTargetLevel(ToeicTarget.GOLD)
        val goldState = vm.calculateDashboardState()
        assertEquals(0, goldState.reviewCount)
    }

    @Test
    fun testI_quotaDisplay_separatesQuizAndPracticeQuota() {
        val vm = createViewModel()

        // Scenario 1: User does 5 review questions (not daily quiz)
        preferences.recordPracticeQuestions("2026-09-21", 5)
        var state = vm.calculateDashboardState()

        assertEquals(5, state.todayQuizCompletedCount) // 5 of 10 completed
        assertEquals(5, state.dailyPracticeQuotaUsed) // Practice quota consumed
        assertEquals(0.5f, state.todayProgress, 0.001f)

        // Scenario 2: User completes daily quiz
        quizRepo.markTodayQuizCompleted()
        preferences.recordPracticeQuestions("2026-09-21", 5) // Now total 10
        state = vm.calculateDashboardState()

        assertEquals(10, state.todayQuizCompletedCount)
        assertEquals(10, state.dailyPracticeQuotaUsed)
        assertEquals(1.0f, state.todayProgress, 0.001f)
        assertTrue(state.isDailyLimitReached)
    }

    @Test
    fun testJ_restartPersistence() {
        // Setup initial state: target GOLD, learning and quiz completed
        preferences.setUserLevel(ToeicTarget.GOLD.rawLevel)
        learningRepo.markTodayLearningCompleted()
        quizRepo.markTodayQuizCompleted()
        preferences.recordPracticeQuestions("2026-09-21", 10)

        // Simulate app restart by instantiating new ViewModel
        val restartedVm = createViewModel()
        val state = restartedVm.calculateDashboardState()

        assertEquals(ToeicTarget.GOLD, state.targetLevel)
        assertTrue(state.isLearningCompleted)
        assertTrue(state.isQuizCompleted)
        assertEquals("🎉 今日學習已完成", state.ctaTitle)
        assertEquals(DashboardCtaAction.COMPLETED, state.ctaAction)
    }

    @Test
    fun testStatistics_accuracyCalculation() {
        db.wordProgressDao().insertOrUpdate(
            WordProgressEntity(word = "apple", learned = true, correctCount = 8, wrongCount = 2)
        )
        val vm = createViewModel()
        val state = vm.calculateDashboardState()

        // 8 / (8 + 2) = 80.0%
        assertEquals(80.0, state.accuracy, 0.01)
        assertEquals(1, state.learnedWords)
    }

    @Test
    fun testK_dailyTarget_defaultsTo10_andReflectedInState() {
        val vm = createViewModel()
        val state = vm.calculateDashboardState()

        assertEquals(10, state.dailyTarget)
        assertEquals(10, state.maxQuizCount)
        assertFalse(state.isUnlimitedTarget)
        assertEquals("10 題", state.dailyTargetText)
        assertEquals(listOf(5, 10, 20, 30, 50, 100, -1), state.availableDailyTargets)
    }

    @Test
    fun testL_selectDailyTarget_whenPremium_updatesStateAndPreferences() {
        val settingsPrefs = InMemorySettingsPreferences()
        val vm = createViewModel(isPremium = true, settingsPrefs = settingsPrefs)

        // 1. Select 20
        val result20 = vm.selectDailyTarget(20)
        assertTrue(result20)
        var state = vm.calculateDashboardState()
        assertEquals(20, state.dailyTarget)
        assertEquals(20, state.maxQuizCount)
        assertEquals("20 題", state.dailyTargetText)
        assertEquals(20, settingsPrefs.getDailyTarget())

        // 2. Select Unlimited (-1)
        val resultUnlimited = vm.selectDailyTarget(DailyTargetPolicy.UNLIMITED_TARGET)
        assertTrue(resultUnlimited)
        state = vm.calculateDashboardState()
        assertEquals(DailyTargetPolicy.UNLIMITED_TARGET, state.dailyTarget)
        assertTrue(state.isUnlimitedTarget)
        assertEquals("不限", state.dailyTargetText)
        assertEquals(DailyTargetPolicy.UNLIMITED_TARGET, settingsPrefs.getDailyTarget())

        // 3. Select 100
        val result100 = vm.selectDailyTarget(100)
        assertTrue(result100)
        state = vm.calculateDashboardState()
        assertEquals(100, state.dailyTarget)
        assertEquals(100, state.maxQuizCount)
        assertEquals("100 題", state.dailyTargetText)
    }

    @Test
    fun testM_selectDailyTarget_whenFree_restrictsTo10() {
        val settingsPrefs = InMemorySettingsPreferences()
        val vm = createViewModel(isPremium = false, settingsPrefs = settingsPrefs)

        // Disallowed targets return false and do not update
        assertFalse(vm.selectDailyTarget(20))
        assertEquals(10, settingsPrefs.getDailyTarget())

        assertFalse(vm.selectDailyTarget(100))
        assertEquals(10, settingsPrefs.getDailyTarget())

        assertFalse(vm.selectDailyTarget(DailyTargetPolicy.UNLIMITED_TARGET))
        assertEquals(10, settingsPrefs.getDailyTarget())

        // 10 is allowed
        assertTrue(vm.selectDailyTarget(10))
        val state = vm.calculateDashboardState()
        assertEquals(10, state.dailyTarget)
        assertEquals(10, state.maxQuizCount)
    }

    @Test
    fun testN_dailyTargetSheetVisibility_togglesCorrectly() {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.showDailyTargetSheet)

        vm.setDailyTargetSheetVisible(true)
        assertTrue(vm.uiState.value.showDailyTargetSheet)

        vm.setDailyTargetSheetVisible(false)
        assertFalse(vm.uiState.value.showDailyTargetSheet)
    }

    // ---------------------------------------------------------
    // User Specified Scenarios: Tests 1 - 6
    // ---------------------------------------------------------

    @Test
    fun test1_dailyGoal10_completed0() {
        val vm = createViewModel(isPremium = false)
        val state = vm.calculateDashboardState()

        assertEquals(10, state.dailyTarget)
        assertEquals(0, state.todayQuizCompletedCount)
        assertEquals(0, state.dailyPracticeQuotaUsed)
        assertFalse(state.isDailyLimitReached)
        assertEquals("開始今日練習", state.ctaTitle)
        assertEquals(DashboardCtaAction.START_LEARNING, state.ctaAction)
    }

    @Test
    fun test2_dailyGoal10_completed10() {
        learningRepo.markTodayLearningCompleted()
        quizRepo.markTodayQuizCompleted()
        preferences.recordPracticeQuestions("2026-09-21", 10)

        val vm = createViewModel(isPremium = false)
        val state = vm.calculateDashboardState()

        assertEquals(10, state.dailyTarget)
        assertEquals(10, state.todayQuizCompletedCount)
        assertEquals(10, state.dailyPracticeQuotaUsed)
        assertTrue(state.isDailyLimitReached)
        assertEquals("🎉 今日學習已完成", state.ctaTitle)
        assertEquals(DashboardCtaAction.COMPLETED, state.ctaAction)
    }

    @Test
    fun test3_dailyGoal10_completed10_upgradePremium_dailyGoal20() {
        // Step 1: User completed 10/10 as Free
        learningRepo.markTodayLearningCompleted()
        quizRepo.markTodayQuizCompleted()
        preferences.recordPracticeQuestions("2026-09-21", 10)

        // Step 2: User upgrades to Premium and changes dailyGoal to 20
        val settingsPrefs = InMemorySettingsPreferences()
        val fakeBilling = FakeBillingRepository()
        fakeBilling.setEntitlementForTesting(PremiumEntitlement.Premium(PremiumProduct.ANNUAL))

        val vm = DashboardViewModel(
            learningRepository = learningRepo,
            quizRepository = quizRepo,
            reviewRepository = reviewRepo,
            preferences = preferences,
            database = db,
            clock = fixedClock,
            entitlementProvider = fakeBilling,
            settingsPreferences = settingsPrefs
        )

        val selectResult = vm.selectDailyTarget(20)
        assertTrue(selectResult)

        val state = vm.calculateDashboardState()

        // Requirements:
        // todayCompleted = 10
        // dailyGoal = 20
        // remaining = 10
        // UI displays: 10 / 20, can continue learning 10 words
        assertEquals(20, state.dailyTarget)
        assertEquals(10, state.todayQuizCompletedCount)
        assertEquals(10, state.dailyPracticeQuotaUsed)
        assertFalse(state.isDailyLimitReached)
        assertEquals(0.5f, state.todayProgress, 0.001f)
        assertEquals("繼續今日學習 ➜", state.ctaTitle)
        assertEquals(DashboardCtaAction.START_LEARNING, state.ctaAction)
    }

    @Test
    fun test4_dailyGoal20_completed10() {
        preferences.recordPracticeQuestions("2026-09-21", 10)
        val settingsPrefs = InMemorySettingsPreferences().apply { setDailyTarget(20) }

        val vm = createViewModel(isPremium = true, settingsPrefs = settingsPrefs)
        val state = vm.calculateDashboardState()

        assertEquals(20, state.dailyTarget)
        assertEquals(10, state.todayQuizCompletedCount)
        assertEquals(10, state.dailyPracticeQuotaUsed)
        assertFalse(state.isDailyLimitReached)
        assertEquals("繼續今日學習 ➜", state.ctaTitle)
        assertEquals(DashboardCtaAction.START_LEARNING, state.ctaAction)
    }

    @Test
    fun test5_dailyGoal20_completed20() {
        learningRepo.markTodayLearningCompleted()
        quizRepo.markTodayQuizCompleted()
        preferences.recordPracticeQuestions("2026-09-21", 20)
        val settingsPrefs = InMemorySettingsPreferences().apply { setDailyTarget(20) }

        val vm = createViewModel(isPremium = true, settingsPrefs = settingsPrefs)
        val state = vm.calculateDashboardState()

        assertEquals(20, state.dailyTarget)
        assertEquals(20, state.todayQuizCompletedCount)
        assertEquals(20, state.dailyPracticeQuotaUsed)
        assertTrue(state.isDailyLimitReached)
        assertEquals(1.0f, state.todayProgress, 0.001f)
        assertEquals("🎉 今日學習已完成", state.ctaTitle)
        assertEquals(DashboardCtaAction.COMPLETED, state.ctaAction)
    }

    @Test
    fun test6_modifyDailyGoal_restartApp_statePreserved() {
        // Step 1: User completed 10 questions today
        preferences.recordPracticeQuestions("2026-09-21", 10)
        val settingsPrefs = InMemorySettingsPreferences()
        val fakeBilling = FakeBillingRepository()
        fakeBilling.setEntitlementForTesting(PremiumEntitlement.Premium(PremiumProduct.ANNUAL))

        var vm = DashboardViewModel(
            learningRepository = learningRepo,
            quizRepository = quizRepo,
            reviewRepository = reviewRepo,
            preferences = preferences,
            database = db,
            clock = fixedClock,
            entitlementProvider = fakeBilling,
            settingsPreferences = settingsPrefs
        )
        vm.selectDailyTarget(20)

        // Step 2: Simulate App Restart with persisted preferences & DB
        val restartedVm = DashboardViewModel(
            learningRepository = learningRepo,
            quizRepository = quizRepo,
            reviewRepository = reviewRepo,
            preferences = preferences,
            database = db,
            clock = fixedClock,
            entitlementProvider = fakeBilling,
            settingsPreferences = settingsPrefs
        )
        val state = restartedVm.calculateDashboardState()

        assertEquals(20, state.dailyTarget)
        assertEquals(10, state.todayQuizCompletedCount)
        assertEquals(10, state.dailyPracticeQuotaUsed)
        assertFalse(state.isDailyLimitReached)
        assertEquals("繼續今日學習 ➜", state.ctaTitle)
        assertEquals(DashboardCtaAction.START_LEARNING, state.ctaAction)
    }

    @Test
    fun test7_learningCompleted10_quizNotCompleted_dashboardStateCompliesWithProductSpec() {
        // Free user, target = 10, completes 10 learning cards
        learningRepo.markTodayLearningCompleted()

        // Quiz is NOT yet completed, no practice quota consumed yet
        assertFalse(quizRepo.isTodayQuizCompleted())
        assertEquals(0, preferences.getDailyPracticeCount())
        assertEquals(0, preferences.getDailyQuizCount())

        val vm = createViewModel()
        val state = vm.calculateDashboardState()

        // 1. Completed Counts
        // Today quiz completed count must be 0 (user has not answered quiz questions yet)
        assertEquals(0, state.todayQuizCompletedCount)
        // Daily practice quota used must be 0 (browsing/learning cards does not consume quota)
        assertEquals(0, state.dailyPracticeQuotaUsed)

        // 2. Remaining Counts
        val remainingFreeQuota = state.maxPracticeQuota - state.dailyPracticeQuotaUsed
        assertEquals(10, remainingFreeQuota)
        val remainingTargetQuestions = state.dailyTarget - state.todayQuizCompletedCount
        assertEquals(10, remainingTargetQuestions)
        assertFalse(state.isDailyLimitReached)

        // 3. Status & CTA
        assertTrue(state.isLearningCompleted)
        assertFalse(state.isQuizCompleted)
        assertEquals("開始今日測驗 ➜", state.ctaTitle)
        assertEquals(DashboardCtaAction.START_QUIZ, state.ctaAction)
        assertEquals(0f, state.todayProgress, 0.001f)
    }
}
