package com.jmj.imdaesomun.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jmj.imdaesomun.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_regular, FontWeight.Normal),
)

object AppTextStyle {
    /**
     * Headline
     *
     * 24sp, 700 weight, 32sp line height, -3% letter spacing
     */
    val Headline = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.72).sp // -3% of 24
    )

    /**
     * Title1
     *
     * 20sp, 700 weight, 28sp line height, -3% letter spacing
     */
    val Title1 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.6).sp // -3% of 20
    )

    /**
     * Title2
     *
     * 20sp, 500 weight, 28sp line height, -3% letter spacing
     */
    val Title2 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.6).sp
    )

    /**
     * SubTitle1
     *
     * 18sp, 700 weight, 24sp line height, -3% letter spacing
     */
    val SubTitle1 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.54).sp
    )

    /**
     * SubTitle2
     *
     * 18sp, 500 weight, 24sp line height, -3% letter spacing
     */
    val SubTitle2 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.54).sp
    )

    /**
     * Body1
     *
     * 16sp, 600 weight, 22sp line height, -3% letter spacing
     */
    val Body1 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.48).sp
    )

    /**
     * Body2
     *
     * 16sp, 400 weight, 22sp line height, -3% letter spacing
     */
    val Body2 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.48).sp
    )

    /**
     * SubBody1
     *
     * 14sp, 600 weight, 20sp line height, -3% letter spacing
     */
    val SubBody1 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.42).sp
    )

    /**
     * SubBody2
     *
     * 14sp, 500 weight, 20sp line height, -3% letter spacing
     */
    val SubBody2 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.42).sp
    )

    /**
     * SubBody3
     *
     * 14sp, 400 weight, 20sp line height, -3% letter spacing
     */
    val SubBody3 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.42).sp
    )

    /**
     * Caption1
     *
     * 12sp, 600 weight, 18sp line height, -3% letter spacing
     */
    val Caption1 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.36).sp
    )

    /**
     * Caption2
     *
     * 12sp, 400 weight, 18sp line height, -3% letter spacing
     */
    val Caption2 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.36).sp
    )
}
