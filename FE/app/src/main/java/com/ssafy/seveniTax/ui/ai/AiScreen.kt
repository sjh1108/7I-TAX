package com.ssafy.seveniTax.ui.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.*
import kotlinx.coroutines.launch

// ── 색상 ──
private val Primary900 = Color(0xFF281C9D)
private val Primary700 = Color(0xFF3A2FB0)
private val Primary600 = Color(0xFF5655B9)
private val Primary300 = Color(0xFFA8A3D7)
private val Primary100 = Color(0xFFD4D1EC)
private val Primary50 = Color(0xFFF2F1F9)
private val Neutral900 = Color(0xFF343434)
private val Neutral700 = Color(0xFF555555)
private val Neutral500 = Color(0xFF898989)
private val Neutral400 = Color(0xFF989898)
private val Neutral300 = Color(0xFFCACACA)
private val Neutral200 = Color(0xFFE8E8E8)
private val Neutral50 = Color(0xFFF7F7F7)
private val ErrorColor = Color(0xFFFF4267)
private val SuccessColor = Color(0xFF52D5BA)
private val AccentColor = Color(0xFFFB6B18)
private val WarningColor = Color(0xFFFFAF2A)

private val PurpleGradient = Brush.linearGradient(listOf(Primary900, Primary700))
private val SendGradient = Brush.linearGradient(listOf(Primary900, Primary600))

// ── 데이터 모델 ──
sealed class ChatItem {
    data class DateDivider(val date: String) : ChatItem()
    data class UserMessage(val text: String, val time: String) : ChatItem()
    data class AiMessage(
        val text: String,
        val time: String,
        val intentBadge: String? = null,
        val card: CardContent? = null,
        val isError: Boolean = false,
        val showConsultBanner: Boolean = false
    ) : ChatItem()
    object Typing : ChatItem()
}

sealed class CardContent {
    data class Classification(
        val category: String,
        val merchant: String,
        val amount: String,
        val confidence: Int,
        val legalBasis: String,
        val tip: String
    ) : CardContent()

    data class TaxRate(
        val title: String,
        val subtitle: String,
        val rows: List<TaxRateRow>,
        val currentIndex: Int
    ) : CardContent()
}

data class TaxRateRow(val bracket: String, val rate: String, val deduction: String)

@Composable
fun AiScreen(navController: NavController, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // 목업 데이터
    val chatItems = remember { buildMockData() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        ChatHeader(onBack = { navController.popBackStack() })

        // Chat Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(chatItems) { item ->
                when (item) {
                    is ChatItem.DateDivider -> DateDividerRow(item.date)
                    is ChatItem.UserMessage -> UserBubble(item)
                    is ChatItem.AiMessage -> AiBubble(item)
                    is ChatItem.Typing -> TypingIndicator()
                }
            }
        }

        // Input Area
        ChatInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    inputText = ""
                    scope.launch { listState.animateScrollToItem(chatItems.size - 1) }
                }
            }
        )
    }
}

// ── Header ──
@Composable
private fun ChatHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary50)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Neutral900)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title + online dot
        Text("AI 챗봇", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(SuccessColor)
        )

        Spacer(modifier = Modifier.weight(1f))

        // New chat button
        IconBtn(text = "+")
        Spacer(modifier = Modifier.width(4.dp))
        // History button
        IconBtn(text = "💬")
    }
}

@Composable
private fun IconBtn(text: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, color = Neutral900)
    }
}

// ── Date Divider ──
@Composable
private fun DateDividerRow(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Neutral200)
        Text(
            text = date,
            modifier = Modifier.padding(horizontal = 14.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Neutral400
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Neutral200)
    }
}

// ── User Bubble ──
@Composable
private fun UserBubble(msg: ChatItem.UserMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                .background(PurpleGradient)
                .padding(12.dp, 12.dp)
        ) {
            Text(msg.text, fontSize = 13.sp, lineHeight = 21.sp, color = Color.White)
        }
        Text(msg.time, fontSize = 10.sp, color = Neutral300, modifier = Modifier.padding(top = 4.dp, end = 2.dp))
    }
}

// ── AI Bubble ──
@Composable
private fun AiBubble(msg: ChatItem.AiMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Primary900, Primary600))),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Sender + intent badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("7iTAX AI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Neutral500)
                if (msg.intentBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Primary50)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(msg.intentBadge, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Primary600)
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Error or normal bubble
            if (msg.isError) {
                ErrorBubbleContent(msg.text)
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                        .background(Neutral50)
                        .border(1.dp, Neutral200, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                        .padding(12.dp, 12.dp)
                ) {
                    Text(msg.text, fontSize = 13.sp, lineHeight = 21.sp, color = Neutral900)
                }
            }

            // Intent Card
            msg.card?.let { card ->
                Spacer(modifier = Modifier.height(8.dp))
                when (card) {
                    is CardContent.Classification -> ClassificationCard(card)
                    is CardContent.TaxRate -> TaxRateCard(card)
                }
            }

            // Consult banner
            if (msg.showConsultBanner) {
                Spacer(modifier = Modifier.height(8.dp))
                ConsultBanner()
            }

            // Feedback + time
            FeedbackRow(msg.time)
        }
    }
}

// ── Classification Card ──
@Composable
private fun ClassificationCard(data: CardContent.Classification) {
    var showLegal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Primary100, RoundedCornerShape(16.dp))
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x0F3629B7))
    ) {
        // Card Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PurpleGradient)
                .padding(12.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2EFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text("📋", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("AI 추천 세목", fontSize = 10.sp, color = Color(0xA6FFFFFF))
                Text(data.category, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Card Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(14.dp)
        ) {
            CardRow("가맹점", data.merchant)
            CardRow("금액", data.amount)

            // Confidence bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("신뢰도", fontSize = 12.sp, color = Neutral500)
                Spacer(modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                LinearProgressIndicator(
                    progress = { data.confidence / 100f },
                    modifier = Modifier
                        .weight(2f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (data.confidence >= 80) Primary900 else AccentColor,
                    trackColor = Primary50,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${data.confidence}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (data.confidence >= 80) Primary900 else AccentColor
                )
            }

            // Legal toggle
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { showLegal = !showLegal },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛡️", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (showLegal) "법률 근거 접기" else "법률 근거 보기",
                    fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Primary600
                )
                Text(if (showLegal) " ▲" else " ▼", fontSize = 9.sp, color = Primary600)
            }

            if (showLegal) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary50)
                        .padding(10.dp, 10.dp)
                ) {
                    Text(data.legalBasis, fontSize = 11.sp, lineHeight = 18.sp, color = Neutral700)
                }
            }

            // Tip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary50)
                    .padding(10.dp, 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Primary600),
                    contentAlignment = Alignment.Center
                ) {
                    Text("i", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(data.tip, fontSize = 11.sp, lineHeight = 17.sp, color = Neutral700, modifier = Modifier.weight(1f))
            }
        }

        // Card Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary900)
            ) {
                Text("확인", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Primary100),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary600)
            ) {
                Text("세목 변경", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Tax Rate Table Card ──
@Composable
private fun TaxRateCard(data: CardContent.TaxRate) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Primary100, RoundedCornerShape(16.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PurpleGradient)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2EFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text("📊", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(data.subtitle, fontSize = 10.sp, color = Color(0xA6FFFFFF))
                Text(data.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary50)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text("과세표준", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary600, modifier = Modifier.weight(1f))
            Text("세율", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary600, modifier = Modifier.weight(0.6f))
            Text("누진공제", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary600, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
        }

        // Table rows
        data.rows.forEachIndexed { index, row ->
            val isCurrent = index == data.currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isCurrent) Color(0x0F5655B9) else Color.White)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    row.bracket, fontSize = 11.sp, color = Neutral900,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    row.rate, fontSize = 11.sp, color = Neutral900,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(0.6f)
                )
                Text(
                    row.deduction, fontSize = 11.sp, color = Neutral900,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(0.7f),
                    textAlign = TextAlign.End
                )
            }
            if (index < data.rows.lastIndex) {
                HorizontalDivider(color = Neutral50)
            }
        }
    }
}

// ── Error Bubble ──
@Composable
private fun ErrorBubbleContent(text: String) {
    Row(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
            .background(Color(0xFFFFF5F7))
            .border(1.dp, Color(0x26FF4267), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("⚠️", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text, fontSize = 13.sp, lineHeight = 21.sp, color = ErrorColor)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.clickable { },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔄", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("다시 질문하기", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary600)
            }
        }
    }
}

// ── Typing Indicator ──
@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Primary900, Primary600))),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(Neutral50)
                .border(1.dp, Neutral200, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                TypingDot(delay = index * 200)
            }
        }
    }
}

@Composable
private fun TypingDot(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                -4f at 300
                0f at 600
                0f at 1400
            },
            initialStartOffset = StartOffset(delay)
        ),
        label = "dot"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0.3f at 0
                1f at 300
                0.3f at 600
                0.3f at 1400
            },
            initialStartOffset = StartOffset(delay)
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(Primary300.copy(alpha = alpha))
    )
}

// ── Consult Banner ──
@Composable
private fun ConsultBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Neutral50)
            .border(1.dp, Neutral200, RoundedCornerShape(12.dp))
            .clickable { }
            .padding(10.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentColor),
            contentAlignment = Alignment.Center
        ) {
            Text("📞", fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("정확한 세액이 궁금하다면?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
            Text("전문 세무사 상담 연결하기", fontSize = 10.sp, color = Neutral500)
        }
        Text("›", fontSize = 18.sp, color = Neutral300)
    }
}

// ── Feedback Row ──
@Composable
private fun FeedbackRow(time: String) {
    Row(
        modifier = Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var liked by remember { mutableStateOf(false) }
        var disliked by remember { mutableStateOf(false) }

        FeedbackButton(text = "👍", active = liked) {
            liked = !liked; if (liked) disliked = false
        }
        Spacer(modifier = Modifier.width(4.dp))
        FeedbackButton(text = "👎", active = disliked) {
            disliked = !disliked; if (disliked) liked = false
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(time, fontSize = 10.sp, color = Neutral300)
    }
}

@Composable
private fun FeedbackButton(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (active) Primary600 else Neutral200,
                RoundedCornerShape(8.dp)
            )
            .background(if (active) Primary50 else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp)
    }
}

// ── Card Row ──
@Composable
private fun CardRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Neutral500)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Neutral900)
    }
}

// ── Chat Input Bar ──
@Composable
private fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Neutral50)
            .border(1.5.dp, Neutral200, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, top = 5.dp, bottom = 5.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("세무 관련 질문을 입력하세요", fontSize = 13.sp, color = Neutral400)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Primary900
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                color = Neutral900
            ),
            singleLine = true
        )

        // Send button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SendGradient)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Text("➤", fontSize = 16.sp, color = Color.White)
        }
    }
}

// ── Mock Data ──
private fun buildMockData(): List<ChatItem> = listOf(
    ChatItem.DateDivider("2026년 3월 28일"),

    ChatItem.AiMessage(
        text = "안녕하세요!\n세무 관련 궁금한 점이 있으시면 편하게 물어보세요.",
        time = "오전 9:30"
    ),

    ChatItem.UserMessage(
        text = "스타벅스에서 5,500원 결제했는데 어떤 세목으로 분류하면 좋을까요?",
        time = "오전 9:31"
    ),

    ChatItem.AiMessage(
        text = "카페/음료 업종 결제 내역을 분석했어요.",
        time = "오전 9:31",
        intentBadge = "경비 분류",
        card = CardContent.Classification(
            category = "복리후생비",
            merchant = "스타벅스 강남점",
            amount = "5,500원",
            confidence = 97,
            legalBasis = "소득세법 제19조 제2항\n카페/음료 업종에서의 직원 간 지출은 복리후생비로 분류됩니다. 접대비(연 3,600만원 한도)와 달리 한도 제한이 없어 절세에 유리합니다.",
            tip = "거래처 식사 = 접대비, 직원 간 커피 = 복리후생비. 복리후생비는 한도 없이 경비 인정돼요."
        )
    ),

    ChatItem.UserMessage(
        text = "종합소득세 세율이 어떻게 되나요?",
        time = "오전 9:33"
    ),

    ChatItem.AiMessage(
        text = "2026년 종합소득세 세율표입니다.\n현재 구간이 하이라이트 되어 있어요.",
        time = "오전 9:33",
        intentBadge = "세율 조회",
        card = CardContent.TaxRate(
            title = "종합소득세 세율표",
            subtitle = "소득세법 제55조",
            rows = listOf(
                TaxRateRow("~1,400만", "6%", "-"),
                TaxRateRow("~5,000만", "15%", "126만"),
                TaxRateRow("~8,800만", "24%", "576만"),
                TaxRateRow("~1.5억", "35%", "1,544만"),
                TaxRateRow("~3억", "38%", "1,994만"),
                TaxRateRow("3억 초과", "40~45%", "-")
            ),
            currentIndex = 2
        ),
        showConsultBanner = true
    ),

    ChatItem.UserMessage(
        text = "내년 세법 개정안 알려줘",
        time = "오전 9:35"
    ),

    ChatItem.AiMessage(
        text = "참고 자료에 직접적인 근거가 없어 정확한 답변이 어려워요. 세법 개정안은 국회 통과 전까지 변동될 수 있습니다.",
        time = "오전 9:35",
        isError = true
    ),

    ChatItem.Typing
)
