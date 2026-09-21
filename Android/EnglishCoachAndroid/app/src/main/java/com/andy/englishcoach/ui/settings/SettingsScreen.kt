package com.andy.englishcoach.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
                        icon = EnglishCoachIcons.Quiz,
                        iconTint = EnglishCoachColors.Purple,
                        title = "每日學習目標",
                        value = "固定 ${state.dailyTarget} 題"
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        title = "今日完成進度",
                        value = "${state.todayCompletedCount} / ${state.dailyTarget} 題",
                        valueColor = if (state.todayCompletedCount >= state.dailyTarget) EnglishCoachColors.Orange else EnglishCoachColors.TextPrimary
                    )
                    HorizontalDivider(color = EnglishCoachColors.CardBorder.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        title = "剩餘免費額度",
                        value = "${state.remainingFreeQuestions} 題",
                        valueColor = EnglishCoachColors.TextSecondary
                    )
                }
            }
            Text(
                text = "自訂學習情境將於下一次新單字選題時生效。免費版每日固定享有 10 題練習。",
                style = EnglishCoachTypography.caption,
                color = EnglishCoachColors.TextSecondary,
                modifier = Modifier.padding(horizontal = EnglishCoachSpacing.sm, vertical = EnglishCoachSpacing.xxs)
            )

            // Section 3: 語音與音訊 (Audio / TTS)
            SettingsSectionHeader(title = "語音與音訊")
            Card(
                shape = EnglishCoachShapes.card,
                colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnglishCoachColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsSwitchRow(
                    icon = EnglishCoachIcons.Volume,
                    iconTint = EnglishCoachColors.Purple,
                    title = "自動朗讀單字",
                    subtitle = "進入卡片時自動發音，加強聽力與口說記憶",
                    checked = state.autoReadEnabled,
                    onCheckedChange = { viewModel.setAutoReadEnabled(it) }
                )
            }

            // Section 4: 互動與支持 (Interaction & Support)
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

            // Section 5: 社群 (Social)
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

            // Section 6: 關於 (About)
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
    }
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
    valueColor: Color = EnglishCoachColors.TextSecondary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EnglishCoachSpacing.cardPadding, vertical = EnglishCoachSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
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

        Text(
            text = value,
            style = EnglishCoachTypography.secondary.copy(fontWeight = FontWeight.Medium),
            color = valueColor
        )
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
