package com.andy.englishcoach.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.andy.englishcoach.notification.DailyReminderScheduler
import com.andy.englishcoach.onboarding.model.LearningScenario
import com.andy.englishcoach.onboarding.model.ReminderTimeOption
import com.andy.englishcoach.ui.components.DailyTargetBottomSheet
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.billing.DailyTargetPolicy
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

/**
 * Android EnglishCoach Settings / Personalization & Product Shell.
 * Source of truth: iOS SettingsView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showEditProfileSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EnglishCoachColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "設定",
                        style = EnglishCoachTypography.sectionTitle,
                        color = EnglishCoachColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = EnglishCoachIcons.ArrowBack,
                            contentDescription = "返回",
                            tint = EnglishCoachColors.Purple
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EnglishCoachColors.Surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EnglishCoachSpacing.screenHorizontal, vertical = EnglishCoachSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xl)
        ) {
            // Section 1: 個人化 (Personalization / Profile)
            SettingsSectionHeader(title = "個人化")
            Card(
                shape = EnglishCoachShapes.card,
                colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsProfileRow(
                    avatarEmoji = state.avatarEmoji,
                    displayName = state.displayName,
                    onClick = { showEditProfileSheet = true }
                )
            }

            // Section 1.5: 會員方案 (Membership Plan)
            SettingsSectionHeader(title = "會員方案")
            Card(
                shape = EnglishCoachShapes.card,
                colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                if (state.isPremium) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenPaywall)
                            .padding(horizontal = EnglishCoachSpacing.cardPadding, vertical = EnglishCoachSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
                        ) {
                            Icon(
                                imageVector = EnglishCoachIcons.Star,
                                contentDescription = null,
                                tint = EnglishCoachColors.Orange,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "會員狀態",
                                    style = EnglishCoachTypography.bodyLarge,
                                    color = EnglishCoachColors.TextPrimary
                                )
                                Text(
                                    text = "Premium 會員",
                                    style = EnglishCoachTypography.caption,
                                    color = EnglishCoachColors.Purple
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
                        ) {
                            Text(
                                text = "管理／變更方案",
                                style = EnglishCoachTypography.secondary.copy(fontWeight = FontWeight.Medium),
                                color = EnglishCoachColors.Purple
                            )
                            Icon(
                                imageVector = EnglishCoachIcons.ChevronRight,
                                contentDescription = "管理／變更方案",
                                tint = EnglishCoachColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    SettingsActionRow(
                        icon = EnglishCoachIcons.Star,
                        iconTint = EnglishCoachColors.Orange,
                        title = "升級至 Premium 會員",
                        onClick = onOpenPaywall
                    )
                }
            }

            // Section 2: 學習 (Learning)
            SettingsSectionHeader(title = "學習")
            Card(
                shape = EnglishCoachShapes.card,
                colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsInfoRow(
                        icon = EnglishCoachIcons.Adjust,
                        iconTint = EnglishCoachColors.Purple,
                        title = "目標路徑",
                        value = state.targetLevel.displayName
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        icon = EnglishCoachIcons.Star,
                        iconTint = EnglishCoachColors.Purple,
                        title = "學習情境",
                        value = state.learningScenariosText?.replace("🎯 學習情境：", "") ?: "未設定",
                        onClick = { viewModel.setLearningScenariosSheetVisible(true) }
                    )
                    SettingsSwitchRow(
                        icon = EnglishCoachIcons.Notification,
                        iconTint = EnglishCoachColors.Purple,
                        title = "每日學習提醒",
                        subtitle = if (state.isReminderEnabled) "每天 ${state.reminderTimeText} 提醒背單字" else "定時提醒你回來學習",
                        checked = state.isReminderEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleReminder(
                                enabled = enabled,
                                onScheduleAlarm = { h, m ->
                                    DailyReminderScheduler.scheduleDailyReminder(context, h, m)
                                },
                                onCancelAlarm = {
                                    DailyReminderScheduler.cancelDailyReminder(context)
                                }
                            )
                        }
                    )
                    if (state.isReminderEnabled) {
                        HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                        SettingsInfoRow(
                            title = "提醒時間",
                            value = state.reminderTimeText,
                            onClick = { viewModel.setReminderTimeDialogVisible(true) }
                        )
                    }
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        icon = EnglishCoachIcons.Quiz,
                        iconTint = EnglishCoachColors.Purple,
                        title = "每日學習目標",
                        value = if (state.dailyTarget == DailyTargetPolicy.UNLIMITED_TARGET) "無限制" else "${state.dailyTarget} 題 / 天",
                        onClick = { viewModel.setDailyTargetSheetVisible(true) }
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        title = "今日完成進度",
                        value = "${state.todayCompletedCount} / ${if (state.dailyTarget == DailyTargetPolicy.UNLIMITED_TARGET) "∞" else state.dailyTarget.toString()} 題",
                        valueColor = if (state.dailyTarget != DailyTargetPolicy.UNLIMITED_TARGET && state.todayCompletedCount >= state.dailyTarget) EnglishCoachColors.Orange else EnglishCoachColors.TextPrimary
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        title = "剩餘免費額度",
                        value = if (state.isPremium) "無限制" else "${state.remainingFreeQuestions} 題",
                        valueColor = EnglishCoachColors.TextSecondary
                    )
                }
            }
            Text(
                text = if (state.isPremium) {
                    "自訂學習情境將於下一次新單字選題時生效。目前為 Premium 會員，已解鎖自訂每日目標與無限制複習測驗。"
                } else {
                    "自訂學習情境將於下一次新單字選題時生效。免費版每日固定享有 10 題練習，升級 Premium 可解鎖自訂目標與無限制複習測驗。"
                },
                style = EnglishCoachTypography.caption,
                color = EnglishCoachColors.TextSecondary,
                modifier = Modifier.padding(horizontal = EnglishCoachSpacing.sm, vertical = EnglishCoachSpacing.xxs)
            )

            // Section 3: 互動與支持 (Interaction & Support)
            SettingsSectionHeader(title = "互動與支持")
            Card(
                shape = EnglishCoachShapes.card,
                colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsActionRow(
                        icon = EnglishCoachIcons.Star,
                        iconTint = EnglishCoachColors.Orange,
                        title = "幫我們評分",
                        onClick = { rateApp(context) }
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsActionRow(
                        icon = EnglishCoachIcons.Share,
                        iconTint = EnglishCoachColors.Purple,
                        title = "分享 APP 給朋友",
                        onClick = { shareApp(context) }
                    )
                }
            }

            // Section 4: 社群 (Social)
            SettingsSectionHeader(title = "社群")
            Card(
                shape = EnglishCoachShapes.card,
                colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsExternalRow(
                        title = "追蹤 EnglishCoach IG",
                        subtitle = "@englishcoach_toeic_tw",
                        icon = EnglishCoachIcons.OpenInNew,
                        onClick = { openInstagram(context) }
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsExternalRow(
                        title = "追蹤 EnglishCoach Threads",
                        subtitle = "@englishcoach_toeic_tw",
                        icon = EnglishCoachIcons.OpenInNew,
                        onClick = { openThreads(context) }
                    )
                }
            }

            // Section 5: 關於 (About)
            SettingsSectionHeader(title = "關於")
            Card(
                shape = EnglishCoachShapes.card,
                colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsInfoRow(
                        title = "App 名稱",
                        value = "EnglishCoach"
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        title = "副標題",
                        value = "多益單字與口說教練"
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        title = "版本資訊",
                        value = state.versionName
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsExternalRow(
                        title = "隱私權政策 (Privacy Policy)",
                        icon = EnglishCoachIcons.OpenInNew,
                        onClick = {
                            openUrl(context, "https://chunchiech.github.io/EnglishCoach/privacy-policy.html")
                        }
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsExternalRow(
                        title = "使用條款 (Terms of Service)",
                        icon = EnglishCoachIcons.OpenInNew,
                        onClick = {
                            openUrl(context, "https://chunchiech.github.io/EnglishCoach/terms-of-service.html")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxl))
        }

        if (showEditProfileSheet) {
            ProfileEditBottomSheet(
                currentName = state.displayName,
                currentEmoji = state.avatarEmoji,
                availableEmojis = state.availableEmojis,
                onDismiss = { showEditProfileSheet = false },
                onSave = { name, emoji ->
                    viewModel.updateProfile(name, emoji)
                    showEditProfileSheet = false
                }
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
                    onOpenPaywall()
                }
            )
        }

        if (state.showLearningScenariosSheet) {
            LearningScenariosBottomSheet(
                selectedScenarioIds = state.selectedScenarioIds,
                onDismiss = { viewModel.setLearningScenariosSheetVisible(false) },
                onToggle = { viewModel.toggleScenario(it) },
                onMoveUp = { viewModel.moveScenarioUp(it) },
                onMoveDown = { viewModel.moveScenarioDown(it) }
            )
        }

        if (state.showReminderTimeDialog) {
            ReminderTimeDialog(
                selectedTimeText = state.reminderTimeText,
                onDismiss = { viewModel.setReminderTimeDialogVisible(false) },
                onSelectOption = { option ->
                    viewModel.updateReminderTime(
                        hour = option.hour,
                        minute = option.minute,
                        onScheduleAlarm = { h, m ->
                            DailyReminderScheduler.scheduleDailyReminder(context, h, m)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ReminderTimeDialog(
    selectedTimeText: String,
    onDismiss: () -> Unit,
    onSelectOption: (ReminderTimeOption) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "選擇提醒時間",
                style = EnglishCoachTypography.sectionHeader,
                color = EnglishCoachColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)) {
                ReminderTimeOption.PRESET_OPTIONS.forEach { option ->
                    val isSelected = selectedTimeText == option.displayTime
                    androidx.compose.material3.Surface(
                        shape = EnglishCoachShapes.card,
                        color = if (isSelected) EnglishCoachColors.Purple.copy(alpha = 0.08f) else EnglishCoachColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectOption(option) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = EnglishCoachSpacing.md, vertical = EnglishCoachSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = option.displayTime,
                                    style = EnglishCoachTypography.sectionHeader,
                                    color = EnglishCoachColors.TextPrimary
                                )
                                Text(
                                    text = option.periodTag,
                                    style = EnglishCoachTypography.caption,
                                    color = EnglishCoachColors.TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = EnglishCoachIcons.Check,
                                    contentDescription = "已選取",
                                    tint = EnglishCoachColors.Purple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉", color = EnglishCoachColors.Purple)
            }
        },
        containerColor = EnglishCoachColors.Surface,
        shape = EnglishCoachShapes.dialog
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = EnglishCoachTypography.caption.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = EnglishCoachColors.TextSecondary,
        modifier = Modifier.padding(start = EnglishCoachSpacing.sm, bottom = EnglishCoachSpacing.xxs)
    )
}

@Composable
private fun SettingsProfileRow(
    avatarEmoji: String,
    displayName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(EnglishCoachSpacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(EnglishCoachColors.Purple.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarEmoji,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.width(EnglishCoachSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "修改名稱＆大頭貼",
                style = EnglishCoachTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = EnglishCoachColors.TextPrimary
            )
            Text(
                text = displayName,
                style = EnglishCoachTypography.secondary,
                color = EnglishCoachColors.TextSecondary
            )
        }

        Icon(
            imageVector = EnglishCoachIcons.ChevronRight,
            contentDescription = "編輯",
            tint = EnglishCoachColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsInfoRow(
    title: String,
    value: String,
    icon: ImageVector? = null,
    iconTint: Color = EnglishCoachColors.Purple,
    valueColor: Color = EnglishCoachColors.TextSecondary,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = EnglishCoachSpacing.cardPadding, vertical = EnglishCoachSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = EnglishCoachTypography.bodyLarge,
                color = EnglishCoachColors.TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
        ) {
            Text(
                text = value,
                style = EnglishCoachTypography.secondary.copy(fontWeight = FontWeight.Medium),
                color = valueColor
            )
            if (onClick != null) {
                Icon(
                    imageVector = EnglishCoachIcons.ChevronRight,
                    contentDescription = null,
                    tint = EnglishCoachColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconTint: Color = EnglishCoachColors.Purple
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(EnglishCoachSpacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(EnglishCoachSpacing.sm))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = EnglishCoachTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = EnglishCoachColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = EnglishCoachTypography.caption,
                color = EnglishCoachColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(EnglishCoachSpacing.sm))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EnglishCoachColors.Surface,
                checkedTrackColor = EnglishCoachColors.Purple,
                uncheckedThumbColor = EnglishCoachColors.Surface,
                uncheckedTrackColor = EnglishCoachColors.CardBorder
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = EnglishCoachSpacing.cardPadding, vertical = EnglishCoachSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(EnglishCoachSpacing.sm))

        Text(
            text = title,
            style = EnglishCoachTypography.bodyLarge,
            color = EnglishCoachColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = EnglishCoachIcons.ChevronRight,
            contentDescription = null,
            tint = EnglishCoachColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsExternalRow(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = EnglishCoachSpacing.cardPadding, vertical = EnglishCoachSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = EnglishCoachTypography.bodyLarge,
            color = EnglishCoachColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = EnglishCoachTypography.caption,
                color = EnglishCoachColors.TextSecondary,
                modifier = Modifier.padding(end = EnglishCoachSpacing.xs)
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = "開啟連結",
            tint = EnglishCoachColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditBottomSheet(
    currentName: String,
    currentEmoji: String,
    availableEmojis: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempName by remember { mutableStateOf(currentName) }
    var tempEmoji by remember { mutableStateOf(currentEmoji) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EnglishCoachColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EnglishCoachSpacing.screenHorizontal)
                .padding(bottom = EnglishCoachSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "取消",
                        color = EnglishCoachColors.TextSecondary,
                        style = EnglishCoachTypography.bodyLarge
                    )
                }

                Text(
                    text = "個人資料",
                    style = EnglishCoachTypography.sectionTitle,
                    color = EnglishCoachColors.TextPrimary
                )

                TextButton(
                    onClick = {
                        val trimmed = tempName.trim()
                        val finalName = if (trimmed.isNotEmpty()) trimmed else currentName
                        onSave(finalName, tempEmoji)
                    }
                ) {
                    Text(
                        text = "儲存",
                        color = EnglishCoachColors.Purple,
                        style = EnglishCoachTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            HorizontalDivider(color = EnglishCoachColors.CardBorder)

            // Section: 選擇大頭貼
            Text(
                text = "選擇大頭貼",
                style = EnglishCoachTypography.caption.copy(fontWeight = FontWeight.Bold),
                color = EnglishCoachColors.TextSecondary
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableEmojis) { emoji ->
                    val isSelected = tempEmoji == emoji
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) EnglishCoachColors.Purple.copy(alpha = 0.18f)
                                else EnglishCoachColors.CardBorder.copy(alpha = 0.25f)
                            )
                            .then(
                                if (isSelected) Modifier.border(2.dp, EnglishCoachColors.Purple, CircleShape)
                                else Modifier
                            )
                            .clickable { tempEmoji = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 22.sp
                        )
                    }
                }
            }

            // Section: 學習者暱稱
            Text(
                text = "學習者暱稱",
                style = EnglishCoachTypography.caption.copy(fontWeight = FontWeight.Bold),
                color = EnglishCoachColors.TextSecondary
            )

            OutlinedTextField(
                value = tempName,
                onValueChange = { tempName = it },
                placeholder = { Text("請輸入您的名稱") },
                singleLine = true,
                shape = EnglishCoachShapes.card,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EnglishCoachColors.Purple,
                    unfocusedBorderColor = EnglishCoachColors.CardBorder,
                    cursorColor = EnglishCoachColors.Purple
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// MARK: - External Intent Utilities

private fun openUrl(context: Context, urlString: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        // Safe fallback in environments without browser
    }
}

private fun shareApp(context: Context) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                "推薦好用的英文學習 App：EnglishCoach"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "推薦你這款「EnglishCoach - 多益單字與口說教練」，每天練習高效累積多益核心單字！\nhttps://chunchiech.github.io/EnglishCoach/"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "分享 EnglishCoach").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        // Safe fallback
    }
}

private fun rateApp(context: Context) {
    val packageName = context.packageName
    try {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$packageName")
    } catch (_: Exception) {
        // Safe fallback
    }
}

private fun openInstagram(context: Context) {
    try {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username=englishcoach_toeic_tw")).apply {
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(appIntent)
    } catch (_: Exception) {
        openUrl(context, "https://www.instagram.com/englishcoach_toeic_tw/")
    }
}

private fun openThreads(context: Context) {
    try {
        val threadsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("barcelona://user?username=englishcoach_toeic_tw")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(threadsIntent)
    } catch (_: Exception) {
        openUrl(context, "https://www.threads.net/@englishcoach_toeic_tw")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearningScenariosBottomSheet(
    selectedScenarioIds: List<String>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EnglishCoachColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EnglishCoachSpacing.screenHorizontal)
                .padding(bottom = EnglishCoachSpacing.xxl)
        ) {
            Text(
                text = "自訂您的學習情境",
                style = EnglishCoachTypography.sectionTitle,
                color = EnglishCoachColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(EnglishCoachSpacing.xxs))
            Text(
                text = "可複選，已按您的選擇順序排列優先級。\n系統將依據您設定的情境優先推薦相關高頻單字。",
                style = EnglishCoachTypography.caption,
                color = EnglishCoachColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(EnglishCoachSpacing.lg))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LearningScenario.AVAILABLE_SCENARIOS.forEach { scenario ->
                    val isSelected = selectedScenarioIds.contains(scenario.id)
                    val priorityIndex = selectedScenarioIds.indexOf(scenario.id)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(scenario.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = scenario.title,
                                        fontSize = 16.sp,
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

                                        if (selectedScenarioIds.size > 1) {
                                            Spacer(modifier = Modifier.width(4.dp))
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

                                            if (priorityIndex < selectedScenarioIds.size - 1) {
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

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = scenario.subtitle,
                                    style = EnglishCoachTypography.caption,
                                    color = EnglishCoachColors.TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

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

            Spacer(modifier = Modifier.height(EnglishCoachSpacing.xl))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnglishCoachColors.Purple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
