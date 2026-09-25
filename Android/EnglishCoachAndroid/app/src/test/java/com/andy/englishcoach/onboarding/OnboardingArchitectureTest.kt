package com.andy.englishcoach.onboarding

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.InMemoryDailyLearningPreferences
import com.andy.englishcoach.data.preference.SharedPreferencesDailyLearningPreferences
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.data.repository.ReviewRepository
import com.andy.englishcoach.notification.DailyReminderScheduler
import com.andy.englishcoach.onboarding.data.InMemoryOnboardingPreferences
import com.andy.englishcoach.onboarding.data.SharedPreferencesOnboardingPreferences
import com.andy.englishcoach.onboarding.model.AssessmentScoringPolicy
import com.andy.englishcoach.onboarding.model.LearningScenario
import com.andy.englishcoach.onboarding.model.PlacementQuestion
import com.andy.englishcoach.onboarding.model.ReminderTimeOption
import com.andy.englishcoach.ui.onboarding.OnboardingStep
import com.andy.englishcoach.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class OnboardingArchitectureTest {

    private lateinit var context: Context
    private lateinit var database: EnglishCoachDatabase
    private lateinit var learningPrefs: InMemoryDailyLearningPreferences
    private lateinit var learningRepository: DailyLearningRepository
    private lateinit var quizRepository: QuizRepository
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var onboardingPrefs: InMemoryOnboardingPreferences
    private lateinit var viewModel: OnboardingViewModel
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-22T08:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, EnglishCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        learningPrefs = InMemoryDailyLearningPreferences()
        learningRepository = DailyLearningRepository(database, learningPrefs, fixedClock)
        quizRepository = QuizRepository(database, learningPrefs, learningRepository)
        reviewRepository = ReviewRepository(database, learningPrefs, fixedClock)
        onboardingPrefs = InMemoryOnboardingPreferences()

        viewModel = OnboardingViewModel(
            onboardingPreferences = onboardingPrefs,
            learningRepository = learningRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // 1. Assessment 題庫 Source of Truth Verification (iOS PlacementQuestion Parity)
    // =========================================================================

    @Test
    fun test01_assessmentQuestionCountAndDistribution() {
        val questions = PlacementQuestion.TEST_QUESTIONS
        assertEquals("Total assessment questions must be exactly 20", 20, questions.size)

        val basicCount = questions.count { it.level == "toeic_basic" }
        val advancedCount = questions.count { it.level == "toeic_advanced" }
        val goldCount = questions.count { it.level == "toeic_gold" }

        assertEquals("550+ 基礎 tier must have 7 questions", 7, basicCount)
        assertEquals("750+ 進階 tier must have 7 questions", 7, advancedCount)
        assertEquals("860+ 金證 tier must have 6 questions", 6, goldCount)
    }

    @Test
    fun test02_assessmentQuestionsExactParityWithIos() {
        val questions = PlacementQuestion.TEST_QUESTIONS

        // Question 1: accept
        assertEquals(1, questions[0].id)
        assertEquals("accept", questions[0].word)
        assertEquals("/əkˈsept/", questions[0].phonetic)
        assertEquals("接受，同意", questions[0].correctAnswer)
        assertEquals("toeic_basic", questions[0].level)
        assertTrue(questions[0].options.contains("接受，同意"))

        // Question 7: agenda (last of basic tier)
        assertEquals(7, questions[6].id)
        assertEquals("agenda", questions[6].word)
        assertEquals("議程，討論事項", questions[6].correctAnswer)
        assertEquals("toeic_basic", questions[6].level)

        // Question 8: budget (first of advanced tier)
        assertEquals(8, questions[7].id)
        assertEquals("budget", questions[7].word)
        assertEquals("預算", questions[7].correctAnswer)
        assertEquals("toeic_advanced", questions[7].level)

        // Question 14: inventory (last of advanced tier)
        assertEquals(14, questions[13].id)
        assertEquals("inventory", questions[13].word)
        assertEquals("庫存，盤點", questions[13].correctAnswer)
        assertEquals("toeic_advanced", questions[13].level)

        // Question 15: negotiate (first of gold tier)
        assertEquals(15, questions[14].id)
        assertEquals("negotiate", questions[14].word)
        assertEquals("談判，協商", questions[14].correctAnswer)
        assertEquals("toeic_gold", questions[14].level)

        // Question 20: prerequisite (last of gold tier)
        assertEquals(20, questions[19].id)
        assertEquals("prerequisite", questions[19].word)
        assertEquals("先決條件，必備要素", questions[19].correctAnswer)
        assertEquals("toeic_gold", questions[19].level)

        // All questions must have 4 options and valid correct answer
        questions.forEach { q ->
            assertEquals("Each question must have 4 choices", 4, q.options.size)
            assertTrue("Correct answer must be among options", q.options.contains(q.correctAnswer))
        }
    }

    // =========================================================================
    // 2. Assessment Scoring Policy Tests
    // =========================================================================

    @Test
    fun test03_scoringPolicy_goldTier() {
        assertEquals(ToeicTarget.GOLD, AssessmentScoringPolicy.calculateRecommendedLevel(20))
        assertEquals(ToeicTarget.GOLD, AssessmentScoringPolicy.calculateRecommendedLevel(18))
        assertEquals(ToeicTarget.GOLD, AssessmentScoringPolicy.calculateRecommendedLevel(16))
    }

    @Test
    fun test04_scoringPolicy_advancedTier() {
        assertEquals(ToeicTarget.ADVANCED, AssessmentScoringPolicy.calculateRecommendedLevel(15))
        assertEquals(ToeicTarget.ADVANCED, AssessmentScoringPolicy.calculateRecommendedLevel(12))
        assertEquals(ToeicTarget.ADVANCED, AssessmentScoringPolicy.calculateRecommendedLevel(10))
    }

    @Test
    fun test05_scoringPolicy_basicTier() {
        assertEquals(ToeicTarget.BASIC, AssessmentScoringPolicy.calculateRecommendedLevel(9))
        assertEquals(ToeicTarget.BASIC, AssessmentScoringPolicy.calculateRecommendedLevel(5))
        assertEquals(ToeicTarget.BASIC, AssessmentScoringPolicy.calculateRecommendedLevel(0))
    }

    // =========================================================================
    // 3. Strict Quota, Review, and SM-2 Isolation Tests
    // =========================================================================

    @Test
    fun test06_assessmentDoesNotConsumeDailyQuota() = runBlocking {
        assertEquals(0, learningPrefs.getDailyPracticeCount())

        viewModel.startAssessment()

        // Answer 10 questions in assessment
        val questions = PlacementQuestion.TEST_QUESTIONS
        for (i in 0 until 10) {
            viewModel.answerAssessmentQuestion(questions[i].correctAnswer)
        }

        // Verify quota is completely untouched
        assertEquals("Assessment must not consume daily quota", 0, learningPrefs.getDailyPracticeCount())
        assertEquals("Assessment must not record daily practice date", null, learningPrefs.getDailyPracticeDate())
    }

    @Test
    fun test07_assessmentDoesNotCreateReviewRecordsOrSm2Entries() = runBlocking {
        viewModel.startAssessment()

        // Deliberately answer with incorrect options
        val questions = PlacementQuestion.TEST_QUESTIONS
        for (i in 0 until 5) {
            val wrongOption = questions[i].options.first { it != questions[i].correctAnswer }
            viewModel.answerAssessmentQuestion(wrongOption)
        }

        // Verify review records remain 0
        val reviewWords = reviewRepository.getReviewWords()
        assertEquals("Assessment mistakes must not create review records", 0, reviewWords.size)

        // No word progress altered
        assertEquals("Assessment mistakes must not create wrong words in WordProgress", 0, database.wordProgressDao().getWrongWords().size)
        assertEquals("Learned count must remain 0", 0, database.wordProgressDao().getLearnedCount())
    }

    // =========================================================================
    // 4. Learning Context Priority Persistence & Reordering Tests
    // =========================================================================

    @Test
    fun test08_learningContext_defaultOrder() {
        val defaultScenarios = onboardingPrefs.getSelectedLearningContexts()
        assertEquals(listOf("business", "meetings"), defaultScenarios)
        assertEquals("🎯 學習情境：職場商務 · 會議談判", onboardingPrefs.getScenarioDisplayText())
    }

    @Test
    fun test09_learningContext_toggleAndPreservePriorityOrder() {
        // Toggle 'travel' -> should be added to the end (priority 3)
        onboardingPrefs.toggleScenario("travel")
        val updated = onboardingPrefs.getSelectedLearningContexts()
        assertEquals(listOf("business", "meetings", "travel"), updated)
        assertEquals("🎯 學習情境：職場商務 · 會議談判 +1", onboardingPrefs.getScenarioDisplayText())

        // Toggle 'meetings' -> removed
        onboardingPrefs.toggleScenario("meetings")
        assertEquals(listOf("business", "travel"), onboardingPrefs.getSelectedLearningContexts())
        assertEquals("🎯 學習情境：職場商務 · 差旅觀光", onboardingPrefs.getScenarioDisplayText())
    }

    @Test
    fun test10_learningContext_cannotDeselectLastScenario() {
        val single = listOf("business")
        onboardingPrefs.setSelectedLearningContexts(single)

        // Try to toggle the last remaining scenario
        onboardingPrefs.toggleScenario("business")
        assertEquals("Must retain at least 1 scenario", listOf("business"), onboardingPrefs.getSelectedLearningContexts())
    }

    @Test
    fun test11_learningContext_moveUpAndMoveDown() {
        onboardingPrefs.setSelectedLearningContexts(listOf("business", "meetings", "travel"))

        // Move 'travel' up (from index 2 to index 1)
        onboardingPrefs.moveScenarioUp("travel")
        assertEquals(listOf("business", "travel", "meetings"), onboardingPrefs.getSelectedLearningContexts())

        // Move 'business' down (from index 0 to index 1)
        onboardingPrefs.moveScenarioDown("business")
        assertEquals(listOf("travel", "business", "meetings"), onboardingPrefs.getSelectedLearningContexts())
    }

    @Test
    fun test12_learningContext_relaunchRestoreOrderedList() {
        val sharedPrefs = context.getSharedPreferences("test_onboarding_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()

        val instance1 = SharedPreferencesOnboardingPreferences(sharedPrefs)
        instance1.setSelectedLearningContexts(listOf("career", "travel", "office"))

        // Simulate app restart by creating a new instance
        val instance2 = SharedPreferencesOnboardingPreferences(sharedPrefs)
        val restored = instance2.getSelectedLearningContexts()

        assertEquals("Priority order must be strictly preserved across restarts", listOf("career", "travel", "office"), restored)
    }

    // =========================================================================
    // 5. Notification Scheduling Tests
    // =========================================================================

    @Test
    fun test13_notificationScheduling_schedule1900DailyAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager: ShadowAlarmManager = Shadows.shadowOf(alarmManager)

        // Schedule at 19:00
        DailyReminderScheduler.scheduleDailyReminder(context, 19, 0)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Exactly 1 alarm should be scheduled", 1, scheduledAlarms.size)

        val alarm = scheduledAlarms[0]
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertEquals(AlarmManager.INTERVAL_DAY, alarm.interval)
        assertNotNull(alarm.operation)
    }

    @Test
    fun test14_notificationScheduling_updateTimeReplacesOldAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager: ShadowAlarmManager = Shadows.shadowOf(alarmManager)

        // Schedule at 08:00
        DailyReminderScheduler.scheduleDailyReminder(context, 8, 0)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Update to 21:00
        DailyReminderScheduler.scheduleDailyReminder(context, 21, 0)

        // ShadowAlarmManager replaces matching pendingIntent alarms
        assertEquals("Must only have 1 active scheduled alarm after update", 1, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun test15_notificationScheduling_cancelAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager: ShadowAlarmManager = Shadows.shadowOf(alarmManager)

        DailyReminderScheduler.scheduleDailyReminder(context, 19, 0)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        DailyReminderScheduler.cancelDailyReminder(context)
        assertEquals("Alarms must be 0 after cancellation", 0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun test21_notificationScheduling_duplicateCallsDoNotCreateMultipleAlarms() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager: ShadowAlarmManager = Shadows.shadowOf(alarmManager)

        // Call schedule 3 times consecutively with same time
        DailyReminderScheduler.scheduleDailyReminder(context, 19, 0)
        DailyReminderScheduler.scheduleDailyReminder(context, 19, 0)
        DailyReminderScheduler.scheduleDailyReminder(context, 19, 0)

        assertEquals("Multiple schedule calls must never create duplicate alarms", 1, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun test22_notificationScheduling_idempotentCancellation() {
        // Cancel when nothing is scheduled
        DailyReminderScheduler.cancelDailyReminder(context)
        DailyReminderScheduler.cancelDailyReminder(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager: ShadowAlarmManager = Shadows.shadowOf(alarmManager)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    // =========================================================================
    // 6. Complete Onboarding Flow & Relaunch Behavior Tests
    // =========================================================================

    @Test
    fun test16_freshInstall_startsAtReminderStep() {
        assertFalse("Fresh install must not have onboarding completed", onboardingPrefs.isOnboardingCompleted())
        assertEquals(OnboardingStep.REMINDER, viewModel.uiState.value.currentStep)
    }

    @Test
    fun test17_completeOnboardingFlow_persistsAllData() {
        // Step 1: Reminder
        viewModel.selectReminderOption("opt_21")
        viewModel.enableReminder(onScheduleAlarm = { _, _ -> }, onProceed = {})
        assertEquals(OnboardingStep.ASSESSMENT_INTRO, viewModel.uiState.value.currentStep)
        assertTrue(onboardingPrefs.isReminderEnabled())
        assertEquals(21, onboardingPrefs.getReminderHour())

        // Step 2: Skip Assessment to default
        viewModel.skipAssessmentToDefaultTarget()
        assertEquals(OnboardingStep.TARGET_SELECTION, viewModel.uiState.value.currentStep)
        assertEquals(ToeicTarget.BASIC, viewModel.uiState.value.selectedTarget)

        // Step 3: Change Target to 750+ ADVANCED
        viewModel.selectTarget(ToeicTarget.ADVANCED)
        viewModel.proceedToLearningContext()
        assertEquals(OnboardingStep.LEARNING_CONTEXT, viewModel.uiState.value.currentStep)

        // Step 4: Reorder learning context
        viewModel.moveScenarioUp("meetings") // now ["meetings", "business"]
        assertEquals(listOf("meetings", "business"), viewModel.uiState.value.selectedScenarioIds)

        // Step 5: Complete Onboarding
        var completed = false
        viewModel.completeOnboarding {
            completed = true
        }

        assertTrue(completed)
        assertTrue("Onboarding completion flag must be set", onboardingPrefs.isOnboardingCompleted())
        assertEquals("Target level must be saved in DailyLearningRepository", ToeicTarget.ADVANCED, learningRepository.getUserTargetLevel())
        assertEquals(listOf("meetings", "business"), onboardingPrefs.getSelectedLearningContexts())
    }

    @Test
    fun test18_backNavigationThroughOnboardingSteps() {
        // At Step 1
        assertEquals(OnboardingStep.REMINDER, viewModel.uiState.value.currentStep)
        assertFalse("At root step, back handler returns false to allow system back", viewModel.handleBackPress())

        // Move to Step 2
        viewModel.skipReminder(onProceed = {})
        assertEquals(OnboardingStep.ASSESSMENT_INTRO, viewModel.uiState.value.currentStep)

        // Back from Step 2 -> returns to Step 1
        assertTrue(viewModel.handleBackPress())
        assertEquals(OnboardingStep.REMINDER, viewModel.uiState.value.currentStep)

        // Move forward again to Quiz
        viewModel.skipReminder(onProceed = {})
        viewModel.startAssessment()
        assertEquals(OnboardingStep.ASSESSMENT_QUIZ, viewModel.uiState.value.currentStep)

        // Back from Quiz -> cancels to Intro
        assertTrue(viewModel.handleBackPress())
        assertEquals(OnboardingStep.ASSESSMENT_INTRO, viewModel.uiState.value.currentStep)
    }

    @Test
    fun test19_skipReminder_doesNotRequestAlarmAndContinues() {
        viewModel.skipReminder(
            onCancelAlarm = {},
            onProceed = {}
        )

        assertEquals(OnboardingStep.ASSESSMENT_INTRO, viewModel.uiState.value.currentStep)
        assertFalse("Reminder should be disabled when skipped", onboardingPrefs.isReminderEnabled())
    }

    @Test
    fun test20_targetLevelIsolation_preservesCorrectRawLevel() {
        viewModel.selectTarget(ToeicTarget.GOLD)
        viewModel.completeOnboarding {}

        assertEquals("toeic_gold", learningPrefs.getUserLevel())
        assertEquals(ToeicTarget.GOLD, learningRepository.getUserTargetLevel())
    }

    // =========================================================================
    // 7. Backup Isolation & Onboarding Preference Exemption Tests
    // =========================================================================

    @Test
    fun test21_caseA_freshInstall_preferenceMissing_initialScreenOnboarding() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val generalPrefs = appContext.getSharedPreferences("test_general_caseA", Context.MODE_PRIVATE)
        val onboardingPrefs = appContext.getSharedPreferences("test_onboarding_caseA", Context.MODE_PRIVATE)
        generalPrefs.edit().clear().commit()
        onboardingPrefs.edit().clear().commit()

        val preferences = SharedPreferencesOnboardingPreferences(
            prefs = generalPrefs,
            onboardingPrefs = onboardingPrefs
        )

        // Case A verification:
        assertFalse("When onboarding preference does not exist, isOnboardingCompleted must be false", preferences.isOnboardingCompleted())
        val initialScreen = if (preferences.isOnboardingCompleted()) "dashboard" else "onboarding"
        assertEquals("When isOnboardingCompleted is false, initialScreen must be 'onboarding'", "onboarding", initialScreen)
    }

    @Test
    fun test22_caseB_completedOnboarding_initialScreenDashboard() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val generalPrefs = appContext.getSharedPreferences("test_general_caseB", Context.MODE_PRIVATE)
        val onboardingPrefs = appContext.getSharedPreferences("test_onboarding_caseB", Context.MODE_PRIVATE)
        generalPrefs.edit().clear().commit()
        onboardingPrefs.edit().clear().commit()

        val preferences = SharedPreferencesOnboardingPreferences(
            prefs = generalPrefs,
            onboardingPrefs = onboardingPrefs
        )

        preferences.setOnboardingCompleted(true)

        // Case B verification:
        assertTrue("When onboarding is completed, isOnboardingCompleted must be true", preferences.isOnboardingCompleted())
        val initialScreen = if (preferences.isOnboardingCompleted()) "dashboard" else "onboarding"
        assertEquals("When isOnboardingCompleted is true, initialScreen must be 'dashboard'", "dashboard", initialScreen)
    }

    @Test
    fun test23_caseC_simulateGoogleBackupRestore_generalPreferencesRestored_onboardingNotRestored() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val generalPrefs = appContext.getSharedPreferences(SharedPreferencesOnboardingPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val onboardingPrefs = appContext.getSharedPreferences(SharedPreferencesOnboardingPreferences.ONBOARDING_PREFS_NAME, Context.MODE_PRIVATE)

        // Simulate state before fresh install:
        // Google Backup restores general preferences (e.g. englishcoach_preferences.xml with user settings & old legacy key)
        generalPrefs.edit()
            .putString("user_display_name", "Andy")
            .putInt("daily_learning_target", 20)
            .putInt(SharedPreferencesOnboardingPreferences.KEY_REMINDER_HOUR, 21)
            .putBoolean("hasCompletedPersonalizedOnboarding", true) // Legacy key from old backup
            .commit()

        // But onboardingPrefs (englishcoach_onboarding_preferences.xml) was excluded from backup, so it is NOT restored (empty).
        onboardingPrefs.edit().clear().commit()

        // Instantiate using standard production factory
        val preferences = SharedPreferencesOnboardingPreferences.create(appContext)

        // Case C verification:
        assertFalse("Onboarding completion state must NOT be restored from general preferences", preferences.isOnboardingCompleted())
        val initialScreen = if (preferences.isOnboardingCompleted()) "dashboard" else "onboarding"
        assertEquals("Fresh install after cloud restore must still show onboarding", "onboarding", initialScreen)

        // General settings must still be preserved
        assertEquals(21, preferences.getReminderHour())
        assertEquals("Andy", generalPrefs.getString("user_display_name", null))
        assertEquals(20, generalPrefs.getInt("daily_learning_target", 0))

        // Legacy key must have been sanitized from generalPrefs
        assertFalse("Legacy onboarding completion key must be stripped from generalPrefs", generalPrefs.contains("hasCompletedPersonalizedOnboarding"))
    }

    @Test
    fun test24_backupExclusionRulesXml_verifiesExclusionOfOnboardingPreferencesFile() {
        val rootDir = java.io.File(".").canonicalFile
        val candidatePaths = listOf(
            java.io.File(rootDir, "src/main/res/xml"),
            java.io.File(rootDir, "app/src/main/res/xml"),
            java.io.File(rootDir, "Android/EnglishCoachAndroid/app/src/main/res/xml")
        )
        val xmlDir = candidatePaths.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find res/xml in candidate paths: $candidatePaths")

        val dataExtractionRulesFile = java.io.File(xmlDir, "data_extraction_rules.xml")
        val backupRulesFile = java.io.File(xmlDir, "backup_rules.xml")

        assertTrue("data_extraction_rules.xml must exist", dataExtractionRulesFile.exists())
        assertTrue("backup_rules.xml must exist", backupRulesFile.exists())

        val dataExtractionContent = dataExtractionRulesFile.readText()
        val backupRulesContent = backupRulesFile.readText()

        // Verify Android 12+ cloud-backup and device-transfer exclusion
        assertTrue(
            "data_extraction_rules.xml must exclude onboarding preferences in cloud-backup",
            dataExtractionContent.contains("""<exclude domain="sharedpref" path="englishcoach_onboarding_preferences.xml" />""") ||
            dataExtractionContent.contains("""<exclude domain="sharedpref" path="englishcoach_onboarding_preferences.xml"/>""")
        )

        // Verify Android <31 full-backup-content exclusion
        assertTrue(
            "backup_rules.xml must exclude onboarding preferences",
            backupRulesContent.contains("""<exclude domain="sharedpref" path="englishcoach_onboarding_preferences.xml" />""") ||
            backupRulesContent.contains("""<exclude domain="sharedpref" path="englishcoach_onboarding_preferences.xml"/>""")
        )
    }
}
