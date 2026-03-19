package com.ssafy.seveniTax.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

// ── 카테고리 데이터 ──────────────────────────────────────

private data class HomeCategory(
    val label: String,
    @DrawableRes val iconRes: Int
)

private val categories = listOf(
    HomeCategory("카드 관리", R.drawable.ic_07),
    HomeCategory("결제", R.drawable.ic_03),
    HomeCategory("간편장부", R.drawable.ic_16),
    HomeCategory("세금 분류", R.drawable.ic_10),
    HomeCategory("세금 달력", R.drawable.ic_15),
    HomeCategory("세금 추정", R.drawable.ic_04),
    HomeCategory("카드 등록", R.drawable.ic_08),
    HomeCategory("내보내기", R.drawable.ic_11),
    HomeCategory("더보기", R.drawable.ic_22),
)

// ── HomeScreen ───────────────────────────────────────────

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(BrandPurple)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── 보라색 헤더 영역 ──
            HomeHeader()

            // ── 흰색 콘텐츠 영역 (라운드 상단) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Color.White,
                        RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                CardStack()
                Spacer(modifier = Modifier.height(28.dp))
                CategoryGrid(navController)
            }
        }
    }
}

// ── 헤더: 아바타 + 인사말 + 알림 벨 ─────────────────────

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타
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

        // 인사말
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

        // 알림 벨
        Box {
            Icon(
                painter = painterResource(R.drawable.ic_34),
                contentDescription = "알림",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            // 배지
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

// ── 카드 스택 위젯 ───────────────────────────────────────

@Composable
private fun CardStack() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 뒤쪽 카드 3 (보라)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(180.dp)
                .offset(y = 32.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .background(LogoPurple, RoundedCornerShape(10.dp))
        )
        // 뒤쪽 카드 2 (빨강)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(185.dp)
                .offset(y = 18.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .background(Error, RoundedCornerShape(10.dp))
        )
        // 메인 카드
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
            // 카드 번호
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

// ── 카테고리 그리드 (3x3) ────────────────────────────────

@Composable
private fun CategoryGrid(navController: NavController) {
    val rows = categories.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { category ->
                    CategoryItem(
                        category = category,
                        modifier = Modifier.weight(1f),
                        onClick = { /* TODO: 네비게이션 연결 */ }
                    )
                }
                // 빈 칸 채우기 (마지막 행이 3개 미만일 때)
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: HomeCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(15.dp))
            .background(Color.White, RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Icon(
            painter = painterResource(category.iconRes),
            contentDescription = category.label,
            tint = BrandPurple,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}
