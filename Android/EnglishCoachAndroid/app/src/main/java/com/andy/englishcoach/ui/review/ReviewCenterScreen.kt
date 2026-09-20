package com.andy.englishcoach.ui.review

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.andy.englishcoach.data.model.Word
import com.andy.englishcoach.data.model.quiz.QuizQuestion
import com.andy.englishcoach.data.model.review.ReviewWordItem
import com.andy.englishcoach.ui.learning.WordCardView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCenterScreen(
    viewModel: ReviewViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val purpleColor = Color(0xFF8A2BE2)
    val blueColor = Color(0xFF1E90FF)
    val greenColor = Color(0xFF2E7D32)
    val orangeColor = Color(0xFFE65100)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.isQuizActive) "複習測驗" else "智慧複習中心",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    if (uiState.isQuizActive && !uiState.isQuizCompleted) {
                        TextButton(onClick = { viewModel.exitQuiz() }) {
                            Text("離開測驗", color = purpleColor, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = onNavigateBack) {
                            Text("‹ 返回", color = purpleColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = purpleColor
                    )
                }
                uiState.isQuizActive -> {
                    if (uiState.isQuizCompleted) {
                        ReviewQuizResultContent(
                            uiState = uiState,
                            onDismiss = { viewModel.dismissResults() },
                            onSpeakWord = { viewModel.speakWord(it) },
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
                        onStartReviewQuiz = { viewModel.startReviewQuiz() }
                    )
                }
            }

            // Word detail dialog / sheet
            uiState.selectedWordForDetail?.let { word ->
                WordDetailDialog(
                    word = word,
                    onDismiss = { viewModel.selectWordForDetail(null) },
                    onSpeakWord = { viewModel.speakWord(it) },
                    onSpeakSentence = { viewModel.speakSentence(it) }
                )
            }
        }
    }
}

/**
 * Review word list view with stats header, cards, and Start Quiz CTA button.
 */
@Composable
private fun ReviewListView(
    reviewWords: List<ReviewWordItem>,
    isDailyLimitReached: Boolean,
    onWordClick: (Word) -> Unit,
    onSpeakWord: (String) -> Unit,
    onStartReviewQuiz: () -> Unit
) {
    val purpleColor = Color(0xFF8A2BE2)
    val blueColor = Color(0xFF1E90FF)

    val questionCount = minOf(reviewWords.size, 10)
    val ctaTitle = if (isDailyLimitReached) {
        "今日目標已達成（共 10 題）"
    } else {
        "開始複習測驗（共 ${questionCount} 題）➜"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Stats Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "${reviewWords.size} 個單字",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "需要加強練習的單字",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // List of Review Words
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reviewWords, key = { it.word.word }) { item ->
                ReviewWordListItem(
                    item = item,
                    onClick = { onWordClick(item.word) },
                    onSpeak = { onSpeakWord(item.word.word) }
                )
            }
        }

        // Bottom CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = onStartReviewQuiz,
                enabled = !isDailyLimitReached && reviewWords.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = purpleColor
                )
            ) {
                Text(
                    text = ctaTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Single item card in Review Center word list.
 */
@Composable
private fun ReviewWordListItem(
    item: ReviewWordItem,
    onClick: () -> Unit,
    onSpeak: () -> Unit
) {
    val word = item.word
    val purpleColor = Color(0xFF8A2BE2)
    val blueColor = Color(0xFF1E90FF)
    val orangeColor = Color(0xFFE65100)

    val scenarioText = if (word.topic.isNotBlank() && word.subtopic.isNotBlank()) {
        "${word.topic} › ${word.subtopic}"
    } else if (word.topic.isNotBlank()) {
        word.topic
    } else {
        "商務英語"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Scenario Breadcrumb
            Text(
                text = scenarioText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = purpleColor
            )

            // Word Row with Chips & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = word.word,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Target level chip (550+ 基礎, 750+ 進階, 860+ 金證 - NEVER Lv.1..Lv.5)
                    Box(
                        modifier = Modifier
                            .background(
                                color = blueColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = word.target.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = blueColor
                        )
                    }

                    // Part of speech chip
                    if (word.partOfSpeech.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = word.partOfSpeech,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status Badge: wrongCount or scheduled review
                if (item.wrongCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = orangeColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "⚠️", fontSize = 10.sp)
                            Text(
                                text = "${item.wrongCount} 次錯誤",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = orangeColor
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                color = blueColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "📅", fontSize = 10.sp)
                            Text(
                                text = "排程複習",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = blueColor
                            )
                        }
                    }
                }
            }

            // Phonetic & Translation Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (word.phonetic.isNotBlank()) {
                    Text(
                        text = word.phonetic,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                        color = purpleColor
                    )
                }

                Text(
                    text = word.translation,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Empty state when no words are due for review.
 */
@Composable
private fun ReviewEmptyState(onNavigateBack: () -> Unit) {
    val purpleColor = Color(0xFF8A2BE2)
    val greenColor = Color(0xFF2E7D32)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(greenColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✔",
                fontSize = 44.sp,
                color = greenColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "太棒了！目前無待複習單字",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "測驗中答錯或需要強化的單字，會自動收錄在智慧複習中心，依照記憶曲線排程複習。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNavigateBack,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = purpleColor)
        ) {
            Text(
                text = "返回首頁",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Review Quiz Question Screen.
 */
@Composable
private fun ReviewQuizQuestionContent(
    uiState: ReviewUiState,
    onSelectOption: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onSpeakWord: (String) -> Unit
) {
    val currentQuestion = uiState.quizQuestions[uiState.currentQuestionIndex]
    val purpleColor = Color(0xFF8A2BE2)
    val greenColor = Color(0xFF2E7D32)
    val redColor = Color(0xFFC62828)

    val correctCount = uiState.answers.count { it.isCorrect }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "複習 ${uiState.currentQuestionIndex + 1} / ${uiState.quizQuestions.size}",
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

        Spacer(modifier = Modifier.height(16.dp))

        // Question Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentQuestion.word.word,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (currentQuestion.word.phonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentQuestion.word.phonetic,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                        color = purpleColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(purpleColor)
                        .clickable { onSpeakWord(currentQuestion.word.word) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔊", fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            currentQuestion.options.forEach { option ->
                val isCorrectChoice = option == currentQuestion.correctOption
                val isSelectedChoice = option == uiState.selectedOption

                val backgroundColor: Color = when {
                    uiState.isAnswered && isCorrectChoice -> greenColor.copy(alpha = 0.15f)
                    uiState.isAnswered && isSelectedChoice -> redColor.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderColor: Color = when {
                    uiState.isAnswered && isCorrectChoice -> greenColor
                    uiState.isAnswered && isSelectedChoice -> redColor
                    else -> Color.Transparent
                }

                val textColor: Color = when {
                    uiState.isAnswered && isCorrectChoice -> greenColor
                    uiState.isAnswered && isSelectedChoice -> redColor
                    uiState.isAnswered -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable(
                            enabled = !uiState.isAnswered,
                            onClick = { onSelectOption(option) }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )

                        if (uiState.isAnswered) {
                            if (isCorrectChoice) {
                                Text(text = "✅", fontSize = 16.sp)
                            } else if (isSelectedChoice) {
                                Text(text = "❌", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next / Complete Button
        if (uiState.isAnswered) {
            Button(
                onClick = onNextQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = purpleColor)
            ) {
                Text(
                    text = if (uiState.currentQuestionIndex == uiState.quizQuestions.size - 1) "完成複習" else "下一個單字",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            Spacer(modifier = Modifier.height(54.dp))
        }
    }
}

/**
 * Review Quiz Results View showing score and list of reviewed words.
 */
@Composable
private fun ReviewQuizResultContent(
    uiState: ReviewUiState,
    onDismiss: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeakSentence: (String) -> Unit
) {
    val purpleColor = Color(0xFF8A2BE2)
    val greenColor = Color(0xFF2E7D32)
    val orangeColor = Color(0xFFE65100)

    val correctCount = uiState.answers.count { it.isCorrect }
    val totalCount = uiState.answers.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp, bottom = 16.dp)
        ) {
            Text(
                text = "複習完成",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "已解決 $correctCount / $totalCount",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (correctCount == totalCount) greenColor else purpleColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "答對的單字已更新複習排程與紀錄。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Answered Words List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.answers) { answer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (answer.isCorrect) "✅" else "🔄",
                            fontSize = 20.sp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = answer.word.word,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "🔊",
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { onSpeakWord(answer.word.word) }
                                )
                            }
                            Text(
                                text = answer.word.translation,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Badge: 已解決 / 需練習
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (answer.isCorrect) greenColor.copy(alpha = 0.1f) else orangeColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (answer.isCorrect) "已解決" else "需練習",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (answer.isCorrect) greenColor else orangeColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Return button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = purpleColor)
        ) {
            Text(
                text = "返回智慧複習中心",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Word detail modal dialog containing the 3D flippable WordCardView.
 */
@Composable
private fun WordDetailDialog(
    word: Word,
    onDismiss: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeakSentence: (String) -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val purpleColor = Color(0xFF8A2BE2)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "單字詳情",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onDismiss) {
                        Text("✕", color = purpleColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Flippable WordCardView
                WordCardView(
                    word = word,
                    isFlipped = isFlipped,
                    onCardClick = { isFlipped = !isFlipped },
                    onSpeakWord = { onSpeakWord(word.word) },
                    onSpeakSentence = { onSpeakSentence(it) },
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = purpleColor)
                ) {
                    Text("完成", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
