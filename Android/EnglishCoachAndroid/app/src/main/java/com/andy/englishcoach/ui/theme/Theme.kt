package com.andy.englishcoach.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val EnglishCoachLightColorScheme = lightColorScheme(
    primary = EnglishCoachColors.Purple,
    onPrimary = Color.White,
    primaryContainer = EnglishCoachColors.Purple.copy(alpha = 0.12f),
    onPrimaryContainer = EnglishCoachColors.Purple,

    secondary = EnglishCoachColors.Blue,
    onSecondary = Color.White,
    secondaryContainer = EnglishCoachColors.Blue.copy(alpha = 0.12f),
    onSecondaryContainer = EnglishCoachColors.Blue,

    tertiary = EnglishCoachColors.Orange,
    onTertiary = Color.White,
    tertiaryContainer = EnglishCoachColors.Orange.copy(alpha = 0.12f),
    onTertiaryContainer = EnglishCoachColors.Orange,

    background = EnglishCoachColors.Background,
    onBackground = EnglishCoachColors.TextPrimary,

    surface = EnglishCoachColors.Surface,
    onSurface = EnglishCoachColors.TextPrimary,
    surfaceVariant = EnglishCoachColors.SurfaceVariant,
    onSurfaceVariant = EnglishCoachColors.TextSecondary,

    outline = EnglishCoachColors.Border,
    outlineVariant = EnglishCoachColors.Border.copy(alpha = 0.6f),

    error = EnglishCoachColors.Red,
    onError = Color.White,
    errorContainer = EnglishCoachColors.Red.copy(alpha = 0.12f),
    onErrorContainer = EnglishCoachColors.Red
)

private val EnglishCoachDarkColorScheme = darkColorScheme(
    primary = EnglishCoachColors.Purple,
    onPrimary = Color.White,
    primaryContainer = EnglishCoachColors.Purple.copy(alpha = 0.2f),
    onPrimaryContainer = EnglishCoachColors.Purple,

    secondary = EnglishCoachColors.Blue,
    onSecondary = Color.White,
    secondaryContainer = EnglishCoachColors.Blue.copy(alpha = 0.2f),
    onSecondaryContainer = EnglishCoachColors.Blue,

    tertiary = EnglishCoachColors.Orange,
    onTertiary = Color.White,
    tertiaryContainer = EnglishCoachColors.Orange.copy(alpha = 0.2f),
    onTertiaryContainer = EnglishCoachColors.Orange,

    background = EnglishCoachColors.BackgroundDark,
    onBackground = EnglishCoachColors.TextPrimaryDark,

    surface = EnglishCoachColors.SurfaceDark,
    onSurface = EnglishCoachColors.TextPrimaryDark,
    surfaceVariant = EnglishCoachColors.SurfaceVariantDark,
    onSurfaceVariant = EnglishCoachColors.TextSecondaryDark,

    outline = EnglishCoachColors.BorderDark,
    outlineVariant = EnglishCoachColors.BorderDark.copy(alpha = 0.6f),

    error = EnglishCoachColors.Red,
    onError = Color.White,
    errorContainer = EnglishCoachColors.Red.copy(alpha = 0.2f),
    onErrorContainer = EnglishCoachColors.Red
)

@Composable
fun EnglishCoachAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Fixed brand color palette; default to false to prevent wallpaper dynamic color from overriding brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EnglishCoachDarkColorScheme
        else -> EnglishCoachLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}