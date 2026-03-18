package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.CardInputForm
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.viewmodel.CardViewModel

@Composable
fun CardInputScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "카드 정보 입력", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            CardInputForm(
                cardNumber = uiState.cardNumber,
                expiry = uiState.expiry,
                cvc = uiState.cvc,
                onCardNumberChange = viewModel::updateCardNumber,
                onExpiryChange = viewModel::updateExpiry,
                onCvcChange = viewModel::updateCvc,
                modifier = Modifier.weight(1f)
            )
            TaxButton(
                text = "다음",
                onClick = { navController.navigate(Route.CardOwnerVerify.path) },
                enabled = uiState.cardNumber.length == 16 && uiState.expiry.length == 4 && uiState.cvc.length == 3
            )
        }
    }
}
