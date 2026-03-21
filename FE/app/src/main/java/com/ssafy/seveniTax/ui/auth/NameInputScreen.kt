package com.ssafy.seveniTax.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.AuthViewModel

data class TermDetail(
    val consentType: String,
    val title: String,
    val required: Boolean,
    val subItems: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameInputScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var showTermsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val termDetails = remember {
        listOf(
            TermDetail(
                "SERVICE", "기택스 회원 약관 및 동의사항", true,
                listOf("서비스 이용약관", "통합 금융정보 및 전자문서 서비스 약관", "개인(신용)정보 수집 및 이용 동의(회원가입)")
            ),
            TermDetail(
                "PRIVACY", "본인 확인 서비스 약관 및 동의사항", true,
                listOf("통신사 이용약관", "본인확인서비스 이용약관", "개인정보 수집, 이용 및 위탁 동의")
            ),
            TermDetail(
                "FINANCIAL", "기택스 세무 서비스 제공 동의", false
            )
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        viewModel.loadTerms()
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
                    viewModel.updateCarrier("")
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
                text = "이름을 알려주세요",
                style = Typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 이름
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

            // 통신사 (읽기전용)
            Text("통신사", style = Typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.carrier,
                    style = Typography.titleLarge,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Divider)

            Spacer(modifier = Modifier.height(24.dp))

            // 주민등록번호 (읽기전용)
            FormFieldReadOnly("주민등록번호", buildSsnDisplay(uiState.residentFront, uiState.residentBack))
            Spacer(modifier = Modifier.height(24.dp))

            // 휴대폰 번호 (읽기전용)
            FormFieldReadOnly("휴대폰 번호", formatPhoneForm(uiState.phone))
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.errorMessage.isNotEmpty()) {
                Text(
                    text = uiState.errorMessage,
                    style = Typography.bodySmall,
                    color = Error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "입력하신 정보는 5일 동안 안전히 보관해드릴게요",
                style = Typography.bodySmall,
                color = TextSecondary
            )
        }

        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            TaxButton(
                text = "다음",
                onClick = { showTermsSheet = true },
                enabled = uiState.name.isNotBlank()
            )
        }
    }

    // 약관동의 바텀시트
    if (showTermsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTermsSheet = false },
            sheetState = sheetState,
            containerColor = Background,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "서비스 이용에\n꼭 필요한 동의만 추렸어요",
                    style = Typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))

                // 전체 동의 버튼
                val allIds = termDetails.map { it.consentType }.toSet()
                val isAllAgreed = uiState.agreedTermIds.containsAll(allIds)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAllTerms() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (isAllAgreed) BrandPurple else Disabled,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "전체 동의",
                        style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
                HorizontalDivider(color = Divider)
                Spacer(modifier = Modifier.height(4.dp))

                termDetails.forEach { term ->
                    val isAgreed = uiState.agreedTermIds.contains(term.consentType)
                    var expanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleTerm(term.consentType) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isAgreed) BrandPurple else Disabled,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                        if (term.subItems.isNotEmpty()) {
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

                    // 확장 시 하위 항목 표시
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier.padding(start = 36.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            term.subItems.forEach { subItem ->
                                Text(
                                    text = subItem,
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TaxButton(
                    text = if (uiState.isLoading) "처리 중..." else "동의하고 본인 인증하기",
                    onClick = {
                        viewModel.verifyIdentity(
                            onSuccess = {
                                showTermsSheet = false
                                navController.navigate(Route.SmsVerification.path)
                            }
                        )
                    },
                    enabled = uiState.allRequiredTermsAgreed && !uiState.isLoading
                )

                TextButton(
                    onClick = { showTermsSheet = false },
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
}
