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
                text = "간편 비밀번호를\n입력 해주세요",
                style = Typography.headlineMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            CodeBoxes(
                code = uiState.pin,
                length = 6,
                masked = true
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        PinKeypad(
            onNumberClick = { viewModel.appendPin(it.toString()) },
            onDelete = { viewModel.deletePin() },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .padding(vertical = 8.dp)
        )
    }
}
