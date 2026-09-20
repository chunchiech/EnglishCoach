package com.andy.englishcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.data.repository.ReviewRepository
import com.andy.englishcoach.ui.learning.DailyLearningScreen
import com.andy.englishcoach.ui.learning.DailyLearningViewModel
import com.andy.englishcoach.ui.quiz.QuizScreen
import com.andy.englishcoach.ui.quiz.QuizViewModel
import com.andy.englishcoach.ui.theme.EnglishCoachAndroidTheme
import com.andy.englishcoach.util.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var ttsManager: TtsManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = EnglishCoachDatabase.getInstance(applicationContext)
        val preferences = SharedPreferencesDailyLearningPreferences.create(applicationContext)
        ttsManager = TtsManager(applicationContext)
        val learningRepository = DailyLearningRepository(database, preferences)
        val quizRepository = QuizRepository(database, preferences, learningRepository)
        val reviewRepository = ReviewRepository(database, preferences)

        val learningViewModel = DailyLearningViewModel(learningRepository, ttsManager)
        val quizViewModel = QuizViewModel(quizRepository, ttsManager)
        val reviewViewModel = com.andy.englishcoach.ui.review.ReviewViewModel(reviewRepository, ttsManager)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                VocabularyDatabaseInitializer.initializeIfNeeded(applicationContext, database)
            }
            learningViewModel.loadTodayWords()
        }

        setContent {
            EnglishCoachAndroidTheme {
                var currentScreen by remember { mutableStateOf("learning") }

                when (currentScreen) {
                    "learning" -> {
                        DailyLearningScreen(
                            viewModel = learningViewModel,
                            onComplete = {
                                finish()
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
                                currentScreen = "learning"
                            },
                            onNavigateToReview = {
                                reviewViewModel.loadReviewWords()
                                currentScreen = "review"
                            }
                        )
                    }
                    "review" -> {
                        com.andy.englishcoach.ui.review.ReviewCenterScreen(
                            viewModel = reviewViewModel,
                            onNavigateBack = {
                                currentScreen = "learning"
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