package com.jmj.imdaesomun.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTextStyle {
    // Headline
    val Headline = TextStyle(
        fontWeight = FontWeight.Bold, // 700
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.72).sp // -3% of 24
    )

    // Title1
    val Title1 = TextStyle(
        fontWeight = FontWeight.Bold, // 700
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.6).sp // -3% of 20
    )

    // Title2
    val Title2 = TextStyle(
        fontWeight = FontWeight.Medium, // 500
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.6).sp
    )

    // SubTitle1
    val SubTitle1 = TextStyle(
        fontWeight = FontWeight.Bold, // 700
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.54).sp
    )

    // SubTitle2
    val SubTitle2 = TextStyle(
        fontWeight = FontWeight.Medium, // 500
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.54).sp
    )

    // Body1
    val Body1 = TextStyle(
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.48).sp
    )

    // Body2
    val Body2 = TextStyle(
        fontWeight = FontWeight.Normal, // 400
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.48).sp
    )

    // SubBody1
    val SubBody1 = TextStyle(
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.42).sp
    )

    // SubBody2
    val SubBody2 = TextStyle(
        fontWeight = FontWeight.Medium, // 500
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.42).sp
    )

    // SubBody3
    val SubBody3 = TextStyle(
        fontWeight = FontWeight.Normal, // 400
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.42).sp
    )

    // Caption1
    val Caption1 = TextStyle(
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.36).sp
    )

    // Caption2
    val Caption2 = TextStyle(
        fontWeight = FontWeight.Normal, // 400
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.36).sp
    )
}
