package com.ssafy.seveniTax.ui.classification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun MemoAddScreen(
    navController: NavController,
    merchantName: String = "스타벅스 강남점",
    amount: String = "5,500원",
    category: String = "복리후생비",
    onSave: (String) -> Unit = {},
    onSkip: () -> Unit = {}
) {
    var memoText by remember { mutableStateOf("") }
    var isTipExpanded by remember { mutableStateOf(true) }
    val maxLength = 200

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 헤더 (건너뛰기 포함)
        MemoHeader(
            onBack = { navController.popBackStack() },
            onSkip = onSkip
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 페이지 타이틀
            Text(
                text = "메모를 추가하시겠어요?",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF343434)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "세금 신고 시 증빙자료로 활용됩니다",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF898989)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 결제 정보 태그
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$merchantName · $amount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF898989)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 메모 입력 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                TextField(
                    value = memoText,
                    onValueChange = {
                        if (it.length <= maxLength) memoText = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    placeholder = {
                        Text(
                            text = "증빙에 필요한 내용을 자유롭게 입력하세요",
                            fontSize = 13.sp,
                            color = Color(0xFFCACACA)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF343434)
                    )
                )

                HorizontalDivider(color = Surface, thickness = 1.dp)

                Spacer(modifier = Modifier.height(8.dp))

                // 글자 수 카운터
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = LogoPurple)) {
                                append("${memoText.length}")
                            }
                            append("/$maxLength")
                        },
                        fontSize = 12.sp,
                        color = Color(0xFFCACACA)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 사진 첨부 버튼 (점선 테두리)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { /* TODO: 사진 첨부 */ },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📷",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "사진 첨부하기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF898989)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 팁 섹션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTipExpanded = !isTipExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 이런 메모가 도움돼요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF898989)
                )
                Text(
                    text = if (isTipExpanded) "▲" else "▼",
                    fontSize = 12.sp,
                    color = Color(0xFF898989)
                )
            }

            if (isTipExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TipRow(category = "접대비", example = "거래처 OOO 대표 식사 (3명)")
                    TipRow(category = "여비교통비", example = "서울→부산 출장 택시비")
                    TipRow(category = "복리후생비", example = "팀 회식 (마케팅팀 5명)")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 하단 저장 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = { onSave(memoText) },
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
                    text = "저장",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MemoHeader(
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Primary)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Background
            )
        }

        Text(
            text = "메모 추가",
            style = Typography.titleLarge,
            color = Background,
            modifier = Modifier.align(Alignment.Center)
        )

        Text(
            text = "건너뛰기",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFD0D0E0),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(onClick = onSkip)
                .padding(end = 16.dp)
        )
    }
}

@Composable
private fun TipRow(
    category: String,
    example: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandPurple,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = example,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF898989)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MemoAddScreenPreview() {
    MemoAddScreen(
        navController = rememberNavController()
    )
}
