package com.andy.englishcoach.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.data.model.Word
import com.andy.englishcoach.data.model.quiz.QuizQuestion
import com.andy.englishcoach.data.model.review.ReviewWordItem
import com.andy.englishcoach.ui.learning.WordCardView
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachGradients
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

/**
 * Smart Review Center screen with review list, detail bottom sheet, review quiz session, and results.
 * Polished with EnglishCoach Design System tokens, vector icons, and gradient CTAs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCenterScreen(
    viewModel: ReviewViewModel,
    onNavigateBack: () -> Unit,
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.isQuizActive) "複習測驗" else "智慧複習中心",
                        style = EnglishCoachTypography.screenTitle.copy(fontSize = 18.sp),
                        color = EnglishCoachColors.TextPrimary
                    )
                },
                navigationIcon = {
                    if (uiState.isQuizActive && !uiState.isQuizCompleted) {
                        TextButton(onClick = { viewModel.exitQuiz() }) {
                            Text(
                                text = "離開測驗",
                                style = EnglishCoachTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = EnglishCoachColors.Purple
                            )
                        }
                    } else {
                        TextButton(onClick = onNavigateBack) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                            ) {
                                Icon(
                                    imageVector = EnglishCoachIcons.ChevronLeft,
                                    contentDescription = "返回",
                                    tint = EnglishCoachColors.Purple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "返回",
                                    style = EnglishCoachTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EnglishCoachColors.Purple
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = EnglishCoachColors.Background
                )
            )
        },
        containerColor = EnglishCoachColors.Background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = EnglishCoachColors.Purple
                    )
                }
                uiState.isQuizActive -> {
                    if (uiState.isQuizCompleted) {
                        ReviewQuizResultContent(
                            uiState = uiState,
                            onDismiss = { viewModel.dismissResults() },
                            onSpeakSentence = { viewModel.speakSentence(it) }
                        )
                    } else if (uiState.quizQuestions.isNotEmpty()) {
                        ReviewQuizQuestionContent(
                            uiState = uiState,
                            onSelectOption = { viewModel.selectOption(it) },
                            onNextQuestion = { viewModel.nextQuestion() },
                            onSpeakWord = { viewModel.speakWord(it) }
                        )
                    } else {
                        ReviewEmptyState(onNavigateBack = onNavigateBack)
                    }
                }
                uiState.reviewWords.isEmpty() -> {
                    ReviewEmptyState(onNavigateBack = onNavigateBack)
                }
                else -> {
                    ReviewListView(
                        reviewWords = uiState.reviewWords,
                        isDailyLimitReached = uiState.isDailyLimitReached,
                        onWordClick = { viewModel.selectWordForDetail(it) },
                        onSpeakWord = { viewModel.speakWord(it) },
                        onStartReviewQuiz = { viewModel.startReviewQuiz() },
                        onOpenPaywall = onOpenPaywall
                    )
                }
            }

            // Word detail modal bottom sheet
            uiState.selectedWordForDetail?.let { word ->
                WordDetailBottomSheet(
                    word = word,
                    onDismiss = { viewModel.selectWordForDetail(null) },
                    onSpeakWord = { viewModel.speakWord(it) },
                    onSpeakSentence = { viewModel.speakSentence(it) }
                )
            }
        }
    }
}

@Composable
private fun ReviewListView(
    reviewWords: List<ReviewWordItem>,
    isDailyLimitReached: Boolean,
    onWordClick: (Word) -> Unit,
    onSpeakWord: (String) -> Unit,
    onStartReviewQuiz: () -> Unit,
    onOpenPaywall: () -> Unit = {}
) {
    val questionCount = minOf(reviewWords.size, 10)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal)
    ) {
        // Banner card with CTA button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = EnglishCoachSpacing.md),
            shape = EnglishCoachShapes.secondaryCard,
            colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(EnglishCoachSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)) {
                        Text(
                            text = "待複習單字",
                            style = EnglishCoachTypography.caption.copy(fontWeight = FontWeight.Bold),
                            color = EnglishCoachColors.TextSecondary
                        )
                        Text(
                            text = "${reviewWords.size} 個單字",
                            style = EnglishCoachTypography.screenTitle.copy(fontSize = 24.sp),
                            color = EnglishCoachColors.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(EnglishCoachShapes.pill)
                            .background(EnglishCoachColors.Orange.copy(alpha = 0.12f))
                            .padding(horizontal = EnglishCoachSpacing.md, vertical = EnglishCoachSpacing.xxs)
                    ) {
                        Text(
                            text = "SM-2 智慧排程",
                            style = EnglishCoachTypography.badge,
                            color = EnglishCoachColors.Orange
                        )
                    }
                }

                // CTA Button
                if (isDailyLimitReached) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(EnglishCoachShapes.button)
                            .background(EnglishCoachColors.Orange.copy(alpha = 0.12f))
                            .clickable(
                                role = Role.Button,
                                onClick = onOpenPaywall
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "今日免費額度已滿（升級 Premium 無限制複習 ➜）",
                            style = EnglishCoachTypography.button,
                            color = EnglishCoachColors.Orange
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(EnglishCoachShapes.button)
                            .background(EnglishCoachGradients.purpleBlue)
                            .clickable(
                                role = Role.Button,
                                onClick = onStartReviewQuiz
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "開始複習測驗（共 ${questionCount} 題）➜",
                            style = EnglishCoachTypography.button,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Section header
        Text(
            text = "單字列表（點擊查看卡片）",
            style = EnglishCoachTypography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = EnglishCoachColors.TextSecondary,
            modifier = Modifier.padding(vertical = EnglishCoachSpacing.xs)
        )

        // Word Items List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm),
            contentPadding = PaddingValues(bottom = EnglishCoachSpacing.xxxl)
        ) {
            items(reviewWords) { item ->
                ReviewWordRow(
                    item = item,
                    onClick = { onWordClick(item.word) },
                    onSpeak = { onSpeakWord(item.word.word) }
                )
            }
        }
    }
}

@Composable
private fun ReviewWordRow(
    item: ReviewWordItem,
    onClick: () -> Unit,
    onSpeak: () -> Unit
) {
    val word = item.word

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        shape = EnglishCoachShapes.secondaryButton,
        colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EnglishCoachSpacing.lg, vertical = EnglishCoachSpacing.md),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
                ) {
                    Text(
                        text = word.word,
                        style = EnglishCoachTypography.bodyMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = EnglishCoachColors.TextPrimary
                    )

                    // Target level chip (550+ 基礎, 750+ 進階, 860+ 金證 - NEVER Lv.1..Lv.5)
                    Box(
                        modifier = Modifier
                            .clip(EnglishCoachShapes.chip)
                            .background(EnglishCoachColors.Blue.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = word.target.displayName,
                            style = EnglishCoachTypography.badge,
                            color = EnglishCoachColors.Blue
                        )
                    }

                    // Part of speech chip
                    if (word.partOfSpeech.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(EnglishCoachShapes.chip)
                                .background(EnglishCoachColors.Border.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = word.partOfSpeech,
                                style = EnglishCoachTypography.badge.copy(fontWeight = FontWeight.SemiBold),
                                color = EnglishCoachColors.TextSecondary
                            )
                        }
                    }
                }

                // Status Badge: wrongCount or scheduled review
                if (item.wrongCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(EnglishCoachShapes.badge)
                            .background(EnglishCoachColors.Orange.copy(alpha = 0.1f))
                            .padding(horizontal = EnglishCoachSpacing.sm, vertical = EnglishCoachSpacing.xxs)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                        ) {
                            Icon(
                                imageVector = EnglishCoachIcons.Warning,
                                contentDescription = null,
                                tint = EnglishCoachColors.Orange,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${item.wrongCount} 次錯誤",
                                style = EnglishCoachTypography.badge,
                                color = EnglishCoachColors.Orange
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(EnglishCoachShapes.badge)
                            .background(EnglishCoachColors.Blue.copy(alpha = 0.1f))
                            .padding(horizontal = EnglishCoachSpacing.sm, vertical = EnglishCoachSpacing.xxs)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                        ) {
                            Icon(
                                imageVector = EnglishCoachIcons.CalendarClock,
                                contentDescription = null,
                                tint = EnglishCoachColors.Blue,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "排程複習",
                                style = EnglishCoachTypography.badge,
                                color = EnglishCoachColors.Blue
                            )
                        }
                    }
                }
            }

            // Phonetic & Translation Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
            ) {
                if (word.phonetic.isNotBlank()) {
                    Text(
                        text = word.phonetic,
                        style = EnglishCoachTypography.ipa.copy(fontSize = 13.sp),
                        color = EnglishCoachColors.Purple
                    )
                }

                Text(
                    text = word.translation,
                    style = EnglishCoachTypography.caption,
                    color = EnglishCoachColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ReviewEmptyState(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(EnglishCoachSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(EnglishCoachColors.Green.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = EnglishCoachIcons.CheckCircle,
                contentDescription = null,
                tint = EnglishCoachColors.Green,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.xl))

        Text(
            text = "太棒了！",
            style = EnglishCoachTypography.screenTitle,
            color = EnglishCoachColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.sm))

        Text(
            text = "目前沒有待複習的單字\n所有學習中的單字都處於良好的記憶排程中！",
            style = EnglishCoachTypography.body,
            color = EnglishCoachColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxxl))

        Button(
            onClick = onNavigateBack,
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

@Composable
private fun ReviewQuizQuestionContent(
    uiState: ReviewUiState,
    onSelectOption: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onSpeakWord: (String) -> Unit
) {
    val currentQuestion = uiState.quizQuestions.getOrNull(uiState.currentQuestionIndex) ?: return

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
                text = "複習題目 ${uiState.currentQuestionIndex + 1} / ${uiState.quizQuestions.size}",
                style = EnglishCoachTypography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = EnglishCoachColors.Purple
            )
            Text(
                text = "已答對：${uiState.answers.count { it.isCorrect }}",
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
                .height(190.dp),
            shape = EnglishCoachShapes.secondaryCard,
            colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(EnglishCoachSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentQuestion.word.word,
                    style = EnglishCoachTypography.cardWord.copy(fontSize = 32.sp),
                    color = EnglishCoachColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                if (currentQuestion.word.phonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(EnglishCoachSpacing.xs))
                    Text(
                        text = currentQuestion.word.phonetic,
                        style = EnglishCoachTypography.ipa.copy(fontSize = 16.sp),
                        color = EnglishCoachColors.Purple
                    )
                }

                Spacer(modifier = Modifier.height(EnglishCoachSpacing.md))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EnglishCoachGradients.purpleBlue)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "朗讀單字"
                        ) { onSpeakWord(currentQuestion.word.word) },
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

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.md))

        // 4 Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            currentQuestion.options.forEach { option ->
                val isCorrectChoice = option == currentQuestion.correctOption
                val isSelectedChoice = option == uiState.selectedOption

                val backgroundColor: Color = when {
                    uiState.isAnswered && isCorrectChoice -> EnglishCoachColors.Green.copy(alpha = 0.12f)
                    uiState.isAnswered && isSelectedChoice -> EnglishCoachColors.Red.copy(alpha = 0.12f)
                    else -> EnglishCoachColors.Surface
                }

                val borderColor: Color = when {
                    uiState.isAnswered && isCorrectChoice -> EnglishCoachColors.Green
                    uiState.isAnswered && isSelectedChoice -> EnglishCoachColors.Red
                    else -> EnglishCoachColors.Border.copy(alpha = 0.6f)
                }

                val textColor: Color = when {
                    uiState.isAnswered && isCorrectChoice -> EnglishCoachColors.Green
                    uiState.isAnswered && isSelectedChoice -> EnglishCoachColors.Red
                    uiState.isAnswered -> EnglishCoachColors.TextSecondary.copy(alpha = 0.5f)
                    else -> EnglishCoachColors.TextPrimary
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(EnglishCoachShapes.option)
                        .border(
                            width = if (uiState.isAnswered && (isCorrectChoice || isSelectedChoice)) 2.dp else 1.dp,
                            color = borderColor,
                            shape = EnglishCoachShapes.option
                        )
                        .clickable(
                            role = Role.RadioButton,
                            enabled = !uiState.isAnswered,
                            onClick = { onSelectOption(option) }
                        ),
                    shape = EnglishCoachShapes.option,
                    colors = CardDefaults.cardColors(containerColor = backgroundColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EnglishCoachSpacing.xl, vertical = EnglishCoachSpacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = EnglishCoachTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textColor
                        )

                        if (uiState.isAnswered) {
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

        Spacer(modifier = Modifier.weight(1f))

        // Next / Complete Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = EnglishCoachSpacing.xxl)
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isAnswered) {
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
                    Text(
                        text = if (uiState.currentQuestionIndex == uiState.quizQuestions.size - 1) "完成複習" else "下一個單字",
                        style = EnglishCoachTypography.button,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewQuizResultContent(
    uiState: ReviewUiState,
    onDismiss: () -> Unit,
    onSpeakSentence: (String) -> Unit
) {
    val correctCount = uiState.answers.count { it.isCorrect }
    val totalCount = uiState.answers.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(EnglishCoachSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = EnglishCoachSpacing.md, bottom = EnglishCoachSpacing.lg)
        ) {
            Text(
                text = "複習完成",
                style = EnglishCoachTypography.screenTitle.copy(fontSize = 24.sp),
                color = EnglishCoachColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(EnglishCoachSpacing.sm))

            Text(
                text = "已解決 $correctCount / $totalCount",
                style = EnglishCoachTypography.hero.copy(fontSize = 36.sp),
                color = if (correctCount == totalCount) EnglishCoachColors.Green else EnglishCoachColors.Purple
            )

            Spacer(modifier = Modifier.height(EnglishCoachSpacing.xs))

            Text(
                text = "答對的單字已更新複習排程與紀錄。",
                style = EnglishCoachTypography.secondary,
                color = EnglishCoachColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Answered Words List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
        ) {
            items(uiState.answers) { answer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = EnglishCoachShapes.secondaryButton,
                    colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(EnglishCoachSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
                    ) {
                        Icon(
                            imageVector = if (answer.isCorrect) EnglishCoachIcons.CheckCircle else EnglishCoachIcons.Refresh,
                            contentDescription = if (answer.isCorrect) "已掌握" else "待加強",
                            tint = if (answer.isCorrect) EnglishCoachColors.Green else EnglishCoachColors.Orange,
                            modifier = Modifier.size(22.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
                            ) {
                                Text(
                                    text = answer.word.word,
                                    style = EnglishCoachTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EnglishCoachColors.TextPrimary
                                )

                                if (answer.word.example.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                                            .clickable(
                                                role = Role.Button,
                                                onClickLabel = "朗讀例句"
                                            ) { onSpeakSentence(answer.word.example) }
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = EnglishCoachIcons.Volume,
                                            contentDescription = "朗讀例句",
                                            tint = EnglishCoachColors.Purple,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = answer.word.translation,
                                style = EnglishCoachTypography.caption,
                                color = EnglishCoachColors.TextSecondary
                            )
                        }

                        if (!answer.isCorrect) {
                            Text(
                                text = "需再複習",
                                style = EnglishCoachTypography.caption.copy(fontWeight = FontWeight.Bold),
                                color = EnglishCoachColors.Orange
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(EnglishCoachSpacing.md))

        // Return Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = EnglishCoachSpacing.xxl)
                .height(54.dp)
                .clip(EnglishCoachShapes.button)
                .background(EnglishCoachGradients.purpleBlue)
                .clickable(
                    role = Role.Button,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "返回智慧複習中心",
                style = EnglishCoachTypography.button,
                color = Color.White
            )
        }
    }
}

/**
 * Word detail modal bottom sheet containing the 3D flippable WordCardView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordDetailBottomSheet(
    word: Word,
    onDismiss: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeakSentence: (String) -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EnglishCoachColors.Background,
        shape = EnglishCoachShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EnglishCoachSpacing.screenHorizontal)
                .padding(bottom = EnglishCoachSpacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "單字詳情",
                    style = EnglishCoachTypography.screenTitle.copy(fontSize = 18.sp),
                    color = EnglishCoachColors.TextPrimary
                )
                TextButton(onClick = onDismiss) {
                    Icon(
                        imageVector = EnglishCoachIcons.Close,
                        contentDescription = "關閉",
                        tint = EnglishCoachColors.Purple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Flippable WordCardView
            WordCardView(
                word = word,
                isFlipped = isFlipped,
                onCardClick = { isFlipped = !isFlipped },
                onSpeakWord = { onSpeakWord(word.word) },
                onSpeakSentence = { onSpeakSentence(it) }
            )

            // Done Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(EnglishCoachShapes.button)
                    .background(EnglishCoachGradients.purpleBlue)
                    .clickable(
                        role = Role.Button,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "完成",
                    style = EnglishCoachTypography.button,
                    color = Color.White
                )
            }
        }
    }
}
