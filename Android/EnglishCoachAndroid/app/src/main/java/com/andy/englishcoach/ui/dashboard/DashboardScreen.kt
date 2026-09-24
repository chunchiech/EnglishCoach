package com.andy.englishcoach.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.ui.components.DailyTargetBottomSheet
import com.andy.englishcoach.ui.components.ProgressRing
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachGradients
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography
import java.util.Locale

/**
 * Android EnglishCoach Home Hub (Dashboard).
 * Source of truth: iOS DashboardView.swift.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartLearning: () -> Unit,
    onStartQuiz: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EnglishCoachColors.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = EnglishCoachSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxl)
        ) {
            // 1. Header Section
            DashboardHeader(
                greeting = state.greeting,
                onNavigateToSettings = onNavigateToSettings
            )

            // 2. Target Level Selector (Segmented Control)
            TargetLevelSelector(
                selectedTarget = state.targetLevel,
                onTargetSelected = { viewModel.setTargetLevel(it) }
            )

            // 3. Daily Practice Hero Card
            DailyPracticeHeroCard(
                state = state,
                onCtaClicked = {
                    when (state.ctaAction) {
                        DashboardCtaAction.START_LEARNING -> onStartLearning()
                        DashboardCtaAction.START_QUIZ -> onStartQuiz()
                        DashboardCtaAction.NAVIGATE_REVIEW -> onNavigateToReview()
                        DashboardCtaAction.COMPLETED -> {
                            if (state.reviewCount > 0) {
                                onNavigateToReview()
                            }
                        }
                    }
                },
                onAdjustDailyTargetClicked = {
                    viewModel.setDailyTargetSheetVisible(true)
                }
            )

            // 4. Statistics Section (2-Column Grid)
            StatisticsSection(
                learnedWords = state.learnedWords,
                totalWords = state.totalWords,
                accuracy = state.accuracy
            )

            // 5. Action Section: Smart Review Center & Premium
            ActionSection(
                reviewCount = state.reviewCount,
                isPremium = state.isPremium,
                onReviewCenterClicked = onNavigateToReview,
                onPremiumClicked = onNavigateToPaywall
            )
        }

        if (state.showDailyTargetSheet) {
            DailyTargetBottomSheet(
                currentTarget = state.dailyTarget,
                isPremium = state.isPremium,
                availableTargets = state.availableDailyTargets,
                onDismiss = { viewModel.setDailyTargetSheetVisible(false) },
                onSelectTarget = { target ->
                    viewModel.selectDailyTarget(target)
                    viewModel.setDailyTargetSheetVisible(false)
                },
                onOpenPaywall = {
                    viewModel.setDailyTargetSheetVisible(false)
                    onNavigateToPaywall()
                }
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    greeting: String,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal)
            .padding(top = EnglishCoachSpacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)) {
            Text(
                text = greeting,
                style = EnglishCoachTypography.secondary.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = EnglishCoachColors.Purple
            )
            Text(
                text = "English Coach",
                style = EnglishCoachTypography.hero,
                color = EnglishCoachColors.TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
        ) {
            // Decorative Brand Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                    .padding(horizontal = EnglishCoachSpacing.md, vertical = EnglishCoachSpacing.sm)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                ) {
                    Icon(
                        imageVector = EnglishCoachIcons.Crown,
                        contentDescription = "English Coach",
                        tint = EnglishCoachColors.Orange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "TOEIC",
                        style = EnglishCoachTypography.caption.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = EnglishCoachColors.Purple
                    )
                }
            }

            // Settings Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EnglishCoachColors.Surface)
                    .border(1.dp, EnglishCoachColors.CardBorder, CircleShape)
                    .clickable(onClick = onNavigateToSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = EnglishCoachIcons.Settings,
                    contentDescription = "設定",
                    tint = EnglishCoachColors.Purple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TargetLevelSelector(
    selectedTarget: ToeicTarget,
    onTargetSelected: (ToeicTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val targets = listOf(
        ToeicTarget.BASIC,
        ToeicTarget.ADVANCED,
        ToeicTarget.GOLD
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EnglishCoachColors.Border.copy(alpha = 0.6f))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            targets.forEach { target ->
                val isSelected = target == selectedTarget
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) EnglishCoachColors.Surface else Color.Transparent)
                        .then(
                            if (isSelected) {
                                Modifier.shadow(2.dp, RoundedCornerShape(10.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable(role = Role.Tab) {
                            if (!isSelected) {
                                onTargetSelected(target)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = target.displayName,
                        style = EnglishCoachTypography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) EnglishCoachColors.TextPrimary else EnglishCoachColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyPracticeHeroCard(
    state: DashboardUiState,
    onCtaClicked: () -> Unit,
    onAdjustDailyTargetClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal),
        shape = EnglishCoachShapes.card,
        colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EnglishCoachSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress Ring
                ProgressRing(
                    progress = state.todayProgress,
                    size = 110.dp,
                    strokeWidth = 11.dp,
                    centerText = if (state.isUnlimitedTarget) "∞" else null
                )

                // Practice & Quota Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
                ) {
                    Text(
                        text = "每日練習",
                        style = EnglishCoachTypography.sectionHeader,
                        color = EnglishCoachColors.TextPrimary
                    )

                    val quizProgressText = if (state.isUnlimitedTarget) {
                        "今日測驗：${state.todayQuizCompletedCount} 題"
                    } else {
                        "今日測驗：${state.todayQuizCompletedCount} / ${state.dailyTarget} 題"
                    }
                    Text(
                        text = quizProgressText,
                        style = EnglishCoachTypography.secondary.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = EnglishCoachColors.Purple
                    )

                    val adjustTargetText = if (state.isUnlimitedTarget) {
                        "⚙️ 調整每日學習量：不限"
                    } else {
                        "⚙️ 調整每日學習量：${state.dailyTarget} 題"
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                            .clickable { onAdjustDailyTargetClicked() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = adjustTargetText,
                            style = EnglishCoachTypography.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = EnglishCoachColors.Purple
                        )
                        Icon(
                            imageVector = EnglishCoachIcons.ChevronRight,
                            contentDescription = "調整每日學習量",
                            tint = EnglishCoachColors.Purple,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (!state.isPremium) {
                        Text(
                            text = "今日免費額度：${state.dailyPracticeQuotaUsed} / ${state.maxPracticeQuota} 題",
                            style = EnglishCoachTypography.caption.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (state.isDailyLimitReached) EnglishCoachColors.Orange else EnglishCoachColors.TextSecondary
                        )
                    }
                }
            }

            // Dynamic CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(EnglishCoachShapes.button)
                    .background(
                        EnglishCoachGradients.purpleBlue
                    )
                    .clickable(role = Role.Button) {
                        onCtaClicked()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.ctaTitle,
                    style = EnglishCoachTypography.button,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StatisticsSection(
    learnedWords: Int,
    totalWords: Int,
    accuracy: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
    ) {
        Text(
            text = "統計",
            style = EnglishCoachTypography.sectionHeader,
            color = EnglishCoachColors.TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
        ) {
            // Stat 1: Learned Words
            StatBox(
                modifier = Modifier.weight(1f),
                title = "已學習",
                value = learnedWords.toString(),
                subTitle = "共 $totalWords 個單字",
                icon = EnglishCoachIcons.Book,
                color = EnglishCoachColors.Blue
            )

            // Stat 2: Accuracy
            StatBox(
                modifier = Modifier.weight(1f),
                title = "測驗正確率",
                value = String.format(Locale.US, "%.1f%%", accuracy),
                subTitle = "總體正確率",
                icon = EnglishCoachIcons.Percent,
                color = EnglishCoachColors.Green
            )
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    subTitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = EnglishCoachShapes.secondaryCard,
        colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EnglishCoachSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)) {
                Text(
                    text = value,
                    style = EnglishCoachTypography.screenTitle,
                    color = EnglishCoachColors.TextPrimary
                )
                Text(
                    text = title,
                    style = EnglishCoachTypography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = EnglishCoachColors.TextPrimary
                )
                Text(
                    text = subTitle,
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
private fun ActionSection(
    reviewCount: Int,
    isPremium: Boolean = false,
    onReviewCenterClicked: () -> Unit,
    onPremiumClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EnglishCoachSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
    ) {
        // Smart Review Center Action Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    onReviewCenterClicked()
                },
            shape = EnglishCoachShapes.secondaryCard,
            colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.Border.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(EnglishCoachSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
            ) {
                val iconColor = EnglishCoachColors.Orange
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = EnglishCoachIcons.CalendarClock,
                        contentDescription = "智慧複習中心",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                ) {
                    Text(
                        text = "智慧複習中心",
                        style = EnglishCoachTypography.button,
                        color = EnglishCoachColors.TextPrimary
                    )
                    Text(
                        text = if (reviewCount > 0) "${reviewCount} 個單字待加強與複習" else "目前沒有待複習單字",
                        style = EnglishCoachTypography.caption,
                        color = EnglishCoachColors.TextSecondary
                    )
                }

                if (reviewCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(EnglishCoachShapes.pill)
                            .background(EnglishCoachColors.Orange)
                            .padding(horizontal = EnglishCoachSpacing.sm, vertical = EnglishCoachSpacing.xxs),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reviewCount.toString(),
                            style = EnglishCoachTypography.caption.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }

                Icon(
                    imageVector = EnglishCoachIcons.ChevronRight,
                    contentDescription = null,
                    tint = EnglishCoachColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Premium Promotional Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    onPremiumClicked()
                },
            shape = EnglishCoachShapes.secondaryCard,
            colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isPremium) EnglishCoachColors.Green.copy(alpha = 0.3f)
                else EnglishCoachColors.Purple.copy(alpha = 0.2f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isPremium) listOf(
                                EnglishCoachColors.Green.copy(alpha = 0.08f),
                                EnglishCoachColors.Blue.copy(alpha = 0.05f)
                            ) else listOf(
                                EnglishCoachColors.Purple.copy(alpha = 0.08f),
                                EnglishCoachColors.Blue.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(EnglishCoachSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPremium) EnglishCoachColors.Green.copy(alpha = 0.15f)
                            else EnglishCoachColors.Orange.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPremium) EnglishCoachIcons.CheckCircle else EnglishCoachIcons.Crown,
                        contentDescription = "Premium",
                        tint = if (isPremium) EnglishCoachColors.Green else EnglishCoachColors.Orange,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                ) {
                    Text(
                        text = if (isPremium) "Premium 會員" else "解鎖 Premium 尊榮會員",
                        style = EnglishCoachTypography.button,
                        color = EnglishCoachColors.TextPrimary
                    )
                    Text(
                        text = if (isPremium) "已開通無限每日練習與智慧複習" else "解鎖全 3,600 單字庫、每日無限練習與智慧複習",
                        style = EnglishCoachTypography.caption,
                        color = EnglishCoachColors.TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                ) {
                    if (isPremium) {
                        Text(
                            text = "管理方案",
                            style = EnglishCoachTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = EnglishCoachColors.Purple
                        )
                    }
                    Icon(
                        imageVector = EnglishCoachIcons.ChevronRight,
                        contentDescription = if (isPremium) "管理方案" else "升級 Premium",
                        tint = if (isPremium) EnglishCoachColors.Purple else EnglishCoachColors.TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
