package com.navitimerguide.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.navitimerguide.R

/**
 * Saira Condensed — a free condensed sans-serif used as a stand-in for the
 * Breitling brand font (which isn't open-source). Applied to ALL UI text
 * outside the watch face: equation panel, preset chips, input labels,
 * input numbers, every label.
 */
private val Saira = FontFamily(
    Font(R.font.saira_condensed_regular, FontWeight.Normal),
    Font(R.font.saira_condensed_medium, FontWeight.Medium),
    Font(R.font.saira_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.saira_condensed_bold, FontWeight.Bold)
)

private val Default = TextStyle(fontFamily = Saira)

val NavitimerTypography = Typography(
    displayLarge = Default.copy(fontWeight = FontWeight.Bold, fontSize = 36.sp),
    titleLarge = Default.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = Default.copy(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = Default.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = Default.copy(fontSize = 16.sp),
    bodyMedium = Default.copy(fontSize = 14.sp),
    bodySmall = Default.copy(fontSize = 12.sp),
    labelLarge = Default.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = Default.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = Default.copy(fontSize = 11.sp, letterSpacing = 0.5.sp)
)
