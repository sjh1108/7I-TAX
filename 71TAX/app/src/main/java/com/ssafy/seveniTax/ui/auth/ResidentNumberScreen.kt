package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun ResidentNumberScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalDigits = uiState.residentFront.length + uiState.residentBack.length

    LaunchedEffect(totalDigits) {
        if (uiState.residentFront.length == 6 && uiState.residentBack.length == 1) {
            navController.navigate(Route.CarrierSelect.path)
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "주민등록번호를\n입력 해주세요",
                style = Typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SSN field
            Text("주민등록번호", style = Typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    text = uiState.residentFront.padEnd(6, ' '),
                    style = Typography.titleLarge,
                    color = if (uiState.residentFront.isEmpty()) TextSecondary else TextPrimary
                )
                Text(" - ", style = Typography.titleLarge, color = TextSecondary)
                Text(
                    text = uiState.residentBack.ifEmpty { " " },
                    style = Typography.titleLarge,
                    color = if (uiState.residentBack.isEmpty()) TextSecondary else TextPrimary
                )
                Text(
                    text = " " + "\u2022".repeat(6),
                    style = Typography.titleLarge,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Divider)

            Spacer(modifier = Modifier.height(24.dp))

            // Phone field (read-only)
            Text("휴대폰 번호", style = Typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatPhoneDisplay(uiState.phone),
                style = Typography.titleLarge,
                color = TextPrimary
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
                if (uiState.residentFront.length < 6) {
                    viewModel.updateResidentFront(uiState.residentFront + digit)
                } else if (uiState.residentBack.isEmpty()) {
                    viewModel.updateResidentBack(digit.toString())
                }
            },
            onDelete = {
                if (uiState.residentBack.isNotEmpty()) {
                    viewModel.updateResidentBack("")
                } else if (uiState.residentFront.isNotEmpty()) {
                    viewModel.updateResidentFront(uiState.residentFront.dropLast(1))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(KeypadBg)
                .padding(vertical = 8.dp)
        )
    }
}

private fun formatPhoneDisplay(raw: String): String {
    return when {
        raw.length <= 3 -> raw
        raw.length <= 7 -> "${raw.substring(0, 3)}-${raw.substring(3)}"
        else -> "${raw.substring(0, 3)}-${raw.substring(3, 7)}-${raw.substring(7)}"
    }
}
