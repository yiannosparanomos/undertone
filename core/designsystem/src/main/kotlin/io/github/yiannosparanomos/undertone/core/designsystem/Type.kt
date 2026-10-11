package io.github.yiannosparanomos.undertone.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Headings and numbers use a display face; labels use monospace.
 * The dot-matrix display font (an OFL-licensed font such as Doto) is added in a later change;
 * until then [Display] falls back to monospace so layouts already have the right rhythm.
 */
val Display: FontFamily = FontFamily.Monospace
val Label: FontFamily = FontFamily.Monospace
val Body: FontFamily = FontFamily.SansSerif

val UtTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = Display,
                fontWeight = FontWeight.Normal,
                fontSize = 34.sp,
                letterSpacing = 2.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = Display,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
            ),
        titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge =
            TextStyle(
                fontFamily = Label,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = Label,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            ),
    )
