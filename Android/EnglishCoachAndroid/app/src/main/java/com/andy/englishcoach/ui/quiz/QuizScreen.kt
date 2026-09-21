package com.andy.englishcoach.ui.quiz

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.data.model.quiz.QuizQuestion
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachGradients
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

/**
 * Daily Quiz screen providing word testing, immediate answer feedback, and results transition.
 * Polished with EnglishCoach Design System tokens and vector icons.
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "單字測驗",
                        style = EnglishCoachTypography.screenTitle.copy(
                            fontSize = 18.sp
                        ),
                        color = EnglishCoachColors.TextPrimary
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "離開",
                            style = EnglishCoachTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = EnglishCoachColors.Purple
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EnglishCoachColors.Background
                )
            )
        },
        containerColor = EnglishCoachColors.Background
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
                        CircularProgressIndicator(
                            color = EnglishCoachColors.Purple,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(EnglishCoachSpacing.lg))
                        Text(
                            text = "準備測驗題目中...",
                            style = EnglishCoachTypography.body,
                            color = EnglishCoachColors.TextSecondary
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
                    EmptyQuizView(onBack = onComplete)
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
    onOptionSelected: (String) -> Unit,
    onSpeakWord: () -> Unit,
    onNextQuestion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EnglishCoachSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "題目 ${currentIndex + 1} / $totalQuestions",
                style = EnglishCoachTypography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = EnglishCoachColors.Purple
            )
            Text(
                text = "正確：$correctCount",
                style = EnglishCoachTypography.caption.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = EnglishCoachColors.Green
            )
        }

        // Question Word Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = EnglishCoachShapes.secondaryCard,
            colors = CardDefaults.cardColors(
                containerColor = EnglishCoachColors.Surface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(EnglishCoachSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxs))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
                ) {
                    Text(
                        text = currentQuestion.word.word,
                        style = EnglishCoachTypography.cardWord.copy(
                            fontSize = 34.sp
                        ),
                        color = EnglishCoachColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    if (currentQuestion.word.phonetic.isNotBlank()) {
                        Text(
                            text = currentQuestion.word.phonetic,
                            style = EnglishCoachTypography.ipa,
                            color = EnglishCoachColors.Purple
                        )
                    }
                }

                // Speaker icon button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(EnglishCoachGradients.purpleBlue)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "朗讀單字",
                            onClick = onSpeakWord
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = EnglishCoachIcons.Volume,
                        contentDescription = "朗讀單字",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4 Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            currentQuestion.options.forEach { option ->
                val isCorrectChoice = option == currentQuestion.correctOption
                val isSelectedChoice = option == selectedOption

                val (bgColor, borderColor, textColor) = when {
                    isAnswered && isCorrectChoice -> Triple(
                        EnglishCoachColors.Green.copy(alpha = 0.12f),
                        EnglishCoachColors.Green,
                        EnglishCoachColors.Green
                    )
                    isAnswered && isSelectedChoice -> Triple(
                        EnglishCoachColors.Red.copy(alpha = 0.12f),
                        EnglishCoachColors.Red,
                        EnglishCoachColors.Red
                    )
                    isAnswered -> Triple(
                        EnglishCoachColors.Surface.copy(alpha = 0.6f),
                        Color.Transparent,
                        EnglishCoachColors.TextSecondary.copy(alpha = 0.5f)
                    )
                    else -> Triple(
                        EnglishCoachColors.Surface,
                        EnglishCoachColors.Border.copy(alpha = 0.6f),
                        EnglishCoachColors.TextPrimary
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(EnglishCoachShapes.option)
                        .border(
                            width = if (isAnswered && (isCorrectChoice || isSelectedChoice)) 2.dp else 1.dp,
                            color = borderColor,
                            shape = EnglishCoachShapes.option
                        )
                        .clickable(
                            role = Role.RadioButton,
                            enabled = !isAnswered,
                            onClick = { onOptionSelected(option) }
                        ),
                    shape = EnglishCoachShapes.option,
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EnglishCoachSpacing.xl, vertical = EnglishCoachSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            style = EnglishCoachTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = textColor
                        )

                        if (isAnswered) {
                            if (isCorrectChoice) {
                                Icon(
                                    imageVector = EnglishCoachIcons.CheckCircle,
                                    contentDescription = "正確",
                                    tint = EnglishCoachColors.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isSelectedChoice) {
                                Icon(
                                    imageVector = EnglishCoachIcons.CloseCircle,
                                    contentDescription = "錯誤",
                                    tint = EnglishCoachColors.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action (Next button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = EnglishCoachSpacing.xxl)
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isAnswered) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(EnglishCoachShapes.button)
                        .background(EnglishCoachGradients.purpleBlue)
                        .clickable(
                            role = Role.Button,
                            onClick = onNextQuestion
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
                    ) {
                        Text(
                            text = if (isLastQuestion) "查看測驗結果" else "下一題",
                            style = EnglishCoachTypography.button,
                            color = Color.White
                        )
                        Icon(
                            imageVector = if (isLastQuestion) EnglishCoachIcons.Trophy else EnglishCoachIcons.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyQuizView(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(EnglishCoachSpacing.screenHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(EnglishCoachColors.Purple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = EnglishCoachIcons.Quiz,
                contentDescription = null,
                tint = EnglishCoachColors.Purple,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(EnglishCoachSpacing.lg))
        Text(
            text = "今日已無待測驗題目",
            style = EnglishCoachTypography.screenTitle,
            color = EnglishCoachColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(EnglishCoachSpacing.sm))
        Text(
            text = "請先進行今日單字學習，或前往智慧複習中心挑戰！",
            style = EnglishCoachTypography.body,
            color = EnglishCoachColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxl))
        Button(
            onClick = onBack,
            shape = EnglishCoachShapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = EnglishCoachColors.Purple.copy(alpha = 0.1f),
                contentColor = EnglishCoachColors.Purple
            )
        ) {
            Text(
                text = "返回首頁",
                style = EnglishCoachTypography.button
            )
        }
    }
}
