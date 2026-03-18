package com.ssafy.seveniTax.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { viewModel.loadTerms() }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Background
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "서비스 이용에\n꼭 필요한 동의만 추렸어요",
                    style = Typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Terms list
                uiState.terms.forEach { term ->
                    val isAgreed = uiState.agreedTermIds.contains(term.id)
                    var expanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleTerm(term.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isAgreed) BrandPurple else Disabled,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (term.required) "필수" else "선택",
                            style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = term.title,
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (term.required) {
                            IconButton(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "펼치기",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier.padding(start = 36.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("세부 약관 내용 보기", style = Typography.bodySmall, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CTA button
                TaxButton(
                    text = "동의하고 본인 인증하기",
                    onClick = {
                        viewModel.submitTerms()
                        navController.navigate(Route.SmsVerify.path)
                    },
                    enabled = uiState.allRequiredTermsAgreed
                )

                // Decline link
                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("동의 안 함", style = Typography.bodyMedium, color = Disabled)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Background: form review
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
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
            text = "${uiState.name}님이 맞는지\n확인할게요",
            style = Typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        FormFieldReadOnly("이름", uiState.name)
        Spacer(modifier = Modifier.height(24.dp))
        FormFieldReadOnly("통신사", uiState.carrier?.displayName ?: "")
        Spacer(modifier = Modifier.height(24.dp))
        FormFieldReadOnly("주민등록번호", buildSsnDisplay(uiState.residentFront, uiState.residentBack))
        Spacer(modifier = Modifier.height(24.dp))
        FormFieldReadOnly("휴대폰 번호", formatPhoneForm(uiState.phone))
    }
}
