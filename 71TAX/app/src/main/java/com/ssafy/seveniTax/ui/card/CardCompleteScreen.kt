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
fun CardCompleteScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "카드 등록 완료")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("카드 등록이 완료되었습니다!", style = Typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("이제 세금 환급금 자동 납부가 가능해요.", style = Typography.bodyMedium)
            Spacer(modifier = Modifier.height(48.dp))
            TaxButton(
                text = "카드 목록으로",
                onClick = {
                    navController.navigate(Route.CardList.path) {
                        popUpTo(Route.CardList.path) { inclusive = true }
                    }
                }
            )
        }
    }
}
