package com.ssafy.seveniTax.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.R
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(BrandPurple)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader()

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

                // 카테고리 영역 (임시)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("임시", style = Typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader() {
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

        Box {
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
