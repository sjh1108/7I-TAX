package com.ssafy.seveniTax.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.R
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    var showNotificationDialog by remember { mutableStateOf(false) }

    if (showNotificationDialog) {
        NotificationDialog(onDismiss = { showNotificationDialog = false })
    }

    Box(modifier = modifier.fillMaxSize().background(BrandPurple)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader(onNotificationClick = { showNotificationDialog = true })

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 600.dp)
                    .background(
                        Color.White,
                        RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                CardStack()
                Spacer(modifier = Modifier.height(28.dp))

                // 바로가기 버튼 1줄
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShortcutButton(
                        icon = Icons.Outlined.Assessment,
                        label = "레포트",
                        onClick = { navController.navigate(Route.TaxReport.path) }
                    )
                    ShortcutButton(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "캘린더",
                        onClick = { navController.navigate(Route.TaxCalendar.path) }
                    )
                    ShortcutButton(
                        icon = Icons.Outlined.CreditCard,
                        label = "카드등록",
                        onClick = { navController.navigate(Route.CardTypeSelect.path) }
                    )
                    ShortcutButton(
                        icon = Icons.Outlined.Settings,
                        label = "카드관리",
                        onClick = { navController.navigate(Route.CardList.path) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 바로가기 버튼 2줄
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShortcutButton(
                        icon = Icons.Outlined.MenuBook,
                        label = "간편장부",
                        onClick = { navController.navigate(Route.BookEntryList.path) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onNotificationClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(LogoPurple),
            contentAlignment = Alignment.Center
        ) {
            Text("U", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "안녕하세요,",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "홍길동님",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier.clickable(onClick = onNotificationClick)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_34),
                contentDescription = "알림",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Error)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                contentAlignment = Alignment.Center
            ) {
                Text("3", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CardStack() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(180.dp)
                .offset(y = 32.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .background(LogoPurple, RoundedCornerShape(10.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(185.dp)
                .offset(y = 18.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .background(Error, RoundedCornerShape(10.dp))
        )
        BankCard(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BankCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(204.dp)
            .shadow(12.dp, RoundedCornerShape(10.dp))
            .background(
                color = CardBlue,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "홍길동",
                fontSize = 24.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "7iTAX 사업자 카드",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("4756", fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("••••", fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("••••", fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("9018", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₩3,469,520",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ShortcutButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(BrandPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = BrandPurple,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun NotificationDialog(onDismiss: () -> Unit) {
    val notifications = listOf(
        "부가가치세 신고 마감 D-7",
        "3월 카드 매출 자동 분류 완료",
        "간편장부 미분류 거래 2건"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "알림",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notifications.forEach { message ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BrandPurple)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = BrandPurple)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
