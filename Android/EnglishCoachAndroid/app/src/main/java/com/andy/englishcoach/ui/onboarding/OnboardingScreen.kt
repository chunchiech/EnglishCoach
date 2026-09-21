package com.andy.englishcoach.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.notification.DailyReminderScheduler
import com.andy.englishcoach.onboarding.model.LearningScenario
import com.andy.englishcoach.onboarding.model.PlacementQuestion
import com.andy.englishcoach.onboarding.model.ReminderTimeOption
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher for Android 13+ POST_NOTIFICATIONS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.enableReminder(
                onScheduleAlarm = { h, m ->
                    DailyReminderScheduler.scheduleDailyReminder(context, h, m)
                },
                onProceed = {}
            )
        } else {
            // Permission denied: do not block onboarding, continue smoothly
            viewModel.skipReminder(
                onCancelAlarm = {
                    DailyReminderScheduler.cancelDailyReminder(context)
                },
                onProceed = {}
            )
        }
    }

    // Handle back button smoothly across onboarding steps
    BackHandler {
        val handled = viewModel.handleBackPress()
        if (!handled) {
            // At root step: let activity handle back
            (context as? androidx.activity.ComponentActivity)?.finish()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EnglishCoachColors.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Step Indicator for major stages
            OnboardingStepIndicator(currentStep = state.currentStep)

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingStepAnimation",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    OnboardingStep.REMINDER -> {
                        ReminderStepScreen(
                            state = state,
                            onSelectOption = { viewModel.selectReminderOption(it) },
                            onEnable = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.enableReminder(
                                        onScheduleAlarm = { h, m ->
                                            DailyReminderScheduler.scheduleDailyReminder(context, h, m)
                                        },
                                        onProceed = {}
                                    )
                                }
                            },
                            onSkip = {
                                viewModel.skipReminder(
                                    onCancelAlarm = {
                                        DailyReminderScheduler.cancelDailyReminder(context)
                                    },
                                    onProceed = {}
                                )
                            }
                        )
                    }
                    OnboardingStep.ASSESSMENT_INTRO -> {
                        AssessmentIntroScreen(
                            onStart = { viewModel.startAssessment() },
                            onSkip = { viewModel.skipAssessmentToDefaultTarget() }
                        )
                    }
                    OnboardingStep.ASSESSMENT_QUIZ -> {
                        AssessmentQuizScreen(
                            state = state,
                            onSelectOption = { viewModel.answerAssessmentQuestion(it) },
                            onSpeakWord = { viewModel.speakWord(it) }
                        )
                    }
                    OnboardingStep.ASSESSMENT_RESULT -> {
                        AssessmentResultScreen(
                            state = state,
                            onProceed = { viewModel.proceedFromAssessmentResult() }
                        )
                    }
                    OnboardingStep.TARGET_SELECTION -> {
                        TargetSelectionScreen(
                            state = state,
                            onSelectTarget = { viewModel.selectTarget(it) },
                            onProceed = { viewModel.proceedToLearningContext() },
                            onBack = { viewModel.handleBackPress() }
                        )
                    }
                    OnboardingStep.LEARNING_CONTEXT -> {
                        LearningContextScreen(
                            state = state,
                            onToggle = { viewModel.toggleScenario(it) },
                            onMoveUp = { viewModel.moveScenarioUp(it) },
                            onMoveDown = { viewModel.moveScenarioDown(it) },
                            onComplete = { viewModel.completeOnboarding(onFinish = onComplete) },
                            onBack = { viewModel.handleBackPress() }
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Step Indicator
@Composable
private fun OnboardingStepIndicator(currentStep: OnboardingStep) {
    val activeIndex = when (currentStep) {
        OnboardingStep.REMINDER -> 0
        OnboardingStep.ASSESSMENT_INTRO,
        OnboardingStep.ASSESSMENT_QUIZ,
        OnboardingStep.ASSESSMENT_RESULT -> 1
        OnboardingStep.TARGET_SELECTION -> 2
        OnboardingStep.LEARNING_CONTEXT -> 3
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0..3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (i <= activeIndex) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder
                    )
            )
        }
    }
}

// MARK: - Step 1: Reminder Screen
@Composable
private fun ReminderStepScreen(
    state: OnboardingUiState,
    onSelectOption: (String) -> Unit,
    onEnable: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Badge Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(EnglishCoachColors.Purple.copy(alpha = 0.15f), EnglishCoachColors.Blue.copy(alpha = 0.15f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = EnglishCoachColors.Purple,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "設定每日學習提醒",
            style = EnglishCoachTypography.hero,
            color = EnglishCoachColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "每天提醒你回來學英文",
            style = EnglishCoachTypography.sectionHeader,
            color = EnglishCoachColors.Purple,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "選一個適合你的時間，讓 EnglishCoach 每天提醒你背單字。",
            style = EnglishCoachTypography.secondary,
            color = EnglishCoachColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4 Preset Time Options
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReminderTimeOption.PRESET_OPTIONS.forEach { option ->
                val isSelected = state.selectedReminderOptionId == option.id
                Card(
                    shape = EnglishCoachShapes.card,
                    colors = CardDefaults.cardColors(
                        containerColor = EnglishCoachColors.Surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(EnglishCoachShapes.card)
                        .clickable { onSelectOption(option.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.displayTime,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.TextPrimary
                            )
                            Text(
                                text = option.periodTag,
                                fontSize = 13.sp,
                                color = EnglishCoachColors.TextSecondary
                            )
                        }

                        // Radio circle indicator
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(EnglishCoachColors.Purple)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // CTA buttons
        Button(
            onClick = onEnable,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(EnglishCoachColors.Purple, EnglishCoachColors.Blue)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = "開啟每日提醒",
                style = EnglishCoachTypography.button,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "先不用",
                style = EnglishCoachTypography.secondary,
                color = EnglishCoachColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// MARK: - Step 2A: Assessment Intro Screen
@Composable
private fun AssessmentIntroScreen(
    onStart: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = EnglishCoachColors.Purple,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "完成 20 題快速程度評估",
            style = EnglishCoachTypography.hero,
            color = EnglishCoachColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "只要約 2 分鐘，透過 20 題標準商務單字測驗，系統將自動分析您目前的詞彙實力，並為您推薦最適起點。",
            style = EnglishCoachTypography.body,
            color = EnglishCoachColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Info Cards
        Card(
            shape = EnglishCoachShapes.card,
            colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EnglishCoachColors.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "測驗包含 550+ 基礎、750+ 進階、860+ 金證三階題型",
                        style = EnglishCoachTypography.secondary,
                        color = EnglishCoachColors.TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EnglishCoachColors.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "測驗結束後立即提供難度起點建議",
                        style = EnglishCoachTypography.secondary,
                        color = EnglishCoachColors.TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStart,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(EnglishCoachColors.Purple, EnglishCoachColors.Blue)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = "開始 20 題程度評估 ➜",
                style = EnglishCoachTypography.button,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "先使用預設建議直接開始",
                style = EnglishCoachTypography.secondary,
                color = EnglishCoachColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// MARK: - Step 2B: Assessment Quiz Screen
@Composable
private fun AssessmentQuizScreen(
    state: OnboardingUiState,
    onSelectOption: (String) -> Unit,
    onSpeakWord: (String) -> Unit
) {
    val currentQ = state.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "第 ${state.currentQuestionIndex + 1} / ${state.questions.size} 題",
                style = EnglishCoachTypography.badge,
                color = EnglishCoachColors.Purple
            )
            Text(
                text = "完成度 ${state.progressPercent}%",
                style = EnglishCoachTypography.caption,
                color = EnglishCoachColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { state.progressPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = EnglishCoachColors.Purple,
            trackColor = EnglishCoachColors.CardBorder
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Word Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentQ.word,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = EnglishCoachColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentQ.phonetic,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = EnglishCoachColors.Purple
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                IconButton(
                    onClick = { onSpeakWord(currentQ.word) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = EnglishCoachIcons.Volume,
                        contentDescription = "發音",
                        tint = EnglishCoachColors.Purple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4 Multiple Choice Options
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            currentQ.options.forEach { option ->
                val isSelected = state.selectedOption == option
                val isAnswered = state.selectedOption != null
                val isCorrect = option == currentQ.correctAnswer

                val backgroundColor = when {
                    !isAnswered -> EnglishCoachColors.Surface
                    isSelected && isCorrect -> EnglishCoachColors.Green.copy(alpha = 0.12f)
                    isSelected && !isCorrect -> EnglishCoachColors.Red.copy(alpha = 0.12f)
                    isAnswered && isCorrect -> EnglishCoachColors.Green.copy(alpha = 0.12f)
                    else -> EnglishCoachColors.Surface
                }

                val borderColor = when {
                    !isAnswered -> EnglishCoachColors.CardBorder
                    isSelected && isCorrect -> EnglishCoachColors.Green
                    isSelected && !isCorrect -> EnglishCoachColors.Red
                    isAnswered && isCorrect -> EnglishCoachColors.Green
                    else -> EnglishCoachColors.CardBorder
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !isAnswered) { onSelectOption(option) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            style = EnglishCoachTypography.body,
                            color = when {
                                !isAnswered -> EnglishCoachColors.TextPrimary
                                isCorrect -> EnglishCoachColors.Green
                                isSelected -> EnglishCoachColors.Red
                                else -> EnglishCoachColors.TextSecondary
                            },
                            fontWeight = if (isSelected || (isAnswered && isCorrect)) FontWeight.Bold else FontWeight.Medium
                        )

                        if (isAnswered) {
                            if (isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "正確",
                                    tint = EnglishCoachColors.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Close,
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
    }
}

// MARK: - Step 2C: Assessment Result Screen
@Composable
private fun AssessmentResultScreen(
    state: OnboardingUiState,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = EnglishCoachColors.Purple,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "程度評估已完成！",
            style = EnglishCoachTypography.hero,
            color = EnglishCoachColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "已為您量身分析詞彙程度與目標進度",
            style = EnglishCoachTypography.secondary,
            color = EnglishCoachColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Score & Recommended Path Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "評估測驗成績",
                            style = EnglishCoachTypography.caption,
                            color = EnglishCoachColors.TextSecondary
                        )
                        Text(
                            text = "${state.correctCount} / ${state.questions.size} 題",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnglishCoachColors.Purple
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "建議目標路徑",
                            style = EnglishCoachTypography.caption,
                            color = EnglishCoachColors.TextSecondary
                        )
                        Text(
                            text = state.recommendedTarget.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnglishCoachColors.Blue
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(EnglishCoachColors.CardBorder)
                )

                Text(
                    text = "🎯 系統已根據您的答題表現，推薦最合適的起步難度。在下一步您可以自由確認或調整目標。",
                    style = EnglishCoachTypography.secondary,
                    color = EnglishCoachColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onProceed,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(EnglishCoachColors.Purple, EnglishCoachColors.Blue)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = "下一步：確認學習目標 ➜",
                style = EnglishCoachTypography.button,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// MARK: - Step 3: Target Selection Screen
@Composable
private fun TargetSelectionScreen(
    state: OnboardingUiState,
    onSelectTarget: (ToeicTarget) -> Unit,
    onProceed: () -> Unit,
    onBack: () -> Unit
) {
    val goals = listOf(
        Triple(
            ToeicTarget.BASIC,
            "🎯 550+ 基礎",
            "打底必備 · 掌握日常商務基礎單字與基本溝通"
        ),
        Triple(
            ToeicTarget.ADVANCED,
            "🎯 750+ 進階",
            "商務實用 · 跨國職場溝通、會議談判與商務書信"
        ),
        Triple(
            ToeicTarget.GOLD,
            "🏆 860+ 金證",
            "頂尖金色證書 · 外商高管必備，精通全方位商業策略與專業術語"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "設定您的 TOEIC 目標",
            style = EnglishCoachTypography.hero,
            color = EnglishCoachColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "為您客製化單字難度分配與每日練習規劃",
            style = EnglishCoachTypography.secondary,
            color = EnglishCoachColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Disclaimer Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = EnglishCoachColors.Blue.copy(alpha = 0.08f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = EnglishCoachColors.Blue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "此目標僅作為個人化學習進度規劃與題庫配置之參考，不宣稱亦不代表預測正式 TOEIC 測驗成績。",
                    style = EnglishCoachTypography.caption,
                    color = EnglishCoachColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3 Goals
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            goals.forEach { (target, title, subtitle) ->
                val isSelected = state.selectedTarget == target
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectTarget(target) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subtitle,
                                style = EnglishCoachTypography.secondary,
                                color = EnglishCoachColors.TextSecondary
                            )
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已選取",
                                tint = EnglishCoachColors.Purple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(
                    text = "上一步",
                    style = EnglishCoachTypography.button,
                    color = EnglishCoachColors.TextSecondary
                )
            }

            Button(
                onClick = onProceed,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(EnglishCoachColors.Purple, EnglishCoachColors.Blue)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Text(
                    text = "下一步：學習情境 ➜",
                    style = EnglishCoachTypography.button,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// MARK: - Step 4: Learning Context Screen
@Composable
private fun LearningContextScreen(
    state: OnboardingUiState,
    onToggle: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "選擇您的學習情境",
            style = EnglishCoachTypography.hero,
            color = EnglishCoachColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "可複選，已按您的選擇順序排列優先級",
            style = EnglishCoachTypography.secondary,
            color = EnglishCoachColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // List of 6 Scenarios
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.availableScenarios.forEach { scenario ->
                val isSelected = state.selectedScenarioIds.contains(scenario.id)
                val priorityIndex = state.selectedScenarioIds.indexOf(scenario.id)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggle(scenario.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = scenario.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EnglishCoachColors.TextPrimary
                                )

                                if (priorityIndex >= 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EnglishCoachColors.Purple.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "優先 ${priorityIndex + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EnglishCoachColors.Purple
                                        )
                                    }

                                    // Up/Down reordering buttons if selected
                                    if (state.selectedScenarioIds.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (priorityIndex > 0) {
                                            IconButton(
                                                onClick = { onMoveUp(scenario.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = EnglishCoachIcons.ArrowUp,
                                                    contentDescription = "提升優先級",
                                                    tint = EnglishCoachColors.Purple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (priorityIndex < state.selectedScenarioIds.size - 1) {
                                            IconButton(
                                                onClick = { onMoveDown(scenario.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = EnglishCoachIcons.ArrowDown,
                                                    contentDescription = "降低優先級",
                                                    tint = EnglishCoachColors.Purple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = scenario.subtitle,
                                style = EnglishCoachTypography.caption,
                                color = EnglishCoachColors.TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Selection indicator
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(EnglishCoachColors.Purple)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(
                    text = "上一步",
                    style = EnglishCoachTypography.button,
                    color = EnglishCoachColors.TextSecondary
                )
            }

            Button(
                onClick = onComplete,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(EnglishCoachColors.Purple, EnglishCoachColors.Blue)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Text(
                    text = "完成並開始學習 ➜",
                    style = EnglishCoachTypography.button,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
