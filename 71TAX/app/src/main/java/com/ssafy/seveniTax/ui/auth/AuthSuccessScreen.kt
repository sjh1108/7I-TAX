package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Typography

@Composable
fun AuthSuccessScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "인증 완료")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("본인인증이 완료되었습니다.", style = Typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("7iTAX 서비스를 시작합니다.", style = Typography.bodyMedium)
            Spacer(modifier = Modifier.height(48.dp))
            TaxButton(
                text = "시작하기",
                onClick = {
                    navController.navigate(Route.PayIntro.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                }
            )
        }
    }
}
