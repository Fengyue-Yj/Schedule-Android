package com.schedule.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.schedule.app.R

val PingFangSC = FontFamily(
    Font(R.font.pingfang_sc, FontWeight.Normal),
    Font(R.font.pingfang_sc, FontWeight.Medium),
    Font(R.font.pingfang_sc, FontWeight.SemiBold),
    Font(R.font.pingfang_sc, FontWeight.Bold)
)

val AppTypography = Typography(
    // pageTitle: rounded semibold, large
    displayLarge = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    // sectionTitle: title3 semibold
    titleLarge = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    // cardTitle: headline
    headlineMedium = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    // titleMedium
    titleMedium = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.3).sp
    ),
    // rowTitle: body medium
    bodyMedium = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    // control: subheadline medium
    labelLarge = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    labelMedium = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    ),
    // caption: caption
    labelSmall = TextStyle(
        fontFamily = PingFangSC,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

val Typography.pageTitle: TextStyle get() = displayLarge
val Typography.sectionTitle: TextStyle get() = titleLarge
val Typography.cardTitle: TextStyle get() = headlineMedium
val Typography.rowTitle: TextStyle get() = bodyMedium
val Typography.control: TextStyle get() = labelLarge
val Typography.caption: TextStyle get() = labelSmall
