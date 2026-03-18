package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Typography

@Composable
fun CardTypeSelectScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "카드 종류 선택", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("등록할 카드 종류를 선택해주세요.", style = Typography.bodyMedium)
            Button(
                onClick = { navController.navigate(Route.CardInput.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("신용카드")
            }
            Button(
                onClick = { navController.navigate(Route.CardInput.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("체크카드")
            }
        }
    }
}
