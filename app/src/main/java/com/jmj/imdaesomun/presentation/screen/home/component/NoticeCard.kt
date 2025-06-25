package com.jmj.imdaesomun.presentation.screen.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.jmj.imdaesomun.presentation.theme.AppMargin
import com.jmj.imdaesomun.presentation.theme.AppRadius
import com.jmj.imdaesomun.presentation.theme.AppTextStyle
import com.jmj.imdaesomun.presentation.theme.appShadowMedium

@Preview
@Composable
fun NoticeCard() {
    Card(
        modifier = Modifier
            .appShadowMedium(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(AppRadius.medium), // 모서리 둥글기
    ) {
        Column (
            modifier = Modifier
                .padding(AppMargin.medium),
            verticalArrangement = Arrangement.spacedBy(AppMargin.small)
        ) {
            Text(text = "신규", style = AppTextStyle.Caption1)
            Text(text = "2025년 역세권주택 입주자 모집", style = AppTextStyle.SubTitle1)
            Row (
                horizontalArrangement = Arrangement.spacedBy(AppMargin.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "2025.05.01", style = AppTextStyle.SubBody3)
                Text(text = "10,231", style = AppTextStyle.SubBody3)
                Text(text = "공공주택공급부", style = AppTextStyle.SubBody3)
            }
        }
    }
}