package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.CodeBoxes
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun SmsVerifyScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.requestSmsVerification()
    }

    LaunchedEffect(uiState.smsCode.length) {
        if (uiState.smsCode.length == 6) {
            viewModel.verifySms(
                onSuccess = { navController.navigate(Route.PinSetup.path) }
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
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = "문자로 받은\n인증번호 6자리를 입력해주세요",
                style = Typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "남은 시간 03:00",
                style = Typography.bodyMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // OTP boxes
            CodeBoxes(
                code = uiState.smsCode,
                length = 6,
                masked = false,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (uiState.errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage,
                    style = Typography.bodySmall,
                    color = Error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { viewModel.requestSmsVerification() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "문자가 안오나요?",
                    style = Typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                    color = TextPrimary
                )
            }
        }

        PinKeypad(
            onNumberClick = { digit ->
                if (uiState.smsCode.length < 6) {
                    viewModel.updateSmsCode(uiState.smsCode + digit)
                }
            },
            onDelete = {
                if (uiState.smsCode.isNotEmpty()) {
                    viewModel.updateSmsCode(uiState.smsCode.dropLast(1))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .padding(vertical = 8.dp)
        )
    }
}
