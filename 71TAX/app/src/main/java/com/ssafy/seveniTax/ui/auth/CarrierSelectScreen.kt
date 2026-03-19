package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.data.model.auth.Carrier
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarrierSelectScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(true) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                if (uiState.carrier == null) {
                    viewModel.updateResidentFront("")
                    viewModel.updateResidentBack("")
                    navController.popBackStack()
                }
            },
            sheetState = sheetState,
            containerColor = Background
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = "통신사를 알려주세요",
                    style = Typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Carrier.entries.forEach { carrier ->
                    Text(
                        text = carrier.displayName,
                        style = Typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateCarrier(carrier)
                                showSheet = false
                                navController.navigate(Route.NameInput.path)
                            }
                            .padding(vertical = 14.dp)
                    )
                    if (carrier != Carrier.entries.last()) {
                        HorizontalDivider(color = Divider)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Background: form summary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        IconButton(
            onClick = {
                viewModel.updateResidentFront("")
                viewModel.updateResidentBack("")
                navController.popBackStack()
            },
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
            text = "통신사를 선택 해주세요",
            style = Typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Carrier field
        FormField(
            label = "통신사",
            value = uiState.carrier?.displayName ?: "",
            onClick = { showSheet = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SSN field (read-only)
        FormFieldReadOnly("주민등록번호", buildSsnDisplay(uiState.residentFront, uiState.residentBack))
        Spacer(modifier = Modifier.height(24.dp))

        // Phone field (read-only)
        FormFieldReadOnly("휴대폰 번호", formatPhoneForm(uiState.phone))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "입력하신 정보는 5일 동안 안전히 보관해드릴게요",
            style = Typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
internal fun FormField(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(label, style = Typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = value.ifEmpty { " " },
                style = Typography.titleLarge,
                color = if (value.isEmpty()) TextSecondary else TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (onClick != null) {
                Text("▼", style = Typography.bodyMedium, color = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Divider)
    }
}

@Composable
internal fun FormFieldReadOnly(label: String, value: String) {
    Column {
        Text(label, style = Typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = Typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Divider)
    }
}

internal fun buildSsnDisplay(front: String, back: String): String {
    return "$front-$back${"●".repeat(6)}"
}

internal fun formatPhoneForm(raw: String): String {
    return when {
        raw.length <= 3 -> raw
        raw.length <= 7 -> "${raw.substring(0, 3)}-${raw.substring(3)}"
        else -> "${raw.substring(0, 3)}-${raw.substring(3, 7)}-${raw.substring(7)}"
    }
}
