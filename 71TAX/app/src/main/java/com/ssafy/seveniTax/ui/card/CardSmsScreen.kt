package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.components.TaxInput
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Typography
import com.ssafy.seveniTax.viewmodel.CardViewModel

@Composable
fun CardSmsScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "SMS 인증", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("카드 등록을 위한 SMS 인증번호를 입력해주세요.", style = Typography.bodyMedium)
            TaxInput(
                value = uiState.smsCode,
                onValueChange = { if (it.length <= 6) viewModel.updateSmsCode(it) },
                placeholder = "인증번호 6자리",
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.weight(1f))
            TaxButton(
                text = "카드 등록",
                onClick = {
                    viewModel.registerCard(
                        onSuccess = { navController.navigate(Route.CardComplete.path) }
                    )
                },
                enabled = uiState.smsCode.length == 6 && !uiState.isLoading
            )
        }
    }
}
