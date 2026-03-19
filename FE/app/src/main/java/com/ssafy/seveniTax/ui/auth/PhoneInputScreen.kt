package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun PhoneInputScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.phone.length) {
        if (uiState.phone.length == 11) {
            navController.navigate(Route.ResidentNumber.path)
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
                text = "휴대폰 번호를\n입력 해주세요",
                style = Typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text("휴대폰 번호", style = Typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatPhone(uiState.phone).ifEmpty { " " },
                style = Typography.titleLarge,
                color = if (uiState.phone.isEmpty()) TextSecondary else TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Divider)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "입력하신 정보는 5일 동안 안전히 보관해드릴게요",
                style = Typography.bodySmall,
                color = TextSecondary
            )
        }
        PinKeypad(
            onNumberClick = { digit ->
                if (uiState.phone.length < 11) {
                    viewModel.updatePhone(uiState.phone + digit)
                }
            },
            onDelete = {
                if (uiState.phone.isNotEmpty()) {
                    viewModel.updatePhone(uiState.phone.dropLast(1))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .padding(vertical = 8.dp)
        )
    }
}

private fun formatPhone(raw: String): String {
    return when {
        raw.length <= 3 -> raw
        raw.length <= 7 -> "${raw.substring(0, 3)}-${raw.substring(3)}"
        else -> "${raw.substring(0, 3)}-${raw.substring(3, 7)}-${raw.substring(7)}"
    }
}
