package com.ssafy.seveniTax.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.TextSecondary
import com.ssafy.seveniTax.ui.theme.Typography

@Composable
fun SettingsScreen(navController: NavController, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Text("설정 (임시)", style = Typography.bodyMedium, color = TextSecondary)
    }
}
