package com.ssafy.seveniTax.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.ssafy.seveniTax.ui.theme.*

private val categories = listOf(
    "전체", "매출", "지급수수료", "소모품비",
    "접대비", "통신비", "임차료",
    "여비교통비", "도서인쇄비", "차량유지비",
    "광고선전비", "세금과공과", "감가상각비"
)

@Composable
fun BookFilterScreen(
    navController: NavController
) {
    var startDate by remember { mutableStateOf("2025.03.01") }
    var endDate by remember { mutableStateOf("2025.03.31") }
    var selectedCategory by remember { mutableStateOf("전체") }
    var merchantQuery by remember { mutableStateOf("") }
    var onlyWithReceipt by remember { mutableStateOf(false) }
    var onlyUnclassified by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.White)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "필터 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "초기화",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPurple,
                modifier = Modifier.clickable {
                    selectedCategory = "전체"
                    merchantQuery = ""
                    onlyWithReceipt = false
                    onlyUnclassified = false
                    startDate = "2025.03.01"
                    endDate = "2025.03.31"
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 기간 범위
            Text(
                text = "기간 범위",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateBox(date = startDate, modifier = Modifier.weight(1f))
                Text("~", color = TextSecondary, fontSize = 14.sp)
                DateBox(date = endDate, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 계정과목
            Text(
                text = "계정과목",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 카테고리 칩 FlowRow
            val chunked = categories.chunked(4)
            chunked.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    row.forEach { category ->
                        CategoryChip(
                            label = category,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 거래처
            Text(
                text = "거래처",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = merchantQuery,
                onValueChange = { merchantQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("거래처 검색...", color = TextSecondary, fontSize = 14.sp)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 토글 옵션
            ToggleRow(
                label = "증빙 있는 거래만",
                checked = onlyWithReceipt,
                onCheckedChange = { onlyWithReceipt = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ToggleRow(
                label = "미분류 거래만",
                checked = onlyUnclassified,
                onCheckedChange = { onlyUnclassified = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 하단 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPurple,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "필터 적용",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DateBox(date: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(Surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) {
                    Modifier.background(BrandPurple, RoundedCornerShape(20.dp))
                } else {
                    Modifier.border(1.dp, Disabled, RoundedCornerShape(20.dp))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else TextPrimary
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandPurple,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Disabled
            )
        )
    }
}
