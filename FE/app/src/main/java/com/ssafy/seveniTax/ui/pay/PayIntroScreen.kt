package com.ssafy.seveniTax.ui.pay

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.R
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

private data class PayFeature(
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int
)

private val features = listOf(
    PayFeature("카드 등록", "일반 카드 및 사업자 카드를\n등록하고 관리하세요", R.drawable.ic_07),
    PayFeature("간편 결제", "QR 또는 온라인으로 빠르게\n결제하세요", R.drawable.ic_03),
    PayFeature("결제 내역", "기간별, 카드별로 내역을\n조회할 수 있어요", R.drawable.ic_16),
    PayFeature("세무 자동화", "간편 장부를 기반으로\n세무 자동화 기능까지 한번에", R.drawable.ic_04),
)

@Composable
fun PayIntroScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── 상단 네비게이션 바 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary
                )
            }
            Text(
                text = "페이 가입하기",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // ── 카드 목록 (스크롤) ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            features.forEach { feature ->
                FeatureCard(feature)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── 하단 가입 버튼 ──
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            TaxButton(
                text = "가입 시작하기",
                onClick = { navController.navigate(Route.PayBusinessInfo.path) }
            )
        }
    }
}

@Composable
private fun FeatureCard(feature: PayFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 텍스트 영역
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 아이콘
        Icon(
            painter = painterResource(feature.iconRes),
            contentDescription = feature.title,
            tint = BrandPurple,
            modifier = Modifier.size(56.dp)
        )
    }
}
