package com.ssafy.seveniTax.ui.payment

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

private data class MockCard(
    val name: String,
    val last4: String,
    val color: Color
)

private val mockCards = listOf(
    MockCard("신한카드", "5678", Color(0xFF2196F3)),
    MockCard("국민카드", "2342", Color(0xFF1A237E)),
    MockCard("현대카드", "9018", Color(0xFFE53935)),
    MockCard("삼성카드", "1234", Color(0xFF00897B)),
)

@Composable
fun QrPaymentScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(1) } // 0=바코드, 1=QR스캔
    var selectedCard by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── 상단 바 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Pay 결제",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 바코드 / QR스캔 탭 ──
        Row(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth()
                .border(1.dp, Divider, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            TabButton("바코드", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
            TabButton("QR스캔", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── QR / 바코드 영역 ──
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(1.dp, Divider, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTab == 1) {
                // QR 코드 (Mock)
                val qrBitmap = remember { generateMockQr() }
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR 코드",
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                )
            } else {
                // 바코드 (빈 상태)
                Text("바코드 영역", color = TextSecondary, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 카드 슬라이더 ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            items(mockCards.size) { index ->
                val card = mockCards[index]
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(card.color)
                        .border(
                            width = if (index == selectedCard) 3.dp else 0.dp,
                            color = if (index == selectedCard) BrandPurple else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(card.name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("••••${card.last4}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (selected) TextPrimary else Color.Transparent)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TextSecondary,
            modifier = Modifier.then(
                Modifier.padding(horizontal = 8.dp)
            )
        )
    }
}

private fun generateMockQr(): Bitmap {
    val size = 256
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val random = java.util.Random(42)
    val moduleCount = 25
    val moduleSize = size / moduleCount

    // 흰색 배경
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, android.graphics.Color.WHITE)
        }
    }

    // QR 패턴 생성 (Mock)
    for (row in 0 until moduleCount) {
        for (col in 0 until moduleCount) {
            val isFinderPattern = (row < 7 && col < 7) ||
                    (row < 7 && col >= moduleCount - 7) ||
                    (row >= moduleCount - 7 && col < 7)

            val isFilled = if (isFinderPattern) {
                val lr = if (row < 7) row else row - (moduleCount - 7)
                val lc = if (col < 7) col else col - (moduleCount - 7)
                lr == 0 || lr == 6 || lc == 0 || lc == 6 || (lr in 2..4 && lc in 2..4)
            } else {
                random.nextBoolean()
            }

            if (isFilled) {
                for (px in 0 until moduleSize) {
                    for (py in 0 until moduleSize) {
                        val x = col * moduleSize + px
                        val y = row * moduleSize + py
                        if (x < size && y < size) {
                            bitmap.setPixel(x, y, android.graphics.Color.BLACK)
                        }
                    }
                }
            }
        }
    }
    return bitmap
}
