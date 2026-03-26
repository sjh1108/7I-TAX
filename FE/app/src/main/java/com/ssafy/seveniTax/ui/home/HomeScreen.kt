package com.ssafy.seveniTax.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.ssafy.seveniTax.R
import com.ssafy.seveniTax.util.NotificationHelper
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.BrandPurple
import com.ssafy.seveniTax.ui.theme.Error
import com.ssafy.seveniTax.ui.theme.LogoPurple
import com.ssafy.seveniTax.ui.theme.Surface
import com.ssafy.seveniTax.ui.theme.TextPrimary
import com.ssafy.seveniTax.ui.theme.TextSecondary
import com.ssafy.seveniTax.viewmodel.MainViewModel

private data class HomeActionItem(
    val title: String,
    val iconAsset: String,
    val onClick: (NavController) -> Unit
)

private data class SummaryMetric(
    val label: String,
    val value: String
)

private data class ScheduleItem(
    val title: String,
    val dueText: String,
    val dDayText: String
)

private data class TransactionItem(
    val merchant: String,
    val amount: String,
    val status: String
)

private data class InsightItem(
    val title: String,
    val description: String
)

@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val userName = viewModel.getUserName().ifBlank { "이름" }
    val actions = listOf(
        HomeActionItem("세금 일정", "home/icon_tax_calendar.svg") {
            it.navigate(Route.TaxCalendar.path)
        },
        HomeActionItem("QR 결제", "home/icon_qr_payment.svg") {
            it.navigate(Route.QrPayment.path)
        },
        HomeActionItem("카드 관리", "home/icon_card_manage.svg") { /* 준비 중 */ },
        HomeActionItem("리포트 보기", "home/icon_report.svg") {
            it.navigate(Route.TaxReport.path)
        },
        HomeActionItem("장부 보기", "home/icon_book_entries.svg") {
            it.navigate(Route.BookEntryList.path)
        },
        HomeActionItem("송금", "home/icon_transfer.svg") {
            it.navigate(Route.ServerTest.path)
        }
    )

    val schedules = listOf(
        ScheduleItem("부가세 예정신고", "3월 31일", "D-6"),
        ScheduleItem("원천세 신고", "4월 10일", "D-16")
    )
    val recentTransactions = listOf(
        TransactionItem("스타벅스 강남점", "12,000원", "미분류"),
        TransactionItem("쿠팡", "48,000원", "사업용"),
        TransactionItem("강남주유소", "70,000원", "확인 필요")
    )
    val insights = listOf(
        InsightItem("경비 처리 누락 가능 거래 4건", "정리하면 약 32만원 수준의 절세 여지를 확인할 수 있어요."),
        InsightItem("신고 전 검토 추천", "미분류 경비를 먼저 처리하면 신고 누락 위험을 줄일 수 있어요.")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BrandPurple, BrandPurple, Color.White),
                    startY = 0f,
                    endY = 420f
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    HomeHeader(userName = userName)
                    Spacer(modifier = Modifier.height(8.dp))
                    // 알림 테스트 버튼
                    val context = LocalContext.current
                    androidx.compose.material3.Button(
                        onClick = {
                            NotificationHelper.showClassificationNotification(
                                context = context,
                                transactionId = "test_${System.currentTimeMillis()}",
                                merchantName = "스타벅스 강남점",
                                amount = "5,500원",
                                aiCategory = "복리후생비",
                                confidence = 92
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Error)
                    ) {
                        Text("알림 테스트", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(Color.White)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    UnconfirmedLedgerCard(
                        onClick = { navController.navigate(Route.UnclassifiedList.path) }
                    )
                    ActionGrid(
                        navController = navController,
                        items = actions
                    )
                    ScheduleSection(
                        schedules = schedules,
                        onClick = { navController.navigate(Route.TaxCalendar.path) }
                    )
                    LedgerSection(
                        transactions = recentTransactions,
                        onClick = { navController.navigate(Route.BookEntryList.path) }
                    )
                    InsightSection(
                        insights = insights,
                        onClick = { navController.navigate(Route.TaxReport.path) }
                    )
                    PaymentSection(
                        isPayEnrolled = viewModel.isPayEnrolled(),
                        onCardClick = { /* 카드 관리 준비 중 */ },
                        onQrClick = { navController.navigate(Route.QrPayment.path) }
                    )
                    NoticeBanner()
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(userName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "안녕하세요, ${userName}님",
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Box {
            Icon(
                painter = painterResource(R.drawable.ic_34),
                contentDescription = "알림",
                tint = Color.White,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(24.dp)
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Error)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "3",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryOverviewCard(metrics: List<SummaryMetric>) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "이번 달 핵심 요약",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                metrics.forEach { metric ->
                    SummaryStat(
                        label = metric.label,
                        value = metric.value,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun UnconfirmedLedgerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F3FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "미분류 경비",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "12건",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPurple
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "신고 전에 분류와 확인이 필요한 내역이 있어요",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(LogoPurple)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "지금 확인",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ActionGrid(navController: NavController, items: List<HomeActionItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowItems.forEach { item ->
                    ActionCard(
                        item = item,
                        navController = navController,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    item: HomeActionItem,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { item.onClick(navController) },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HomeSvgIcon(
                assetPath = item.iconAsset,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun ScheduleSection(schedules: List<ScheduleItem>, onClick: () -> Unit) {
    HomeSectionCard(
        title = "다가오는 신고 일정",
        actionText = "전체 보기",
        onClick = onClick
    ) {
        schedules.forEachIndexed { index, item ->
            ScheduleRow(item = item)
            if (index != schedules.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ScheduleRow(item: ScheduleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8F8FB))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.dueText,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = item.dDayText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPurple
        )
    }
}

@Composable
private fun LedgerSection(transactions: List<TransactionItem>, onClick: () -> Unit) {
    HomeSectionCard(
        title = "이번 달 장부 현황",
        actionText = "장부 보기",
        onClick = onClick
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LedgerMetricCard("수입", "4,820,000원", Modifier.weight(1f))
            LedgerMetricCard("지출", "1,430,000원", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LedgerMetricCard("고정자산", "320,000원", Modifier.weight(1f))
            LedgerMetricCard("미분류", "5건", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "최근 거래",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        transactions.forEachIndexed { index, transaction ->
            TransactionRow(transaction)
            if (index != transactions.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun LedgerMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8F8FB))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun TransactionRow(item: TransactionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8F8FB))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.merchant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.status,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = item.amount,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun InsightSection(insights: List<InsightItem>, onClick: () -> Unit) {
    HomeSectionCard(
        title = "이번 달 절세 포인트",
        actionText = "리포트 보기",
        onClick = onClick
    ) {
        insights.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFF8ED))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            if (index != insights.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PaymentSection(
    isPayEnrolled: Boolean,
    onCardClick: () -> Unit,
    onQrClick: () -> Unit
) {
    HomeSectionCard(
        title = "결제 수단 상태",
        actionText = null,
        onClick = null
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentStatusCard(
                title = "기본 카드",
                value = "삼성 Business",
                modifier = Modifier.weight(1f),
                onClick = onCardClick
            )
            PaymentStatusCard(
                title = "Tax Pay",
                value = if (isPayEnrolled) "가입 완료" else "미가입",
                modifier = Modifier.weight(1f),
                onClick = onQrClick
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        PaymentStatusWideCard(
            title = "최근 결제",
            value = "오늘 14:32 · 18,000원",
            onClick = onQrClick
        )
    }
}

@Composable
private fun PaymentStatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8F8FB))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun PaymentStatusWideCard(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8F8FB))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun NoticeBanner() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2430)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = "지금 해야 할 것",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "사업용으로 확정되지 않은 거래가 있어요. 신고 전에 검토를 마치면 누락 위험을 줄일 수 있어요.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.82f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun HomeSectionCard(
    title: String,
    actionText: String?,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (actionText != null && onClick != null) {
                    Text(
                        text = actionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandPurple,
                        modifier = Modifier.clickable { onClick() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun HomeSvgIcon(assetPath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data("file:///android_asset/$assetPath")
        .decoderFactory(SvgDecoder.Factory())
        .build()

    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
