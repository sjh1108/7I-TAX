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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel

// ── 디자인 토큰 (스타일 가이드 + SVG 목업 기준) ──
private val Primary900 = Color(0xFF281C9D)
private val Primary600 = Color(0xFF5655B9)
private val Primary50 = Color(0xFFF2F1F9)
private val Neutral900 = Color(0xFF343434)
private val Neutral500 = Color(0xFF898989)
private val Neutral400 = Color(0xFF989898)
private val CardShadow = Color(0x123629B7) // #3629B7 at 7%
private val BarSafe = Color(0xFF52D5BA)
private val BarWarning = Color(0xFFFFAF2A)
private val BarDanger = Color(0xFFFF4267)

private enum class IconType { MEAL, CAR, UMBRELLA, CHART, HEART }

private data class DeductionItem(
    val name: String, val sub: String, val limit: Long, val used: Long, val icon: IconType
)

@Composable
fun TaxSavingsDetailScreen(navController: NavController, bookEntryViewModel: BookEntryViewModel? = null) {
    val year = java.time.LocalDate.now().year
    val exp = bookEntryViewModel?.getAnnualExpenseByCategory(year)?.toMap() ?: emptyMap()

    val items = listOf(
        DeductionItem("접대비", "연간 한도 1,200만원", 12_000_000, (exp["접대비"] ?: 0L).coerceAtMost(12_000_000), IconType.MEAL),
        DeductionItem("차량유지비", "연간 한도 1,500만원", 15_000_000, (exp["차량유지비"] ?: 0L).coerceAtMost(15_000_000), IconType.CAR),
        DeductionItem("노란우산공제", "소득 4천만 이하 연 500만원", 5_000_000, 0L, IconType.UMBRELLA),
        DeductionItem("연금저축/IRP", "연간 한도 900만원", 9_000_000, 0L, IconType.CHART),
        DeductionItem("기부금", "지정기부금 소득금액 30%", 3_000_000, (exp["기부금"] ?: 0L).coerceAtMost(3_000_000), IconType.HEART),
    )

    val tLimit = items.sumOf { it.limit }
    val tUsed = items.sumOf { it.used }
    val tRemain = tLimit - tUsed
    val pct = if (tLimit > 0) (tUsed * 100 / tLimit).toInt() else 0

    Column(Modifier.fillMaxSize().background(Color.White)) {

        // ── 헤더 ──
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Neutral900)
            }
            Spacer(Modifier.weight(1f))
            Text("절세 상세", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            // ── 요약 ──
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("아직 더 아낄 수 있는 금액", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Neutral500)
                Spacer(Modifier.height(6.dp))
                Text("₩${fmt(tRemain)}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary900)
                Spacer(Modifier.height(6.dp))
                Text("전체 한도의 ${pct}% 사용 중", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary600)

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryChip("총 절세 한도", "${fmt(tLimit)}원", Modifier.weight(1f))
                    SummaryChip("사용한 금액", "${fmt(tUsed)}원", Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))
                ProgressBar(pct / 100f, Primary600)
            }

            // ── 항목별 현황 ──
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("항목별 현황", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
                    Text("탭하여 상세 확인", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Neutral400)
                }
                Spacer(Modifier.height(16.dp))
                items.forEach { DeductionCard(it); Spacer(Modifier.height(12.dp)) }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── 하단 CTA ──
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp, 16.dp)) {
            Button(
                onClick = { }, Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary900)
            ) { Text("더 아낄 수 있는 방법 보기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// ── 요약 칩 ──
@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(Primary50, RoundedCornerShape(12.dp)).padding(16.dp, 14.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Neutral400, letterSpacing = 0.3.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Neutral900)
    }
}

// ── 프로그레스 바 ──
@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFF0F0F0))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(5.dp)).background(color))
    }
}

// ── 공제 카드 (SVG 목업 동일) ──
@Composable
private fun DeductionCard(item: DeductionItem) {
    val pct = if (item.limit > 0) (item.used * 100 / item.limit).toInt() else 0
    val remain = item.limit - item.used
    val barCol = when { pct >= 81 -> BarDanger; pct >= 61 -> BarWarning; else -> BarSafe }

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .background(Color.White, RoundedCornerShape(15.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // 상단
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)), Alignment.Center) {
                    ItemIcon(item.icon)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
                    Spacer(Modifier.height(2.dp))
                    Text(item.sub, fontSize = 12.sp, color = Neutral400)
                }
            }
            Text("$pct%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = barCol)
        }

        // 바
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((pct / 100f).coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(barCol))
        }

        // 사용/남은
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Neutral900)) { append("사용 ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Neutral900)) { append("${fmtMan(item.used)}만원") }
            }, fontSize = 13.sp)
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Neutral500)) { append("남은 ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Primary600)) { append("${fmtMan(remain)}만원") }
            }, fontSize = 13.sp)
        }
    }
}

// ── 아이콘 (SVG 스타일) ──
@Composable
private fun ItemIcon(type: IconType) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width; val h = size.height
        when (type) {
            IconType.MEAL -> {
                val c = Color(0xFFFF6B8A)
                // 포크
                drawLine(c, Offset(w * 0.25f, h * 0.1f), Offset(w * 0.25f, h * 0.9f), 2f, StrokeCap.Round)
                drawLine(c, Offset(w * 0.15f, h * 0.1f), Offset(w * 0.15f, h * 0.35f), 1.5f, StrokeCap.Round)
                drawLine(c, Offset(w * 0.35f, h * 0.1f), Offset(w * 0.35f, h * 0.35f), 1.5f, StrokeCap.Round)
                drawLine(c, Offset(w * 0.15f, h * 0.35f), Offset(w * 0.35f, h * 0.35f), 1.5f, StrokeCap.Round)
                // 나이프
                drawRoundRect(c, Offset(w * 0.6f, h * 0.1f), Size(w * 0.15f, h * 0.35f), CornerRadius(w * 0.07f))
                drawLine(c, Offset(w * 0.675f, h * 0.45f), Offset(w * 0.675f, h * 0.9f), 2f, StrokeCap.Round)
            }
            IconType.CAR -> {
                val c = Color(0xFF4A90D9)
                // 차체
                val body = Path().apply {
                    moveTo(w * 0.1f, h * 0.55f)
                    lineTo(w * 0.2f, h * 0.3f); lineTo(w * 0.8f, h * 0.3f)
                    lineTo(w * 0.9f, h * 0.55f); close()
                }
                drawPath(body, c)
                drawRoundRect(c, Offset(w * 0.05f, h * 0.55f), Size(w * 0.9f, h * 0.2f), CornerRadius(3f))
                // 바퀴
                drawCircle(Color(0xFF343434), w * 0.08f, Offset(w * 0.28f, h * 0.75f))
                drawCircle(Color(0xFF343434), w * 0.08f, Offset(w * 0.72f, h * 0.75f))
                // 창문
                drawLine(Color.White.copy(0.5f), Offset(w * 0.5f, h * 0.33f), Offset(w * 0.5f, h * 0.53f), 1f)
            }
            IconType.UMBRELLA -> {
                val c = Color(0xFFF5A623)
                // 우산 돔
                val dome = Path().apply {
                    moveTo(w * 0.1f, h * 0.45f)
                    cubicTo(w * 0.1f, h * 0.05f, w * 0.9f, h * 0.05f, w * 0.9f, h * 0.45f)
                    close()
                }
                drawPath(dome, c)
                // 손잡이
                drawLine(c, Offset(w * 0.5f, h * 0.45f), Offset(w * 0.5f, h * 0.82f), 2.5f, StrokeCap.Round)
                // J 커브
                val hook = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.5f, h * 0.95f, w * 0.35f, h * 0.95f, w * 0.35f, h * 0.85f)
                }
                drawPath(hook, c, style = Stroke(2.5f, cap = StrokeCap.Round))
            }
            IconType.CHART -> {
                val c = Color(0xFF52D5BA)
                // 막대 3개
                drawRoundRect(c.copy(0.5f), Offset(w * 0.08f, h * 0.55f), Size(w * 0.22f, h * 0.35f), CornerRadius(2f))
                drawRoundRect(c, Offset(w * 0.38f, h * 0.3f), Size(w * 0.22f, h * 0.6f), CornerRadius(2f))
                drawRoundRect(c.copy(0.75f), Offset(w * 0.68f, h * 0.12f), Size(w * 0.22f, h * 0.78f), CornerRadius(2f))
                // 상승선
                drawLine(Primary600, Offset(w * 0.19f, h * 0.5f), Offset(w * 0.79f, h * 0.08f), 1.5f, StrokeCap.Round)
                // 화살표
                drawLine(Primary600, Offset(w * 0.79f, h * 0.08f), Offset(w * 0.68f, h * 0.08f), 1.5f, StrokeCap.Round)
                drawLine(Primary600, Offset(w * 0.79f, h * 0.08f), Offset(w * 0.79f, h * 0.2f), 1.5f, StrokeCap.Round)
            }
            IconType.HEART -> {
                val c = Color(0xFFE85D75)
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.88f)
                    cubicTo(w * 0.1f, h * 0.6f, w * 0.05f, h * 0.2f, w * 0.27f, h * 0.15f)
                    cubicTo(w * 0.4f, h * 0.1f, w * 0.5f, h * 0.25f, w * 0.5f, h * 0.25f)
                    cubicTo(w * 0.5f, h * 0.25f, w * 0.6f, h * 0.1f, w * 0.73f, h * 0.15f)
                    cubicTo(w * 0.95f, h * 0.2f, w * 0.9f, h * 0.6f, w * 0.5f, h * 0.88f)
                    close()
                }
                drawPath(path, c)
            }
        }
    }
}

private fun fmt(v: Long): String = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(v)
private fun fmtMan(v: Long): String = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(v / 10_000)
