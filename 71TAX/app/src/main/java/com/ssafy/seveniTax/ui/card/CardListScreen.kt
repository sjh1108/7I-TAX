package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Typography
import com.ssafy.seveniTax.viewmodel.CardViewModel

@Composable
fun CardListScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCards()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "카드 관리")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.cards) { card ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("**** **** **** ${card.cardNumberLast4}", style = Typography.bodyMedium)
                        }
                    }
                }
            }
            TaxButton(
                text = "카드 추가",
                onClick = { navController.navigate(Route.CardTypeSelect.path) }
            )
        }
    }
}
