package com.andy.englishcoach.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * EnglishCoach Design System Gradient Tokens and Helpers.
 * Matches iOS LinearGradients.
 */
object EnglishCoachGradients {
    /**
     * Primary brand action gradient: Purple -> Blue
     * Used for main CTA buttons, progress bars, card accents.
     */
    val purpleBlue: Brush = Brush.linearGradient(
        colors = listOf(EnglishCoachColors.Purple, EnglishCoachColors.Blue)
    )

    /**
     * Interactive flip gradient: Purple -> Indigo
     * Used for "查看中文意思" prompt button.
     */
    val purpleIndigo: Brush = Brush.linearGradient(
        colors = listOf(EnglishCoachColors.Purple, EnglishCoachColors.Indigo)
    )

    /**
     * Celebration / Crown gradient: Purple -> Orange
     * Used for completion celebrations, premium highlights.
     */
    val purpleOrange: Brush = Brush.linearGradient(
        colors = listOf(EnglishCoachColors.Purple, EnglishCoachColors.Orange)
    )

    /**
     * Trophy / Milestone gradient: Orange -> Yellow
     */
    val orangeYellow: Brush = Brush.linearGradient(
        colors = listOf(EnglishCoachColors.Orange, Color(0xFFFFCC00))
    )

    /**
     * Success gradient: Green -> Blue
     * Used for 100% quiz completion CTA button.
     */
    val greenBlue: Brush = Brush.linearGradient(
        colors = listOf(EnglishCoachColors.Green, EnglishCoachColors.Blue)
    )

    /**
     * Subtle 1.5dp card border gradient
     */
    val cardBorder: Brush = Brush.linearGradient(
        colors = listOf(
            EnglishCoachColors.Purple.copy(alpha = 0.25f),
            EnglishCoachColors.Blue.copy(alpha = 0.15f)
        )
    )
}

/**
 * Convenience helper to create an EnglishCoach standard linear gradient.
 */
fun englishCoachGradient(
    colors: List<Color> = listOf(EnglishCoachColors.Purple, EnglishCoachColors.Blue)
): Brush = Brush.linearGradient(colors)
