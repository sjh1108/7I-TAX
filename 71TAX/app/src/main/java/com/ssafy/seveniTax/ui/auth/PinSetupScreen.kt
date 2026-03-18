package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.PinIndicator
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Typography
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun PinSetupScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.pin.length) {
        if (uiState.pin.length == 6) {
            navController.navigate(Route.PinConfirm.path)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "PIN 설정", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("사용할 PIN 6자리를 입력해주세요.", style = Typography.bodyMedium)
            PinIndicator(filled = uiState.pin.length)
            Spacer(modifier = Modifier.weight(1f))
            PinKeypad(
                onNumberClick = { viewModel.appendPin(it.toString()) },
                onDelete = { viewModel.deletePin() }
            )
        }
    }
}
