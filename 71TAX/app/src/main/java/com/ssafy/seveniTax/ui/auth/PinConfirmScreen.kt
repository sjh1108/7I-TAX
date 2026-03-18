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
fun PinConfirmScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.pinConfirm.length) {
        if (uiState.pinConfirm.length == 6) {
            viewModel.confirmPin(
                onSuccess = { navController.navigate(Route.AuthSuccess.path) },
                onMismatch = { viewModel.resetPinConfirm() }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "PIN 확인", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("PIN을 한 번 더 입력해주세요.", style = Typography.bodyMedium)
            PinIndicator(filled = uiState.pinConfirm.length)
            if (uiState.errorMessage.isNotEmpty()) {
                Text(uiState.errorMessage, style = Typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            PinKeypad(
                onNumberClick = { viewModel.appendPinConfirm(it.toString()) },
                onDelete = { viewModel.deletePinConfirm() }
            )
        }
    }
}
