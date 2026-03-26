package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel
import com.ssafy.seveniTax.viewmodel.EntryFilter
import java.text.NumberFormat
import java.util.Locale

private enum class ReportTab(val label: String) {
    MONTHLY("월간"), ANNUAL("연간")
}

@Composable
fun TaxReportScreen(navController: NavController, bookEntryViewModel: BookEntryViewModel? = null) {
    var selectedTab by remember { mutableStateOf(ReportTab.MONTHLY) }
    var selectedYear by remember { mutableIntStateOf(java.time.LocalDate.now().year) }
    var selectedMonth by remember { mutableIntStateOf(java.time.LocalDate.now().monthValue) }

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
            Text("세무 리포트", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        // 탭
        Row(modifier = Modifier.fillMaxWidth()) {
            ReportTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tab.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandPurple else TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) BrandPurple else Color.Transparent)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            when (selectedTab) {
                ReportTab.MONTHLY -> MonthlyReport(
                    year = selectedYear,
                    month = selectedMonth,
                    onPrev = {
                        if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- }
                        else selectedMonth--
                    },
                    onNext = {
                        if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ }
                        else selectedMonth++
                    },
                    onIncomeClick = {
                        bookEntryViewModel?.apply {
                            selectYear(selectedYear)
                            selectMonth(selectedMonth)
                            selectFilter(EntryFilter.INCOME)
                        }
                        navController.navigate(Route.BookEntryList.path)
                    },
                    onExpenseClick = {
                        bookEntryViewModel?.apply {
                            selectYear(selectedYear)
                            selectMonth(selectedMonth)
                            selectFilter(EntryFilter.EXPENSE)
                        }
                        navController.navigate(Route.BookEntryList.path)
                    }
                )
                ReportTab.ANNUAL -> AnnualReport(
                    year = selectedYear,
                    onPrev = { selectedYear-- },
                    onNext = { selectedYear++ }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── 월간 리포트 ─────────────────────────────────────

@Composable
private fun MonthlyReport(
    year: Int, month: Int,
    onPrev: () -> Unit, onNext: () -> Unit,
    onIncomeClick: () -> Unit = {}, onExpenseClick: () -> Unit = {}
) {
    Spacer(Modifier.height(16.dp))
    DateNavigator(text = "${year}년 ${month}월", onPrev = onPrev, onNext = onNext)
    Spacer(Modifier.height(20.dp))

    // 순이익 카드
    ReportSummaryCard("월간 순이익", 9_448_100, 10_880_000, -1_431_900,
        onIncomeClick = onIncomeClick, onExpenseClick = onExpenseClick)

    Spacer(Modifier.height(24.dp))

    // 월별 추이
    SectionTitle("월별 추이")
    Spacer(Modifier.height(12.dp))
    TrendLineChart()

    Spacer(Modifier.height(24.dp))

    // 계정과목별 비용
    SectionTitle("계정과목별 비용")
    Spacer(Modifier.height(12.dp))
    DonutChart()

    Spacer(Modifier.height(24.dp))

    // 거래처별 수입
    SectionTitle("거래처별 수입")
    Spacer(Modifier.height(12.dp))
    IncomeBarChart()

    Spacer(Modifier.height(24.dp))

    // 세금 추정
    SectionTitle("월간 세금 추정")
    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("부가가치세", listOf(
        "매출세액" to "988,000원",
        "매입세액" to "-130,173원",
        "예상 납부액" to "857,827원"
    ))

    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("종합소득세", listOf(
        "월간 수입" to "10,880,000원",
        "월간 경비" to "-1,431,900원",
        "월간 예상 소득세" to "1,417,215원"
    ))

    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("지방소득세", listOf(
        "금액" to "141,722원",
        "비고" to "종합소득세의 10%"
    ))

    Spacer(Modifier.height(12.dp))

    TotalTaxBox("월간 총 예상 세금", "2,416,764원")
}

// ─── 연간 리포트 ────────────────────────────────────────

@Composable
private fun AnnualReport(year: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Spacer(Modifier.height(16.dp))
    DateNavigator(text = "${year}년", onPrev = onPrev, onNext = onNext)
    Spacer(Modifier.height(20.dp))

    // 연간 순이익
    ReportSummaryCard("연간 순이익", 30_000_000, 50_000_000, -20_000_000)

    Spacer(Modifier.height(24.dp))

    // 전년 대비
    SectionTitle("전년 대비")
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ComparisonBox("수입", "+23%", "4,070만 → 5,000만", Color(0xFFEDF8F6), Color(0xFF3DBDA2), Modifier.weight(1f))
        ComparisonBox("비용", "-5%", "2,100만 → 2,000만", Color(0xFFFFF0F3), Color(0xFFE8475A), Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    ComparisonBox("순이익", "+52%", "1,970만 → 3,000만", Surface, BrandPurple, Modifier.fillMaxWidth())

    Spacer(Modifier.height(24.dp))

    // 연간 세금 예상액
    SectionTitle("연간 세금 예상액")
    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("부가가치세", listOf(
        "납부액" to "3,000,000원",
        "1기 (1~6월)" to "1,500,000원",
        "2기 (7~12월)" to "—"
    ))

    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("종합소득세", listOf(
        "총 수입" to "50,000,000원",
        "필요경비" to "-20,000,000원",
        "과세표준" to "27,000,000원",
        "적용 세율" to "15%",
        "예상 납부액" to "3,960,000원"
    ))

    Spacer(Modifier.height(12.dp))

    TaxEstimateItem("지방소득세", listOf(
        "금액" to "396,000원",
        "비고" to "종합소득세의 10%"
    ))

    Spacer(Modifier.height(12.dp))

    TotalTaxBox("연간 총 예상 세금", "7,356,000원")

    Spacer(Modifier.height(24.dp))

    // 세율 구간
    SectionTitle("내 세율 구간")
    Spacer(Modifier.height(12.dp))
    TaxBracketCard()

    Spacer(Modifier.height(24.dp))

    // 올해 공제 요약
    SectionTitle("올해 공제 요약")
    Spacer(Modifier.height(12.dp))
    DeductionSummaryCard()
}

// ─── 날짜 네비게이터 ─────────────────────────────────────

@Composable
private fun DateNavigator(text: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "이전", tint = TextPrimary)
        }
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPurple
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "다음", tint = TextPrimary)
        }
    }
}

// ─── 공통 컴포넌트 ──────────────────────────────────────

@Composable
private fun ReportSummaryCard(
    label: String, net: Long, income: Long, expense: Long,
    onIncomeClick: () -> Unit = {}, onExpenseClick: () -> Unit = {}
) {
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = Color(0x123629B7))
            .background(Color.White, RoundedCornerShape(15.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("+${fmt.format(net)}원", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEDF8F6)).clickable { onIncomeClick() }.padding(12.dp)) {
                    Column { Text("총 수입", fontSize = 11.sp, color = Color(0xFF7ABFB3)); Text("+${fmt.format(income)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF0F3)).clickable { onExpenseClick() }.padding(12.dp)) {
                    Column { Text("총 비용", fontSize = 11.sp, color = Color(0xFFFF9DAE)); Text("${fmt.format(expense)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
}

@Composable
private fun TaxEstimateItem(title: String, items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            items.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 13.sp, color = TextSecondary)
                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun TotalTaxBox(label: String, amount: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Text(amount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
        }
    }
}

@Composable
private fun ComparisonBox(label: String, percent: String, detail: String, bg: Color, textColor: Color, modifier: Modifier) {
    Box(modifier.background(bg, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Column {
            Text(label, fontSize = 11.sp, color = textColor)
            Spacer(Modifier.height(4.dp))
            Text(percent, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(Modifier.height(2.dp))
            Text(detail, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun TaxBracketCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("현재 과세표준", fontSize = 13.sp, color = TextSecondary)
                Text("27,000,000원", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.background(Surface, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("15% 구간", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandPurple)
            }
            Spacer(Modifier.height(12.dp))
            // 프로그레스 바
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(0.54f).clip(RoundedCornerShape(4.dp)).background(BrandPurple))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1,400만원 (6%)", fontSize = 10.sp, color = TextSecondary)
                Text("5,000만원 (24%)", fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().background(Color(0xFFFFF8EE), RoundedCornerShape(8.dp)).padding(12.dp)) {
                Column {
                    Text("다음 구간까지", fontSize = 12.sp, color = Color(0xFFE0A44A))
                    Text("23,000,000원 더 벌면 24% 구간", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun DeductionSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            listOf(
                "경비 인정 금액" to "20,000,000원",
                "부가세 매입공제" to "2,000,000원",
                "경비 절세 효과" to "3,000,000원"
            ).forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 13.sp, color = TextSecondary)
                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Surface)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("총 절세 금액", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("5,000,000원", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
            }
        }
    }
}

// ─── 월별 추이 꺾은선 그래프 ──────────────────────────────

@Composable
private fun TrendLineChart() {
    val incomeData = listOf(7.2f, 8.5f, 6.8f, 10.2f, 9.0f, 10.88f)
    val expenseData = listOf(1.2f, 1.1f, 1.5f, 1.0f, 1.3f, 1.43f)
    val months = listOf("10월", "11월", "12월", "1월", "2월", "3월")
    val maxVal = 12f
    val purple = Color(0xFF5655B9)
    val pink = Color(0xFFFF9DAE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val w = size.width
                val h = size.height - 24.dp.toPx()
                val stepX = w / (incomeData.size - 1)
                val gridLines = 4

                // 그리드
                for (i in 0..gridLines) {
                    val y = h * i / gridLines
                    drawLine(Color(0xFFF0F0F0), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                }

                // 수입 라인
                val incomePath = Path()
                incomeData.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = h - (v / maxVal * h)
                    if (i == 0) incomePath.moveTo(x, y) else incomePath.lineTo(x, y)
                }
                drawPath(incomePath, purple, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                incomeData.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = h - (v / maxVal * h)
                    drawCircle(if (i == incomeData.lastIndex) Color.White else purple, 4.dp.toPx(), Offset(x, y))
                    if (i == incomeData.lastIndex) drawCircle(purple, 4.dp.toPx(), Offset(x, y), style = Stroke(2.5.dp.toPx()))
                }

                // 비용 라인
                val expensePath = Path()
                expenseData.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = h - (v / maxVal * h)
                    if (i == 0) expensePath.moveTo(x, y) else expensePath.lineTo(x, y)
                }
                drawPath(expensePath, pink, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                expenseData.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = h - (v / maxVal * h)
                    drawCircle(if (i == expenseData.lastIndex) Color.White else pink, 4.dp.toPx(), Offset(x, y))
                    if (i == expenseData.lastIndex) drawCircle(pink, 4.dp.toPx(), Offset(x, y), style = Stroke(2.5.dp.toPx()))
                }

                // 월 라벨
                val paint = android.graphics.Paint().apply {
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                months.forEachIndexed { i, label ->
                    val x = stepX * i
                    paint.color = if (i == months.lastIndex) android.graphics.Color.parseColor("#281C9D") else android.graphics.Color.parseColor("#989898")
                    paint.isFakeBoldText = i == months.lastIndex
                    drawContext.canvas.nativeCanvas.drawText(label, x, size.height, paint)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 범례
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(8.dp).background(purple, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(4.dp))
                Text("수입", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(8.dp).background(pink, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(4.dp))
                Text("비용", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

// ─── 계정과목별 비용 도넛 차트 ────────────────────────────

@Composable
private fun DonutChart() {
    val segments = listOf(
        Triple("임차료", 52f, Color(0xFFE8475A)),
        Triple("지급수수료", 20f, Color(0xFF5655B9)),
        Triple("소모품비", 13f, Color(0xFFF5A623)),
        Triple("교육훈련비", 10f, Color(0xFF3DBDA2)),
        Triple("통신비", 8f, Color(0xFF4A90D9)),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeW = 20.dp.toPx()
                    val radius = (size.minDimension - strokeW) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2 - strokeW / 2 + strokeW / 2,
                        (size.height - radius * 2) / 2 - strokeW / 2 + strokeW / 2
                    )
                    val arcSize = Size(radius * 2, radius * 2)
                    var startAngle = -90f
                    segments.forEach { (_, pct, color) ->
                        val sweep = pct / 100f * 360f
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(strokeW / 2, strokeW / 2),
                            size = Size(size.width - strokeW, size.height - strokeW),
                            style = Stroke(strokeW, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("총 비용", fontSize = 11.sp, color = TextSecondary)
                    Text("739,500원", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 범례 - 2줄
            val chunked = segments.chunked(3)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (label, pct, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(4.dp))
                            Text("$label ${pct.toInt()}%", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ─── 거래처별 수입 막대 그래프 ────────────────────────────

@Composable
private fun IncomeBarChart() {
    val bars = listOf(
        Triple("(주)스마트랩", 550f, 51),
        Triple("프리워크", 338f, 31),
        Triple("기타", 200f, 18)
    )
    val maxVal = 600f
    val colors = listOf(Color(0xFF281C9D), Color(0xFF5655B9), Color(0xFFA8A3D7))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val w = size.width
                val h = size.height
                val barCount = bars.size
                val barWidth = w / (barCount * 2 + 1)
                val gridLines = 4

                // 그리드
                for (i in 0..gridLines) {
                    val y = h * i / gridLines
                    drawLine(Color(0xFFF0F0F0), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                }

                // 바
                bars.forEachIndexed { i, (_, value, _) ->
                    val barH = (value / maxVal) * h
                    val x = barWidth * (i * 2 + 1)
                    drawRoundRect(
                        color = colors[i],
                        topLeft = Offset(x, h - barH),
                        size = Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }

                // 값 라벨
                val paint = android.graphics.Paint().apply {
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                bars.forEachIndexed { i, (_, value, _) ->
                    val barH = (value / maxVal) * h
                    val x = barWidth * (i * 2 + 1) + barWidth / 2
                    paint.color = android.graphics.Color.parseColor(
                        when (i) { 0 -> "#281C9D"; 1 -> "#5655B9"; else -> "#A8A3D7" }
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${value.toInt()}만",
                        x, h - barH - 6.dp.toPx(),
                        paint
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 라벨
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bars.forEachIndexed { i, (name, _, pct) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${pct}%", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
