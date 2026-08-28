package com.aura.launcher.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultAuraTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

var AuraTypography: Typography = DefaultAuraTypography

fun applyTypography(fontFamily: FontFamily = FontFamily.Default) {
    AuraTypography = DefaultAuraTypography.copy(
        headlineLarge = DefaultAuraTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = DefaultAuraTypography.headlineMedium.copy(fontFamily = fontFamily),
        titleLarge = DefaultAuraTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = DefaultAuraTypography.titleMedium.copy(fontFamily = fontFamily),
        bodyLarge = DefaultAuraTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = DefaultAuraTypography.bodyMedium.copy(fontFamily = fontFamily),
        labelSmall = DefaultAuraTypography.labelSmall.copy(fontFamily = fontFamily)
    )
}
