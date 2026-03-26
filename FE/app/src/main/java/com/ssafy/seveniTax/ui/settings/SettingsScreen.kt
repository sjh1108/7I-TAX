package com.ssafy.seveniTax.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.BrandPurple
import com.ssafy.seveniTax.ui.theme.Surface
import com.ssafy.seveniTax.ui.theme.TextPrimary
import com.ssafy.seveniTax.ui.theme.TextSecondary

private data class SettingsNavItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun SettingsScreen(navController: NavController, modifier: Modifier = Modifier) {
    var pushEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var marketingEnabled by remember { mutableStateOf(false) }

    val accountItems = listOf(
        SettingsNavItem("계정 정보", "사업자명, 연락처, 사업장 정보", Icons.Default.PersonOutline),
        SettingsNavItem("본인 인증 관리", "인증 상태와 등록 정보 확인", Icons.Default.Badge),
        SettingsNavItem("결제 수단 관리", "카드 및 QR 결제 설정", Icons.Default.CreditCard)
    )

    val supportItems = listOf(
        SettingsNavItem("보안 및 권한", "앱 접근 권한과 로그인 보안", Icons.Default.Security),
        SettingsNavItem("고객 지원", "문의, 도움말, 공지사항", Icons.Default.SupportAgent)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            SettingsSection(title = "계정") {
                accountItems.forEachIndexed { index, item ->
                    SettingsNavRow(item = item)
                    if (index != accountItems.lastIndex) {
                        HorizontalDivider(color = Color(0xFFEAEAF0))
                    }
                }
            }
        }

        item {
            SettingsSection(title = "앱 설정") {
                SettingsToggleRow(
                    title = "푸시 알림",
                    subtitle = "세금 일정과 거래 알림을 받습니다.",
                    icon = Icons.Default.NotificationsNone,
                    checked = pushEnabled,
                    onCheckedChange = { pushEnabled = it }
                )
                HorizontalDivider(color = Color(0xFFEAEAF0))
                SettingsToggleRow(
                    title = "생체 인증",
                    subtitle = "지문 또는 얼굴 인증으로 빠르게 로그인합니다.",
                    icon = Icons.Default.Lock,
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it }
                )
                HorizontalDivider(color = Color(0xFFEAEAF0))
                SettingsToggleRow(
                    title = "혜택 및 이벤트 알림",
                    subtitle = "업데이트와 프로모션 소식을 받습니다.",
                    icon = Icons.Default.NotificationsNone,
                    checked = marketingEnabled,
                    onCheckedChange = { marketingEnabled = it }
                )
            }
        }

        item {
            SettingsSection(title = "지원") {
                supportItems.forEachIndexed { index, item ->
                    SettingsNavRow(item = item)
                    if (index != supportItems.lastIndex) {
                        HorizontalDivider(color = Color(0xFFEAEAF0))
                    }
                }
            }
        }

        item {
            SettingsSection(title = "정보") {
                SettingsNavRow(
                    item = SettingsNavItem("앱 버전", "1.0.0", Icons.Default.Info)
                )
            }
        }

        item { Spacer(modifier = Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandPurple
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsNavRow(item: SettingsNavItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = BrandPurple
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BrandPurple
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandPurple
            )
        )
    }
}
