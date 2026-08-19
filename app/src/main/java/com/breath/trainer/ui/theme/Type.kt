package com.breath.trainer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val defaultHeadline = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = 0.sp,
)

val BreathTypography = Typography(
    displayLarge = defaultHeadline.copy(fontSize = 36.sp, lineHeight = 44.sp),
    displayMedium = defaultHeadline.copy(fontSize = 30.sp, lineHeight = 38.sp),
    headlineLarge = defaultHeadline.copy(fontSize = 26.sp),
    headlineMedium = defaultHeadline.copy(fontSize = 22.sp),
    headlineSmall = defaultHeadline.copy(fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
