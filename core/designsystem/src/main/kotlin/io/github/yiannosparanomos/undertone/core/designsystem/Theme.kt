package io.github.yiannosparanomos.undertone.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * UnderTone palette: monochrome, with one red accent used sparingly
 * (active state, the one thing that needs attention). See docs/design/ui-direction.md.
 */
object UtColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Ink = Color(0xFF111111)
    val Paper = Color(0xFFF4F4F2)
    val GrayDark = Color(0xFF2A2A2A)
    val Gray = Color(0xFF8A8A8A)
    val GrayLight = Color(0xFFD6D6D3)
    val Accent = Color(0xFFE5322D)
}

private val DarkScheme: ColorScheme =
    darkColorScheme(
        primary = UtColors.White,
        onPrimary = UtColors.Black,
        secondary = UtColors.Gray,
        onSecondary = UtColors.Black,
        tertiary = UtColors.Accent,
        onTertiary = UtColors.White,
        background = UtColors.Black,
        onBackground = UtColors.White,
        surface = UtColors.Black,
        onSurface = UtColors.White,
        surfaceVariant = UtColors.Ink,
        onSurfaceVariant = UtColors.Gray,
        outline = UtColors.GrayDark,
        error = UtColors.Accent,
    )

private val LightScheme: ColorScheme =
    lightColorScheme(
        primary = UtColors.Black,
        onPrimary = UtColors.White,
        secondary = UtColors.Gray,
        onSecondary = UtColors.White,
        tertiary = UtColors.Accent,
        onTertiary = UtColors.White,
        background = UtColors.Paper,
        onBackground = UtColors.Black,
        surface = UtColors.Paper,
        onSurface = UtColors.Black,
        surfaceVariant = UtColors.White,
        onSurfaceVariant = UtColors.Gray,
        outline = UtColors.GrayLight,
        error = UtColors.Accent,
    )

@Composable
fun UnderToneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = UtTypography,
        content = content,
    )
}
