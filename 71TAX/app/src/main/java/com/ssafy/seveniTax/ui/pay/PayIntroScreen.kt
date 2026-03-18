package com.ssafy.seveniTax.ui.pay

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
fun PayIntroScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "Pay 개설")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("7iTAX Pay를 개설하면", style = Typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("세금 환급금을 바로 받을 수 있어요.", style = Typography.bodyMedium)
            Spacer(modifier = Modifier.height(48.dp))
            TaxButton(
                text = "Pay 개설하기",
                onClick = { navController.navigate(Route.PayTerms.path) }
            )
        }
    }
}
