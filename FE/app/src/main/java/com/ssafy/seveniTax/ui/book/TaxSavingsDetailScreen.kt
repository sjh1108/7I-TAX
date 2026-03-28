package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel

// ── 색상 (목업 기준) ──
private val Primary900 = Color(0xFF281C9D)
private val Primary600 = Color(0xFF5655B9)
private val Primary50 = Color(0xFFF2F1F9)
private val Neutral900 = Color(0xFF343434)
private val Neutral500 = Color(0xFF898989)
private val Neutral400 = Color(0xFF989898)
private val BarSafe = Color(0xFF52D5BA)
private val BarWarning = Color(0xFFFFAF2A)
private val BarDanger = Color(0xFFFF4267)
private val IconBg = Color(0xFFF5F5F5)

private enum class IconType { MEAL, CAR, UMBRELLA, CHART, HEART }

private data class DeductionItem(
    val name: String,
    val subtitle: String,
    val limit: Long,
    val used: Long,
    val iconType: IconType
)

@Composable
fun TaxSavingsDetailScreen(navController: NavController, bookEntryViewModel: BookEntryViewModel? = null) {
    val year = java.time.LocalDate.now().year
    val expenses = bookEntryViewModel?.getAnnualExpenseByCategory(year) ?: emptyList()
    val expenseMap = expenses.toMap()

    val items = listOf(
        DeductionItem("접대비", "연간 한도 1,200만원", 12_000_000,
            (expenseMap["접대비"] ?: 0L).coerceAtMost(12_000_000), IconType.MEAL),
        DeductionItem("차량유지비", "연간 한도 1,500만원", 15_000_000,
            (expenseMap["차량유지비"] ?: 0L).coerceAtMost(15_000_000), IconType.CAR),
        DeductionItem("노란우산공제", "소득 4천만 이하 연 500만원", 5_000_000,
            0L, IconType.UMBRELLA),
        DeductionItem("연금저축/IRP", "연간 한도 900만원", 9_000_000,
            0L, IconType.CHART),
        DeductionItem("기부금", "지정기부금 소득금액 30%", 3_000_000,
            (expenseMap["기부금"] ?: 0L).coerceAtMost(3_000_000), IconType.HEART)
    )

    val totalLimit = items.sumOf { it.limit }
    val totalUsed = items.sumOf { it.used }
    val totalRemaining = totalLimit - totalUsed
    val overallPercent = if (totalLimit > 0) (totalUsed * 100 / totalLimit).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Top Nav ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = Neutral900)
            }
            Spacer(Modifier.weight(1f))
            Text("절세 상세", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Summary Header ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("아직 더 아낄 수 있는 금액", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Neutral500)
                Spacer(Modifier.height(6.dp))
                Text(
                    "₩${fmt(totalRemaining)}",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary900
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "전체 한도의 ${overallPercent}% 사용 중",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary600
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryBox("총 절세 한도", "${fmt(totalLimit)}원", Modifier.weight(1f))
                    SummaryBox("사용한 금액", "${fmt(totalUsed)}원", Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // 전체 프로그레스 바
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFF0F0F0))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((overallPercent / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(5.dp))
                            .background(Primary600)
                    )
                }
            }

            // ── 항목별 현황 ──
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("항목별 현황", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
                    Text("탭하여 상세 확인", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Neutral400)
                }

                Spacer(Modifier.height(16.dp))

                items.forEach { item ->
                    DeductionCard(item)
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── Bottom CTA ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary900)
            ) {
                Text("더 아낄 수 있는 방법 보기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ── Summary Box ──
@Composable
private fun SummaryBox(label: String, value: String, modifier: Modifier) {
    Box(
        modifier
            .background(Primary50, RoundedCornerShape(12.dp))
            .padding(16.dp, 14.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Neutral400, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Neutral900)
        }
    }
}

// ── Deduction Card (목업 동일 레이아웃) ──
@Composable
private fun DeductionCard(item: DeductionItem) {
    val percent = if (item.limit > 0) (item.used * 100 / item.limit).toInt() else 0
    val remaining = item.limit - item.used
    val barColor = when {
        percent >= 81 -> BarDanger
        percent >= 61 -> BarWarning
        else -> BarSafe
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = Color(0x12362EB7),
                spotColor = Color(0x12362EB7)
            )
            .background(Color.White, RoundedCornerShape(15.dp))
            .padding(20.dp, 24.dp)
    ) {
        // 상단: 아이콘 + 이름 + 퍼센트
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 아이콘 (Canvas로 SVG 스타일)
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IconBg),
                    contentAlignment = Alignment.Center
                ) {
                    DeductionIcon(item.iconType)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900, lineHeight = 21.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(item.subtitle, fontSize = 12.sp, color = Neutral400)
                }
            }
            Text("$percent%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = barColor)
        }

        // 프로그레스 바
        Spacer(Modifier.height(14.dp))
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
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }

        // 사용 / 남은 금액
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Neutral900)) { append("사용 ") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Neutral900)) { append("${fmtMan(item.used)}만원") }
                },
                fontSize = 13.sp
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Neutral500)) { append("남은 ") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Primary600)) { append("${fmtMan(remaining)}만원") }
                },
                fontSize = 13.sp
            )
        }
    }
}

// ── 아이콘 (Canvas) ──
@Composable
private fun DeductionIcon(type: IconType) {
    Canvas(modifier = Modifier.size(20.dp)) {
        when (type) {
            IconType.MEAL -> drawMealIcon(this)
            IconType.CAR -> drawCarIcon(this)
            IconType.UMBRELLA -> drawUmbrellaIcon(this)
            IconType.CHART -> drawChartIcon(this)
            IconType.HEART -> drawHeartIcon(this)
        }
    }
}

private fun drawMealIcon(scope: DrawScope) = with(scope) {
    val c = Color(0xFFFF6B8A)
    // 접시
    drawCircle(c, radius = size.minDimension * 0.4f, center = Offset(size.width / 2, size.height * 0.55f))
    drawCircle(Color.White, radius = size.minDimension * 0.25f, center = Offset(size.width / 2, size.height * 0.55f))
    // 포크 라인
    drawLine(c, Offset(size.width * 0.3f, size.height * 0.1f), Offset(size.width * 0.3f, size.height * 0.4f), strokeWidth = 2f)
    // 나이프 라인
    drawLine(c, Offset(size.width * 0.7f, size.height * 0.1f), Offset(size.width * 0.7f, size.height * 0.4f), strokeWidth = 2f)
}

private fun drawCarIcon(scope: DrawScope) = with(scope) {
    val c = Color(0xFF4A90D9)
    // 차체
    drawRoundRect(c, Offset(size.width * 0.1f, size.height * 0.4f), Size(size.width * 0.8f, size.height * 0.35f), CornerRadius(4f))
    // 지붕
    drawRoundRect(c, Offset(size.width * 0.2f, size.height * 0.2f), Size(size.width * 0.6f, size.height * 0.25f), CornerRadius(6f))
    // 바퀴
    drawCircle(Color(0xFF343434), radius = size.minDimension * 0.1f, center = Offset(size.width * 0.3f, size.height * 0.75f))
    drawCircle(Color(0xFF343434), radius = size.minDimension * 0.1f, center = Offset(size.width * 0.7f, size.height * 0.75f))
}

private fun drawUmbrellaIcon(scope: DrawScope) = with(scope) {
    val c = Color(0xFFF5A623)
    // 우산 캡
    drawArc(c, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
        size = Size(size.width * 0.8f, size.height * 0.6f))
    // 손잡이
    drawLine(c, Offset(size.width / 2, size.height * 0.45f), Offset(size.width / 2, size.height * 0.85f), strokeWidth = 2.5f)
    drawArc(c, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(size.width * 0.38f, size.height * 0.75f),
        size = Size(size.width * 0.24f, size.height * 0.2f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
}

private fun drawChartIcon(scope: DrawScope) = with(scope) {
    val c = Color(0xFF52D5BA)
    // 막대 3개
    drawRoundRect(c.copy(alpha = 0.5f), Offset(size.width * 0.1f, size.height * 0.5f), Size(size.width * 0.2f, size.height * 0.4f), CornerRadius(2f))
    drawRoundRect(c, Offset(size.width * 0.4f, size.height * 0.3f), Size(size.width * 0.2f, size.height * 0.6f), CornerRadius(2f))
    drawRoundRect(c.copy(alpha = 0.7f), Offset(size.width * 0.7f, size.height * 0.15f), Size(size.width * 0.2f, size.height * 0.75f), CornerRadius(2f))
    // 상승 화살표
    drawLine(Color(0xFF343434), Offset(size.width * 0.15f, size.height * 0.45f), Offset(size.width * 0.8f, size.height * 0.1f), strokeWidth = 1.5f)
}

private fun drawHeartIcon(scope: DrawScope) = with(scope) {
    val c = Color(0xFF7C3AED)
    val path = Path().apply {
        val w = size.width; val h = size.height
        moveTo(w * 0.5f, h * 0.85f)
        cubicTo(w * 0.15f, h * 0.55f, w * 0.05f, h * 0.25f, w * 0.25f, h * 0.15f)
        cubicTo(w * 0.4f, h * 0.08f, w * 0.5f, h * 0.25f, w * 0.5f, h * 0.25f)
        cubicTo(w * 0.5f, h * 0.25f, w * 0.6f, h * 0.08f, w * 0.75f, h * 0.15f)
        cubicTo(w * 0.95f, h * 0.25f, w * 0.85f, h * 0.55f, w * 0.5f, h * 0.85f)
        close()
    }
    drawPath(path, c, style = Fill)
}

private fun fmt(amount: Long): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(amount)

private fun fmtMan(amount: Long): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(amount / 10_000)
