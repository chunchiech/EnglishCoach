package com.andy.englishcoach.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * EnglishCoach Design System Shape Tokens.
 * Matches iOS corner radii specifications:
 * - Card: 24dp
 * - Secondary Card: 20dp
 * - Button: 16dp
 * - Option: 14dp
 * - Secondary Button / Dialog: 12dp
 * - Badge: 8dp
 * - Chip: 6dp
 */
object EnglishCoachShapes {
    val card = RoundedCornerShape(24.dp)
    val secondaryCard = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(16.dp)
    val option = RoundedCornerShape(14.dp)
    val secondaryButton = RoundedCornerShape(12.dp)
    val dialog = RoundedCornerShape(16.dp)
    val badge = RoundedCornerShape(8.dp)
    val chip = RoundedCornerShape(6.dp)
    val pill = RoundedCornerShape(20.dp)
}
