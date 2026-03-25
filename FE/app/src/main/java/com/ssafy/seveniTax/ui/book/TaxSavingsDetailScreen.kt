package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.*

private data class SavingItem(
    val name: String,
    val subtitle: String,
    val limit: Long,
    val used: Long,
    val icon: String
)

private val savingItems = listOf(
    SavingItem("노란우산공제", "연간 한도 500만원", 5_000_000, 3_000_000, "☂️"),
    SavingItem("사업용카드 공제", "연간 한도 240만원", 2_400_000, 2_160_000, "💳"),
    SavingItem("교육훈련비 공제", "연간 한도 150만원", 1_500_000, 320_000, "📚")
)

@Composable
fun TaxSavingsDetailScreen(navController: NavController) {
    val totalLimit = savingItems.sumOf { it.limit }
    val totalUsed = savingItems.sumOf { it.used }
    val totalRemaining = totalLimit - totalUsed
    val overallPercent = if (totalLimit > 0) (totalUsed * 100 / totalLimit).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text("절세 상세", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // 상단 요약
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("아직 더 아낄 수 있는 금액", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "₩${formatSavingsAmount(totalRemaining)}",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandPurple
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "전체 한도의 ${overallPercent}% 사용 중",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LogoPurple
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoChip("총 절세 한도", formatSavingsAmount(totalLimit) + "원", Modifier.weight(1f))
                    InfoChip("사용한 금액", formatSavingsAmount(totalUsed) + "원", Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // 프로그레스 바
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF0F0F0))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(overallPercent / 100f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(LogoPurple)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 항목별 현황
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("항목별 현황", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("탭하여 상세 확인", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(16.dp))

                savingItems.forEach { item ->
                    SavingCard(item)
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // 하단 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("더 아낄 수 있는 방법 보기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier) {
    Box(
        modifier
            .background(Surface, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun SavingCard(item: SavingItem) {
    val percent = if (item.limit > 0) (item.used * 100 / item.limit).toInt() else 0
    val remaining = item.limit - item.used
    val barColor = when {
        percent >= 81 -> Color(0xFFFF4267)
        percent >= 61 -> Color(0xFFFFAF2A)
        else -> Color(0xFF52D5BA)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.icon, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(item.subtitle, fontSize = 12.sp, color = TextSecondary)
                }
                Text("$percent%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = barColor)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent / 100f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("사용 ${formatSavingsAmount(item.used)}원", fontSize = 12.sp, color = TextSecondary)
                Text(
                    "남은 ${formatSavingsAmount(remaining)}원",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LogoPurple
                )
            }
        }
    }
}

private fun formatSavingsAmount(amount: Long): String {
    return if (amount >= 10_000) {
        val man = amount / 10_000
        "${java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(man)}만"
    } else {
        java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(amount)
    }
}
