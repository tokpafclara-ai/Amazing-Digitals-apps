package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueContainer,
    onPrimaryContainer = OnRoyalBlueContainer,
    secondary = NavyPrimary,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = NavyPrimary,
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = DangerRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlueLight,
    onPrimary = Color.White,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = RoyalBlueContainer,
    secondary = GoldLight,
    onSecondary = NavyDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = GoldLight,
    onTertiary = NavyDark,
    tertiaryContainer = OnGoldContainer,
    onTertiaryContainer = GoldContainer,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFF87171),
    onError = NavyDark
)

@Composable
fun AmazingOfficeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
