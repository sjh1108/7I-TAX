package com.ssafy.seveniTax.ui.pay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
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
fun PayTermsScreen(navController: NavController) {
    var agreed by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "Pay 약관 동의", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                Text("전자금융거래 이용약관 동의 [필수]", style = Typography.bodyMedium)
            }
            Spacer(modifier = Modifier.weight(1f))
            TaxButton(
                text = "동의하고 계속",
                onClick = { navController.navigate(Route.PayVerify.path) },
                enabled = agreed
            )
        }
    }
}
