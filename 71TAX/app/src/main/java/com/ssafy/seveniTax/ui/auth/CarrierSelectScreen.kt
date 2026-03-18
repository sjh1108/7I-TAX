package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.data.model.auth.Carrier
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Typography
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun CarrierSelectScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "본인인증", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("통신사 선택", style = Typography.titleMedium)
            Carrier.entries.forEach { carrier ->
                Button(
                    onClick = { viewModel.updateCarrier(carrier) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(carrier.displayName)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TaxButton(
                text = "다음",
                onClick = { navController.navigate(Route.NameInput.path) },
                enabled = uiState.carrier != null
            )
        }
    }
}
