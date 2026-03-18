package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.layout.*
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
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun PhoneInputScreen(
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
            TaxInput(
                value = uiState.phone,
                onValueChange = viewModel::updatePhone,
                placeholder = "휴대폰 번호 입력 (- 없이)",
                keyboardType = KeyboardType.Phone
            )
            Spacer(modifier = Modifier.weight(1f))
            TaxButton(
                text = "다음",
                onClick = { navController.navigate(Route.ResidentNumber.path) },
                enabled = uiState.phone.length >= 10
            )
        }
    }
}
