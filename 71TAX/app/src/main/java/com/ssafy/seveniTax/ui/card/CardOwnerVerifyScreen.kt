package com.ssafy.seveniTax.ui.card

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
fun CardOwnerVerifyScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "카드 소유자 확인", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("카드 소유자 본인 여부를 확인합니다.", style = Typography.bodyMedium)
            Spacer(modifier = Modifier.height(48.dp))
            TaxButton(
                text = "SMS 인증으로 확인",
                onClick = { navController.navigate(Route.CardSms.path) }
            )
        }
    }
}
