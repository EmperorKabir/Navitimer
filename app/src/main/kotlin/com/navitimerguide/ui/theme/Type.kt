package com.navitimerguide.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = TextStyle(fontFamily = FontFamily.Default)

val NavitimerTypography = Typography(
    displayLarge = Default.copy(fontWeight = FontWeight.Bold, fontSize = 36.sp),
    titleLarge = Default.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = Default.copy(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = Default.copy(fontSize = 16.sp),
    bodyMedium = Default.copy(fontSize = 14.sp),
    bodySmall = Default.copy(fontSize = 12.sp),
    labelLarge = Default.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = Default.copy(fontSize = 11.sp, letterSpacing = 0.5.sp)
)
