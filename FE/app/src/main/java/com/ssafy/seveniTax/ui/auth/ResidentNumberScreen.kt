package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.PinKeypad
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentNumberScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalDigits = uiState.residentFront.length + uiState.residentBack.length
    val sheetState = rememberModalBottomSheetState()
    var showCarrierSheet by remember { mutableStateOf(false) }

    val carriers = listOf("SKT", "KT", "LG U+", "SKT 알뜰폰", "KT 알뜰폰", "LG U+ 알뜰폰")

    LaunchedEffect(totalDigits) {
        if (uiState.residentFront.length == 6 && uiState.residentBack.length == 1) {
            navController.navigate(Route.NameInput.path)
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
                onClick = {
                    viewModel.updatePhone("")
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

            // 통신사 선택
            Text("통신사", style = Typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCarrierSheet = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.carrier.ifEmpty { "통신사" },
                    style = Typography.titleLarge,
                    color = if (uiState.carrier.isEmpty()) TextSecondary else TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "선택",
                    tint = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Divider)

            Spacer(modifier = Modifier.height(24.dp))

            // 주민등록번호
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

            FormFieldReadOnly("휴대폰 번호", formatPhoneForm(uiState.phone))

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "입력하신 정보는 5일 동안 안전히 보관해드릴게요",
                style = Typography.bodySmall,
                color = TextSecondary
            )
        }
        PinKeypad(
            onNumberClick = { digit ->
                if (uiState.carrier.isEmpty()) {
                    showCarrierSheet = true
                    return@PinKeypad
                }
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

    // 통신사 선택 바텀시트
    if (showCarrierSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCarrierSheet = false },
            sheetState = sheetState,
            containerColor = Background,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "통신사를 알려주세요",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                carriers.forEach { carrier ->
                    Text(
                        text = carrier,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateCarrier(carrier)
                                showCarrierSheet = false
                            }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
}
