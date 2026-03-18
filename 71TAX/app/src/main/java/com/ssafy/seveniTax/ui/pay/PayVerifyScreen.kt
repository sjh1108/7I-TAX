package com.ssafy.seveniTax.ui.pay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.seveniTax.ui.components.*
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.PayViewModel

@Composable
fun PayVerifyScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    viewModel: PayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.payVerified) {
        if (uiState.payVerified) onNext()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "본인 인증", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Text("본인 인증이 필요합니다", style = Typography.titleMedium)
                Text("PASS 앱 또는 신분증으로 인증해주세요", style = Typography.bodyMedium, color = TextSecondary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TaxButton(text = "PASS 앱으로 인증", onClick = { viewModel.verifyIdentity() })
                TaxButton(text = "신분증으로 인증", onClick = { viewModel.verifyIdentity() }, variant = ButtonVariant.Secondary)
            }
        }
        LoadingOverlay(visible = uiState.loading)
    }
}

@Preview(showBackground = true)
@Composable
private fun PayVerifyScreenPreview() {
    PayVerifyScreen(onBack = {}, onNext = {})
}
