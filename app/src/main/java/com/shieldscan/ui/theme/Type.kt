package com.shieldscan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ShieldTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyMedium = Typography().bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Medium)
)
