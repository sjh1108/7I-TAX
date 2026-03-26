package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

// ═══════════════════════════════════════════════════════
// Step 1: 목적 선택
// ═══════════════════════════════════════════════════════

private data class ExportPurposeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String
)

private val purposes = listOf(
    ExportPurposeItem("vat1", "부가가치세 1기", "2025년 1월 ~ 6월", "📋"),
    ExportPurposeItem("vat2", "부가가치세 2기", "2025년 7월 ~ 12월", "📋"),
    ExportPurposeItem("income", "종합소득세", "2025년 1월 ~ 12월", "📊"),
    ExportPurposeItem("local", "지방소득세", "2025년 1월 ~ 12월", "📊"),
    ExportPurposeItem("custom", "직접 설정", "원하는 기간을 선택합니다", "⚙️")
)

@Composable
fun ExportPurposeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        ExportHeader(title = "데이터 내보내기", onClose = { navController.popBackStack() }, useClose = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "어떤 용도로 내보내시나요?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "용도에 맞는 기간이 자동으로 설정됩니다",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            purposes.forEach { purpose ->
                PurposeCard(purpose) {
                    navController.navigate(Route.ExportDateRange.create(purpose.id))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PurposeCard(item: ExportPurposeItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Step 2: 기간 설정
// ═══════════════════════════════════════════════════════

@Composable
fun ExportDateRangeScreen(navController: NavController, purpose: String) {
    val (defaultStart, defaultEnd) = getDefaultDates(purpose)
    var startDate by remember { mutableStateOf(defaultStart) }
    var endDate by remember { mutableStateOf(defaultEnd) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ExportHeader(title = "기간 설정", onClose = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("내보낼 기간을 선택하세요", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("시작일과 종료일을 설정해주세요", fontSize = 13.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            // 시작일
            Text("시작일", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .clickable { }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(startDate, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 종료일
            Text("종료일", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .clickable { }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(endDate, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }

        // 다음 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    navController.navigate(Route.ExportFormat.create(purpose, startDate, endDate))
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("다음", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

private fun getDefaultDates(purpose: String): Pair<String, String> {
    val year = "2025"
    return when (purpose) {
        "vat1" -> "$year.01.01" to "$year.06.30"
        "vat2" -> "$year.07.01" to "$year.12.31"
        "income", "local" -> "$year.01.01" to "$year.12.31"
        else -> "$year.01.01" to "$year.03.31"
    }
}

// ═══════════════════════════════════════════════════════
// Step 3: 파일 형식 선택
// ═══════════════════════════════════════════════════════

private data class FormatItem(
    val id: String,
    val title: String,
    val extension: String,
    val description: String,
    val icon: String
)

private val formats = listOf(
    FormatItem("excel", "Excel", ".xlsx", "스프레드시트, 항목별 상세 내역 포함", "📊"),
    FormatItem("pdf", "PDF", ".pdf", "인쇄 및 백업용 장부 리포트", "📄"),
    FormatItem("csv", "CSV", ".csv", "회계 프로그램 호환용 데이터", "📋")
)

@Composable
fun ExportFormatScreen(
    navController: NavController,
    purpose: String,
    startDate: String,
    endDate: String
) {
    val purposeLabel = purposes.find { it.id == purpose }?.title ?: "직접 설정"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ExportHeader(title = "파일 형식 선택", onClose = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 컨텍스트 배너
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(purposeLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandPurple)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$startDate ~ $endDate", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("파일 형식을 선택하세요", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("선택하면 바로 내보내기가 시작됩니다", fontSize = 13.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(20.dp))

            formats.forEach { format ->
                FormatCard(format) {
                    // TODO: trigger actual export API
                    navController.popBackStack(Route.BookEntryList.path, false)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FormatCard(item: FormatItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface),
                contentAlignment = Alignment.Center
            ) {
                Text(item.icon, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.extension, fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.description, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 공용 헤더
// ═══════════════════════════════════════════════════════

@Composable
private fun ExportHeader(title: String, onClose: () -> Unit, useClose: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = if (useClose) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "닫기",
                tint = TextPrimary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(48.dp))
    }
}
