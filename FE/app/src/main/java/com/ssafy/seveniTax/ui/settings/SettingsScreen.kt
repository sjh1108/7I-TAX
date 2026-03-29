package com.ssafy.seveniTax.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.viewmodel.MainViewModel

private val Primary900 = Color(0xFF281C9D)
private val Primary600 = Color(0xFF5655B9)
private val Primary50 = Color(0xFFF2F1F9)
private val Neutral900 = Color(0xFF343434)
private val Neutral500 = Color(0xFF898989)
private val Neutral400 = Color(0xFF989898)
private val CardShadow = Color(0x123629B7)

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val userName = mainViewModel.getUserName().ifBlank { "사용자" }
    val userPhone = mainViewModel.getUserPhone()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃", fontWeight = FontWeight.Bold) },
            text = { Text("정말 로그아웃 하시겠습니까?", fontSize = 14.sp, color = Neutral500) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    mainViewModel.logout()
                    navController.navigate(Route.Splash.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("로그아웃", color = Color(0xFFFF4267), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = Neutral500)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── 헤더 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary900)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("설정", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 프로필 카드 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = CardShadow, spotColor = CardShadow)
                    .background(Color.White, RoundedCornerShape(15.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Primary50),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userName.take(1), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary900)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Neutral900)
                        if (userPhone.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(userPhone, fontSize = 13.sp, color = Neutral500)
                        }
                    }
                }
            }

            // ── 세금·장부 ──
            SettingsSection("세금·장부") {
                SettingsItem("알림 설정", "세금 일정 알림 관리") {
                    navController.navigate(Route.NotificationSettings.path)
                }
                SettingsDivider()
                SettingsItem("세금 캘린더", "신고·납부 일정 확인") {
                    navController.navigate(Route.TaxCalendar.path)
                }
                SettingsDivider()
                SettingsItem("공제 한도", "절세 항목별 현황") {
                    navController.navigate(Route.TaxSavingsDetail.path)
                }
            }

            // ── 결제·카드 ──
            SettingsSection("결제·카드") {
                SettingsItem("카드 관리", "등록된 카드 조회·변경") {
                    navController.navigate(Route.CardList.path)
                }
                SettingsDivider()
                SettingsItem("QR 결제", "바코드·QR 결제") {
                    navController.navigate(Route.QrPayment.path)
                }
            }

            // ── 앱 정보 ──
            SettingsSection("앱 정보") {
                SettingsInfoRow("버전", "1.0.0")
                SettingsDivider()
                SettingsInfoRow("빌드", "2026.03")
            }

            // ── 로그아웃 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .clickable { showLogoutDialog = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("로그아웃", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF4267))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .background(Color.White, RoundedCornerShape(15.dp))
            .padding(20.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary600, letterSpacing = 0.3.sp)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Neutral900)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Neutral500)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Neutral400,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Neutral900)
        Text(value, fontSize = 14.sp, color = Neutral500)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = Primary50, modifier = Modifier.padding(vertical = 2.dp))
}
