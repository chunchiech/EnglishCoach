package com.andy.englishcoach.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.data.model.quiz.QuizQuestion

/**
 * Daily Quiz screen providing word testing, immediate answer feedback, and results transition.
 * Matches iOS EnglishCoach QuizView behavior and aesthetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onComplete: () -> Unit = {},
    onNavigateToReview: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val purpleColor = Color(0xFF8A2BE2)
    val blueColor = Color(0xFF1E90FF)
    val greenColor = Color(0xFF34C759)
    val redColor = Color(0xFFFF3B30)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "單字測驗",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "離開",
                            color = purpleColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = purpleColor, strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "準備測驗題目中...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                uiState.isSessionCompleted && uiState.result != null -> {
                    QuizResultScreen(
                        result = uiState.result!!,
                        onSpeakSentence = { viewModel.speakSentence(it) },
                        onBackToHome = onComplete,
                        onNavigateToReview = onNavigateToReview
                    )
                }
                uiState.questions.isEmpty() -> {
                    EmptyQuizView(purpleColor = purpleColor, onBack = onComplete)
                }
                else -> {
                    QuizQuestionContent(
                        currentQuestion = uiState.currentQuestion!!,
                        currentIndex = uiState.currentIndex,
                        totalQuestions = uiState.totalQuestions,
                        correctCount = uiState.correctAnswersCount,
                        selectedOption = uiState.selectedOption,
                        isAnswered = uiState.isAnswered,
                        isLastQuestion = uiState.isLastQuestion,
                        purpleColor = purpleColor,
                        blueColor = blueColor,
                        greenColor = greenColor,
                        redColor = redColor,
                        onOptionSelected = { viewModel.selectOption(it) },
                        onSpeakWord = { viewModel.speakWord(uiState.currentQuestion!!.word.word) },
                        onNextQuestion = { viewModel.nextQuestion() }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizQuestionContent(
    currentQuestion: QuizQuestion,
    currentIndex: Int,
    totalQuestions: Int,
    correctCount: Int,
    selectedOption: String?,
    isAnswered: Boolean,
    isLastQuestion: Boolean,
    purpleColor: Color,
    blueColor: Color,
    greenColor: Color,
    redColor: Color,
    onOptionSelected: (String) -> Unit,
    onSpeakWord: () -> Unit,
    onNextQuestion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "題目 ${currentIndex + 1} / $totalQuestions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = purpleColor
            )
            Text(
                text = "正確：$correctCount",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = greenColor
            )
        }

        // Question Word Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = currentQuestion.word.word,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (currentQuestion.word.phonetic.isNotBlank()) {
                        Text(
                            text = currentQuestion.word.phonetic,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif,
                            color = purpleColor
                        )
                    }
                }

                // Speaker icon button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(purpleColor)
                        .clickable(onClick = onSpeakWord),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔊", fontSize = 18.sp)
                }
            }
        }

        // 4 Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            currentQuestion.options.forEach { option ->
                val isCorrectChoice = option == currentQuestion.correctOption
                val isSelectedChoice = option == selectedOption

                val (bgColor, borderColor, textColor) = when {
                    isAnswered && isCorrectChoice -> Triple(
                        greenColor.copy(alpha = 0.15f),
                        greenColor,
                        greenColor
                    )
                    isAnswered && isSelectedChoice -> Triple(
                        redColor.copy(alpha = 0.15f),
                        redColor,
                        redColor
                    )
                    isAnswered -> Triple(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        Color.Transparent,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.onSurface
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isAnswered && (isCorrectChoice || isSelectedChoice)) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !isAnswered) {
                            onOptionSelected(option)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )

                        if (isAnswered) {
                            if (isCorrectChoice) {
                                Text(text = "✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = greenColor)
                            } else if (isSelectedChoice) {
                                Text(text = "✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = redColor)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action (Next button or spacer)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isAnswered) {
                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(purpleColor, blueColor)),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLastQuestion) "完成測驗" else "下一題",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyQuizView(purpleColor: Color, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📝", fontSize = 54.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "無可用於測驗的單字",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "請先學習今日單字以生成測驗題目。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = purpleColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "返回", fontWeight = FontWeight.Bold)
        }
    }
}
