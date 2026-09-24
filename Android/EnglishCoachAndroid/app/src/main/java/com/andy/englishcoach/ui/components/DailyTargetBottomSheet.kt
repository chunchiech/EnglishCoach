package com.andy.englishcoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andy.englishcoach.billing.DailyTargetPolicy
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

/**
 * Reusable modal bottom sheet for adjusting daily learning target.
 * Shared between DashboardScreen and SettingsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTargetBottomSheet(
    currentTarget: Int,
    isPremium: Boolean,
    availableTargets: List<Int>,
    onDismiss: () -> Unit,
    onSelectTarget: (Int) -> Unit,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EnglishCoachColors.Surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EnglishCoachSpacing.screenHorizontal)
                .padding(bottom = EnglishCoachSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每日學習目標",
                    style = EnglishCoachTypography.sectionTitle,
                    color = EnglishCoachColors.TextPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "關閉",
                        color = EnglishCoachColors.Purple,
                        style = EnglishCoachTypography.bodyLarge
                    )
                }
            }

            Text(
                text = if (isPremium) "選擇每天練習的新單字數量" else "免費版固定每日 10 題，升級 Premium 解鎖自由調整",
                style = EnglishCoachTypography.caption,
                color = EnglishCoachColors.TextSecondary
            )

            HorizontalDivider(color = EnglishCoachColors.CardBorder)

            availableTargets.forEach { target ->
                val isSelected = currentTarget == target
                val isUnlocked = isPremium || target == 10
                val label = when (target) {
                    DailyTargetPolicy.UNLIMITED_TARGET -> "不限 (Unlimited)"
                    10 -> "10 題 (預設)"
                    else -> "$target 題"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(EnglishCoachShapes.card)
                        .clickable {
                            if (isUnlocked) {
                                onSelectTarget(target)
                            } else {
                                onOpenPaywall()
                            }
                        }
                        .padding(horizontal = EnglishCoachSpacing.cardPadding, vertical = EnglishCoachSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = EnglishCoachIcons.CheckCircle,
                                contentDescription = "已選擇",
                                tint = EnglishCoachColors.Purple,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(1.5.dp, EnglishCoachColors.CardBorder, CircleShape)
                            )
                        }

                        Text(
                            text = label,
                            style = EnglishCoachTypography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isUnlocked) EnglishCoachColors.TextPrimary else EnglishCoachColors.TextSecondary
                        )
                    }

                    if (!isUnlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EnglishCoachColors.Orange.copy(alpha = 0.12f))
                                .padding(horizontal = EnglishCoachSpacing.xs, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                style = EnglishCoachTypography.badge,
                                color = EnglishCoachColors.Orange
                            )
                        }
                    }
                }
            }
        }
    }
}
