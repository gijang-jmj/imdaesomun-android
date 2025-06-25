package com.jmj.imdaesomun.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.appShadowMedium(
    shape: RoundedCornerShape = RoundedCornerShape(AppRadius.medium)
): Modifier = this.shadow(
    elevation = 2.dp,
    shape = shape,
    ambientColor = Color(0x0D000000), // 0x0D = 5% alpha
    spotColor = Color(0x0D000000),    // 0x0D = 5% alpha
    clip = false
)
