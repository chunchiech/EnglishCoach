package com.andy.englishcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.VocabularyDatabaseInitializer
import com.andy.englishcoach.data.preference.SharedPreferencesDailyLearningPreferences
import com.andy.englishcoach.data.preference.SharedPreferencesSettingsPreferences
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.data.repository.ReviewRepository
import com.andy.englishcoach.ui.dashboard.DashboardScreen
import com.andy.englishcoach.ui.dashboard.DashboardViewModel
import com.andy.englishcoach.ui.learning.DailyLearningScreen
import com.andy.englishcoach.ui.learning.DailyLearningViewModel
import com.andy.englishcoach.ui.quiz.QuizScreen
import com.andy.englishcoach.ui.quiz.QuizViewModel
import com.andy.englishcoach.ui.review.ReviewCenterScreen
import com.andy.englishcoach.ui.review.ReviewViewModel
import com.andy.englishcoach.ui.settings.SettingsScreen
import com.andy.englishcoach.ui.settings.SettingsViewModel
import com.andy.englishcoach.ui.theme.EnglishCoachAndroidTheme
import com.andy.englishcoach.util.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainActivity serving as the application entry point and root host.
 * Initializes the database and routes between the Dashboard Home Hub and feature screens.
 */
class MainActivity : ComponentActivity() {

    private var ttsManager: TtsManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = EnglishCoachDatabase.getInstance(applicationContext)
        val preferences = SharedPreferencesDailyLearningPreferences.create(applicationContext)
        val settingsPreferences = SharedPreferencesSettingsPreferences.create(applicationContext)
        ttsManager = TtsManager(applicationContext)
        val learningRepository = DailyLearningRepository(database, preferences)
        val quizRepository = QuizRepository(database, preferences, learningRepository)
        val reviewRepository = ReviewRepository(database, preferences)

        val versionName = try {
            val pInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            "${pInfo.versionName} (${if (android.os.Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode})"
        } catch (_: Exception) {
            "1.0 (1)"
        }

        val dashboardViewModel = DashboardViewModel(
            learningRepository = learningRepository,
            quizRepository = quizRepository,
            reviewRepository = reviewRepository,
            preferences = preferences,
            database = database
        )
        val settingsViewModel = SettingsViewModel(
            settingsPreferences = settingsPreferences,
            learningPreferences = preferences,
            versionName = versionName
        )
        val learningViewModel = DailyLearningViewModel(learningRepository, ttsManager, settingsPreferences)
        val quizViewModel = QuizViewModel(quizRepository, ttsManager)
        val reviewViewModel = ReviewViewModel(reviewRepository, ttsManager)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                VocabularyDatabaseInitializer.initializeIfNeeded(applicationContext, database)
            }
            dashboardViewModel.refresh()
        }

        setContent {
            EnglishCoachAndroidTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                // System back gesture: return to Dashboard from child screens
                BackHandler(enabled = currentScreen != "dashboard") {
                    currentScreen = "dashboard"
                    dashboardViewModel.refresh()
                }

                when (currentScreen) {
                    "dashboard" -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onStartLearning = {
                                learningViewModel.loadTodayWords()
                                currentScreen = "learning"
                            },
                            onStartQuiz = {
                                quizViewModel.startQuiz()
                                currentScreen = "quiz"
                            },
                            onNavigateToReview = {
                                reviewViewModel.loadReviewWords()
                                currentScreen = "review"
                            },
                            onNavigateToSettings = {
                                settingsViewModel.refresh()
                                currentScreen = "settings"
                            }
                        )
                    }
                    "learning" -> {
                        DailyLearningScreen(
                            viewModel = learningViewModel,
                            onComplete = {
                                currentScreen = "dashboard"
                                dashboardViewModel.refresh()
                            },
                            onStartQuiz = {
                                quizViewModel.startQuiz()
                                currentScreen = "quiz"
                            },
                            onNavigateToReview = {
                                reviewViewModel.loadReviewWords()
                                currentScreen = "review"
                            }
                        )
                    }
                    "quiz" -> {
                        QuizScreen(
                            viewModel = quizViewModel,
                            onComplete = {
                                currentScreen = "dashboard"
                                dashboardViewModel.refresh()
                            },
                            onNavigateToReview = {
                                reviewViewModel.loadReviewWords()
                                currentScreen = "review"
                            }
                        )
                    }
                    "review" -> {
                        ReviewCenterScreen(
                            viewModel = reviewViewModel,
                            onNavigateBack = {
                                currentScreen = "dashboard"
                                dashboardViewModel.refresh()
                            }
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = {
                                currentScreen = "dashboard"
                                dashboardViewModel.refresh()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager?.shutdown()
        ttsManager = null
    }
}