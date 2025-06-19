package com.jmj.imdaesomun.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jmj.imdaesomun.core.theme.AppColor
import com.jmj.imdaesomun.core.theme.AppTextStyle

@Preview
@Composable
fun HomeScreen() {
    // This is a placeholder for the home screen content.
    // You can replace this with your actual home screen UI.
    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Welcome to Imdaesomun",
                    style = AppTextStyle.Headline.copy(color = AppColor.Teal500)
                )
            }
        }
    )
}
