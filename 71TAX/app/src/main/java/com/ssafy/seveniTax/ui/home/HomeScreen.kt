package com.ssafy.seveniTax.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun HomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("7iTAX", style = Typography.headlineLarge, color = BrandPurple)
            Spacer(modifier = Modifier.height(16.dp))
            Text("홈 화면 (임시)", style = Typography.bodyMedium, color = TextSecondary)
        }
    }
}
