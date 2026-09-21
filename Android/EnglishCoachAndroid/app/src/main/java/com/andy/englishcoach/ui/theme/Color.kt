package com.andy.englishcoach.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * EnglishCoach Design System Color Tokens.
 * Source of Truth: iOS EnglishCoach Production UI.
 */
object EnglishCoachColors {
    // Primary Brand Colors
    val Purple = Color(0xFFAF52DE)       // iOS System Purple (#AF52DE)
    val Blue = Color(0xFF007AFF)         // iOS System Blue (#007AFF)
    val Indigo = Color(0xFF5856D6)       // iOS System Indigo (#5856D6)
    val Green = Color(0xFF34C759)        // iOS System Green (#34C759)
    val Orange = Color(0xFFFF9500)       // iOS System Orange (#FF9500)
    val Red = Color(0xFFFF3B30)          // iOS System Red (#FF3B30)

    // Neutral Colors - Light Theme
    val Background = Color(0xFFF2F2F7)   // iOS systemGroupedBackground (#F2F2F7)
    val Surface = Color(0xFFFFFFFF)      // iOS secondarySystemGroupedBackground (#FFFFFF)
    val SurfaceVariant = Color(0xFFE5E5EA)
    val TextPrimary = Color(0xFF1C1C1E)  // iOS label / primary text
    val TextSecondary = Color(0xFF8E8E93)// iOS secondaryLabel / secondary text
    val Border = Color(0xFFE5E5EA)       // iOS separator / border
    val CardBorder = Color(0xFFE5E5EA)   // Border alias for card outlines
    val Disabled = Color(0xFFD1D1D6)

    // Neutral Colors - Dark Theme
    val BackgroundDark = Color(0xFF000000)
    val SurfaceDark = Color(0xFF1C1C1E)
    val SurfaceVariantDark = Color(0xFF2C2C2E)
    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondaryDark = Color(0xFF98989D)
    val BorderDark = Color(0xFF38383A)
}

// Top-level aliases for direct access
val EnglishCoachPurple = EnglishCoachColors.Purple
val EnglishCoachBlue = EnglishCoachColors.Blue
val EnglishCoachIndigo = EnglishCoachColors.Indigo
val EnglishCoachGreen = EnglishCoachColors.Green
val EnglishCoachOrange = EnglishCoachColors.Orange
val EnglishCoachRed = EnglishCoachColors.Red
val EnglishCoachBackground = EnglishCoachColors.Background
val EnglishCoachSurface = EnglishCoachColors.Surface
val EnglishCoachTextPrimary = EnglishCoachColors.TextPrimary
val EnglishCoachTextSecondary = EnglishCoachColors.TextSecondary
val EnglishCoachBorder = EnglishCoachColors.Border
val EnglishCoachDisabled = EnglishCoachColors.Disabled

// Backward compatibility with previous M3 defaults
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFFAF52DE)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)