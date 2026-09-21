package com.andy.englishcoach.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * EnglishCoach Design System Vector Icon Wrapper.
 * Centralizes all vector icons across screens to eliminate system Emoji reliance.
 */
object EnglishCoachIcons {
    // Audio / Pronunciation
    val Volume: ImageVector = Icons.AutoMirrored.Filled.VolumeUp
    val Speaker: ImageVector = Icons.AutoMirrored.Filled.VolumeUp

    // Validation / Results
    val CheckCircle: ImageVector = Icons.Filled.CheckCircle
    val CloseCircle: ImageVector = Icons.Filled.Cancel
    val Check: ImageVector = Icons.Filled.Check
    val Close: ImageVector = Icons.Filled.Close
    val ThumbUp: ImageVector = Icons.Filled.ThumbUp
    val Percent: ImageVector = Icons.Filled.Percent

    // Status / Alerts
    val Warning: ImageVector = Icons.Filled.Warning
    val WarningAmber: ImageVector = Icons.Filled.WarningAmber
    val Calendar: ImageVector = Icons.Filled.CalendarMonth
    val CalendarClock: ImageVector = Icons.Filled.CalendarToday
    val Refresh: ImageVector = Icons.Filled.Refresh
    val Notification: ImageVector = androidx.compose.material.icons.Icons.Filled.Notifications

    // Badges / Honors
    val Crown: ImageVector = Icons.Filled.WorkspacePremium
    val Trophy: ImageVector = Icons.Filled.EmojiEvents

    // Content / Actions
    val Edit: ImageVector = Icons.Filled.Edit
    val Quiz: ImageVector = Icons.Filled.Edit
    val Book: ImageVector = Icons.AutoMirrored.Filled.MenuBook
    val ChevronLeft: ImageVector = Icons.AutoMirrored.Filled.ArrowBackIos
    val ChevronRight: ImageVector = Icons.AutoMirrored.Filled.ArrowForwardIos
    val ArrowBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val ArrowForward: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
    val ArrowUp: ImageVector = Icons.Filled.KeyboardArrowUp
    val ArrowDown: ImageVector = Icons.Filled.KeyboardArrowDown

    // Settings & Product Shell
    val Settings: ImageVector = Icons.Filled.Settings
    val Person: ImageVector = Icons.Filled.Person
    val Star: ImageVector = Icons.Filled.Star
    val Share: ImageVector = Icons.Filled.Share
    val OpenInNew: ImageVector = Icons.AutoMirrored.Filled.OpenInNew
    val Policy: ImageVector = Icons.Filled.Policy
    val Description: ImageVector = Icons.Filled.Description
    val Info: ImageVector = Icons.Filled.Info
    val Adjust: ImageVector = Icons.Filled.Adjust
    val Vibration: ImageVector = Icons.Filled.Vibration
}
