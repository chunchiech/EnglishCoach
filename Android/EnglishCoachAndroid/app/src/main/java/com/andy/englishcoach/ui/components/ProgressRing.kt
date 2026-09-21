package com.andy.englishcoach.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.ui.theme.EnglishCoachColors

/**
 * Reusable Progress Ring component matching iOS ProgressRing.swift.
 * Displays an animated circular progress indicator with percentage and subtitle in center.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    primaryColor: Color = EnglishCoachColors.Purple,
    secondaryColor: Color = EnglishCoachColors.Border.copy(alpha = 0.5f),
    centerText: String? = null
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "ProgressRingAnimation"
    )

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    val percentageInt = (clampedProgress * 100).toInt()
    val displayText = centerText ?: "$percentageInt%"

    val numberFontSize = (size.value * 0.22f).sp
    val labelFontSize = (size.value * 0.085f).sp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val halfStroke = strokeWidthPx / 2f
            val arcSize = Size(
                width = this.size.width - strokeWidthPx,
                height = this.size.height - strokeWidthPx
            )
            val topLeft = Offset(halfStroke, halfStroke)

            // Background Track
            drawArc(
                color = secondaryColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Animated Progress Arc
            if (animatedProgress > 0f) {
                val progressBrush = Brush.linearGradient(
                    colors = listOf(primaryColor, EnglishCoachColors.Blue),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height)
                )

                drawArc(
                    brush = progressBrush,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayText,
                fontSize = numberFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = EnglishCoachColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "今日進度",
                fontSize = labelFontSize,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = EnglishCoachColors.TextSecondary
            )
        }
    }
}
