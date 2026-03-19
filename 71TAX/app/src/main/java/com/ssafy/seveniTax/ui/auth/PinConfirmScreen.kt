package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.CodeBoxes
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
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
                onSuccess = {
                    navController.navigate(Route.AuthSuccess.path)
                },
                onMismatch = {
                    // ViewModel이 에러 메시지 + pinConfirm 초기화 처리함
                },
                onResetRequired = {
                    navController.popBackStack(Route.PinSetup.path, inclusive = true)
                    navController.navigate(Route.PinSetup.path)
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "한번 더\n입력 해주세요",
                style = Typography.headlineMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            CodeBoxes(
                code = uiState.pinConfirm,
                length = 6,
                masked = true
            )

            if (uiState.errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage,
                    style = Typography.bodySmall,
                    color = Error
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        PinKeypad(
            onNumberClick = { viewModel.appendPinConfirm(it.toString()) },
            onDelete = { viewModel.deletePinConfirm() },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .padding(vertical = 8.dp)
        )
    }
}
