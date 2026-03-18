package com.ssafy.seveniTax.ui.auth

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
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun ResidentNumberScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "본인인증", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("주민등록번호 앞 6자리", style = Typography.labelLarge)
            TaxInput(
                value = uiState.residentFront,
                onValueChange = { if (it.length <= 6) viewModel.updateResidentFront(it) },
                placeholder = "생년월일 6자리",
                keyboardType = KeyboardType.Number
            )
            Text("주민등록번호 뒷 1자리", style = Typography.labelLarge)
            TaxInput(
                value = uiState.residentBack,
                onValueChange = { if (it.length <= 1) viewModel.updateResidentBack(it) },
                placeholder = "성별 코드",
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.weight(1f))
            TaxButton(
                text = "다음",
                onClick = { navController.navigate(Route.CarrierSelect.path) },
                enabled = uiState.residentFront.length == 6 && uiState.residentBack.length == 1
            )
        }
    }
}
