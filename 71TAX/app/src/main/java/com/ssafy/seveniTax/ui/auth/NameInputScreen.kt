package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun NameInputScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                text = "이름을 알려주세요",
                style = Typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name field (editable)
            Text("이름", style = Typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                textStyle = Typography.titleLarge,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = if (uiState.name.isNotEmpty()) BrandPurple else Divider)

            Spacer(modifier = Modifier.height(24.dp))

            // Carrier (read-only)
            FormFieldReadOnly("통신사", uiState.carrier?.displayName ?: "")
            Spacer(modifier = Modifier.height(24.dp))

            // SSN (read-only)
            FormFieldReadOnly("주민등록번호", buildSsnDisplay(uiState.residentFront, uiState.residentBack))
            Spacer(modifier = Modifier.height(24.dp))

            // Phone (read-only)
            FormFieldReadOnly("휴대폰 번호", formatPhoneForm(uiState.phone))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "입력하신 정보는 5일 동안 안전히 보관해드릴게요",
                style = Typography.bodySmall,
                color = TextSecondary
            )
        }

        // Next button at bottom
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            TaxButton(
                text = "다음",
                onClick = { navController.navigate(Route.Terms.path) },
                enabled = uiState.name.isNotBlank()
            )
        }
    }
}
