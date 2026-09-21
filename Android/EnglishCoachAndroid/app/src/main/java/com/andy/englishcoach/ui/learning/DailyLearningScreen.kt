package com.andy.englishcoach.ui.learning

import androidx.compose.foundation.background
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
import com.andy.englishcoach.data.model.DailyLearningUiState
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachGradients
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

/**
 * Daily Learning screen following the EnglishCoach iOS implementation.
 * Polished to match the EnglishCoach Design System.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLearningScreen(
    viewModel: DailyLearningViewModel,
    onComplete: () -> Unit = {},
    onStartQuiz: (() -> Unit)? = null,
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
                        text = "今日單字",
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
                actions = {
                    if (onNavigateToReview != null) {
                        TextButton(onClick = onNavigateToReview) {
                            Text(
                                text = "智慧複習",
                                style = EnglishCoachTypography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = EnglishCoachColors.Purple
                            )
                        }
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
                    LoadingView()
                }
                uiState.isTierCompleted || uiState.words.isEmpty() -> {
                    TierCompletedScreen(
                        target = uiState.target,
                        onBackToHome = onComplete
                    )
                }
                uiState.isCompleted -> {
                    CompletionScreen(
                        onStartQuiz = onStartQuiz,
                        onBackToHome = onComplete
                    )
                }
                else -> {
                    LearningContent(
                        uiState = uiState,
                        onCardClick = { viewModel.onCardClicked() },
                        onSpeakWord = { viewModel.speakCurrentWord() },
                        onSpeakSentence = { viewModel.speakSentence(it) },
                        onPrevious = { viewModel.onPreviousCard() },
                        onFlip = { viewModel.onFlipToMeaning() },
                        onNext = { viewModel.onNextCard() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LearningContent(
    uiState: DailyLearningUiState,
    onCardClick: () -> Unit,
    onSpeakWord: () -> Unit,
    onSpeakSentence: (String) -> Unit,
    onPrevious: () -> Unit,
    onFlip: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EnglishCoachSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
        ) {
            // Target Badge
            Box(
                modifier = Modifier
                    .clip(EnglishCoachShapes.pill)
                    .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                    .padding(horizontal = EnglishCoachSpacing.md, vertical = EnglishCoachSpacing.xxs)
            ) {
                Text(
                    text = uiState.target.displayName,
                    style = EnglishCoachTypography.caption.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = EnglishCoachColors.Purple
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "卡片 ${uiState.currentIndex + 1} / ${uiState.totalWords}",
                    style = EnglishCoachTypography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = EnglishCoachColors.Purple
                )
                Text(
                    text = "已完成 ${uiState.completedPercentage}%",
                    style = EnglishCoachTypography.caption.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = EnglishCoachColors.TextSecondary
                )
            }

            // Custom Gradient Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(EnglishCoachShapes.chip)
                    .background(EnglishCoachColors.Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(uiState.progressFraction)
                        .height(6.dp)
                        .clip(EnglishCoachShapes.chip)
                        .background(EnglishCoachGradients.purpleBlue)
                )
            }
        }

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.lg))

        // Word Card
        uiState.currentWord?.let { word ->
            WordCardView(
                word = word,
                isFlipped = uiState.isCardFlipped,
                onCardClick = onCardClick,
                onSpeakWord = onSpeakWord,
                onSpeakSentence = onSpeakSentence
            )
        }

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.lg))

        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = EnglishCoachSpacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
        ) {
            // Previous Button
            Button(
                onClick = onPrevious,
                enabled = !uiState.isFirstCard,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = EnglishCoachShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnglishCoachColors.Purple.copy(alpha = 0.1f),
                    contentColor = EnglishCoachColors.Purple,
                    disabledContainerColor = EnglishCoachColors.Border.copy(alpha = 0.4f),
                    disabledContentColor = EnglishCoachColors.TextSecondary.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                ) {
                    Icon(
                        imageVector = EnglishCoachIcons.ChevronLeft,
                        contentDescription = "上一個",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "上一個",
                        style = EnglishCoachTypography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Primary Action Button (Flip or Next)
            if (!uiState.isCardFlipped) {
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(54.dp)
                        .clip(EnglishCoachShapes.button)
                        .background(EnglishCoachGradients.purpleIndigo)
                        .clickable(
                            role = Role.Button,
                            onClick = onFlip
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "查看中文意思",
                        style = EnglishCoachTypography.button,
                        color = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(54.dp)
                        .clip(EnglishCoachShapes.button)
                        .background(EnglishCoachGradients.purpleBlue)
                        .clickable(
                            role = Role.Button,
                            onClick = onNext
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                    ) {
                        Text(
                            text = if (uiState.isLastCard) "完成" else "下一個",
                            style = EnglishCoachTypography.button,
                            color = Color.White
                        )
                        Icon(
                            imageVector = if (uiState.isLastCard) EnglishCoachIcons.Check else EnglishCoachIcons.ArrowForward,
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
private fun LoadingView() {
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
            text = "載入今日單字中...",
            style = EnglishCoachTypography.body,
            color = EnglishCoachColors.TextSecondary
        )
    }
}

@Composable
private fun CompletionScreen(
    onStartQuiz: (() -> Unit)?,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(EnglishCoachSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxxl))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(EnglishCoachColors.Purple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = EnglishCoachIcons.Crown,
                    contentDescription = "完成學習",
                    tint = EnglishCoachColors.Orange,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text(
                text = "太棒了！",
                style = EnglishCoachTypography.hero,
                color = EnglishCoachColors.TextPrimary
            )

            Text(
                text = "你已完成今日單字學習。立即進行單字測驗以正式驗收成果！",
                style = EnglishCoachTypography.body,
                color = EnglishCoachColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = EnglishCoachSpacing.screenHorizontal)
            )
        }

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxxl))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(EnglishCoachShapes.button)
                    .background(EnglishCoachGradients.purpleBlue)
                    .clickable(
                        role = Role.Button,
                        onClick = { onStartQuiz?.invoke() ?: onBackToHome() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "開始今日測驗 ➜",
                    style = EnglishCoachTypography.button,
                    color = Color.White
                )
            }

            TextButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "返回首頁",
                    style = EnglishCoachTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = EnglishCoachColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TierCompletedScreen(
    target: ToeicTarget,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(EnglishCoachSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxxl))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(EnglishCoachColors.Orange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = EnglishCoachIcons.Trophy,
                    contentDescription = "目標完成",
                    tint = EnglishCoachColors.Orange,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text(
                text = "本目標單字已全部學完！",
                style = EnglishCoachTypography.screenTitle,
                color = EnglishCoachColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "恭喜您！${target.displayName} 的單字已全部學習完畢。\n建議前往「智慧複習中心」鞏固記憶，或切換至其他目標等級繼續挑戰！",
                style = EnglishCoachTypography.body,
                color = EnglishCoachColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = EnglishCoachSpacing.screenHorizontal)
            )
        }

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxxl))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(EnglishCoachShapes.button)
                .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                .clickable(
                    role = Role.Button,
                    onClick = onBackToHome
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "返回首頁",
                style = EnglishCoachTypography.button,
                color = EnglishCoachColors.Purple
            )
        }
    }
}
