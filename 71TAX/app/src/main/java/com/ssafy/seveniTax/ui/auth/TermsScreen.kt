package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Typography
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun TermsScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTerms()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TaxHeader(title = "약관 동의", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.terms) { term ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.agreedTermIds.contains(term.id),
                            onCheckedChange = { viewModel.toggleTerm(term.id) }
                        )
                        Column {
                            Text(term.title, style = Typography.bodyMedium)
                            Text(
                                if (term.required) "[필수]" else "[선택]",
                                style = Typography.bodySmall
                            )
                        }
                    }
                }
            }
            TaxButton(
                text = "동의하고 계속",
                onClick = { navController.navigate(Route.SmsVerify.path) },
                enabled = uiState.allRequiredTermsAgreed
            )
        }
    }
}
