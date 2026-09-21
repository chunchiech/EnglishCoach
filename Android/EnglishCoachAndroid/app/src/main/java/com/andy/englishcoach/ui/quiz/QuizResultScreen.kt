package com.andy.englishcoach.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.data.model.quiz.QuizAnswer
import com.andy.englishcoach.data.model.quiz.QuizResult
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachGradients
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

/**
 * Screen displaying the scorecard and detailed question breakdown of a completed quiz session.
 * Polished with EnglishCoach Design System tokens and vector icons.
 */
@Composable
fun QuizResultScreen(
    result: QuizResult,
    onSpeakSentence: (String) -> Unit,
    onBackToHome: () -> Unit,
    onNavigateToReview: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EnglishCoachSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
            ) {
                Text(
                    text = "測驗成果驗收",
                    style = EnglishCoachTypography.screenTitle,
                    color = EnglishCoachColors.TextPrimary
                )
                Text(
                    text = if (result.wrongCount == 0) "全部掌握，太厲害了！" else "今日完成了一次精準自我檢測",
                    style = EnglishCoachTypography.secondary,
                    color = EnglishCoachColors.TextSecondary
                )
            }

            // 4-Stat Metric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
            ) {
                MetricBox(
                    title = "完成題數",
                    value = "${result.totalQuestions} 題",
                    icon = EnglishCoachIcons.Check,
                    color = EnglishCoachColors.Blue,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "答對題數",
                    value = "${result.correctCount} 題",
                    icon = EnglishCoachIcons.ThumbUp,
                    color = EnglishCoachColors.Green,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
            ) {
                MetricBox(
                    title = "測驗正確率",
                    value = "${result.scorePercentage}%",
                    icon = EnglishCoachIcons.Percent,
                    color = EnglishCoachColors.Purple,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "待加強單字",
                    value = "${result.wrongCount} 個",
                    icon = EnglishCoachIcons.Warning,
                    color = if (result.wrongCount > 0) EnglishCoachColors.Orange else EnglishCoachColors.Green,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Detailed question review list
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = EnglishCoachSpacing.md)
        ) {
            Text(
                text = "詳細檢討",
                style = EnglishCoachTypography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = EnglishCoachColors.TextSecondary,
                modifier = Modifier.padding(bottom = EnglishCoachSpacing.sm)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
            ) {
                items(result.answers) { answer ->
                    ReviewAnswerItem(
                        answer = answer,
                        onSpeakSentence = onSpeakSentence
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = EnglishCoachSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            if (result.wrongCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(EnglishCoachShapes.button)
                        .background(EnglishCoachGradients.purpleBlue)
                        .clickable(
                            role = Role.Button,
                            onClick = { onNavigateToReview?.invoke() ?: onBackToHome() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "前往智慧複習中心 ➜",
                        style = EnglishCoachTypography.button,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "返回首頁",
                        style = EnglishCoachTypography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = EnglishCoachColors.TextSecondary
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(EnglishCoachShapes.button)
                        .background(EnglishCoachGradients.greenBlue)
                        .clickable(
                            role = Role.Button,
                            onClick = onBackToHome
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "太棒了，完成今日學習 ➜",
                        style = EnglishCoachTypography.button,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = EnglishCoachShapes.option,
        colors = CardDefaults.cardColors(
            containerColor = EnglishCoachColors.Surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EnglishCoachSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    style = EnglishCoachTypography.bodyMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = EnglishCoachColors.TextPrimary
                )
                Text(
                    text = title,
                    style = EnglishCoachTypography.caption.copy(
                        fontSize = 11.sp
                    ),
                    color = EnglishCoachColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ReviewAnswerItem(
    answer: QuizAnswer,
    onSpeakSentence: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = EnglishCoachShapes.secondaryButton,
        colors = CardDefaults.cardColors(
            containerColor = EnglishCoachColors.Surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EnglishCoachSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            Icon(
                imageVector = if (answer.isCorrect) EnglishCoachIcons.CheckCircle else EnglishCoachIcons.CloseCircle,
                contentDescription = if (answer.isCorrect) "正確" else "錯誤",
                tint = if (answer.isCorrect) EnglishCoachColors.Green else EnglishCoachColors.Red,
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
                        style = EnglishCoachTypography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
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
                    style = EnglishCoachTypography.secondary.copy(
                        fontSize = 13.sp
                    ),
                    color = EnglishCoachColors.TextSecondary
                )

                if (answer.word.example.isNotBlank()) {
                    Text(
                        text = answer.word.example,
                        style = EnglishCoachTypography.caption,
                        color = EnglishCoachColors.Purple.copy(alpha = 0.85f),
                        maxLines = 2
                    )
                }
            }

            if (!answer.isCorrect) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                ) {
                    Text(
                        text = "選擇答案：",
                        style = EnglishCoachTypography.caption.copy(
                            fontSize = 10.sp
                        ),
                        color = EnglishCoachColors.TextSecondary
                    )
                    Text(
                        text = answer.chosenOption,
                        style = EnglishCoachTypography.caption.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = EnglishCoachColors.Red
                    )
                }
            }
        }
    }
}
