package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

// Mock card data for UI development
data class MockCard(
    val id: String,
    val name: String,
    val cardNumber: String,
    val expiry: String,
    val type: String // "personal" or "business"
)

private val mockCards = listOf(
    MockCard("1", "일반카드", "5876-8847-2283-••••", "12/27", "personal"),
    MockCard("2", "사업자카드", "4120-9901-5532-••••", "09/28", "business")
)

@Composable
fun CardListScreen(navController: NavController) {
    // Toggle this to test empty vs filled state
    var cards by remember { mutableStateOf(mockCards) }
    val isEmpty = cards.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Background)
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
                text = "카드 관리",
                style = Typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "기본 카드 변경",
                style = Typography.bodyMedium,
                color = Accent,
                modifier = Modifier
                    .clickable { navController.navigate(Route.CardChange.path) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            if (isEmpty) {
                // Empty state
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .border(
                            border = BorderStroke(1.5.dp, Disabled),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "등록된 카드가 없습니다",
                            style = Typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "카드를 등록하면 간편하게 결제를 이용할 수 있어요",
                            style = Typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // Card list
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards) { card ->
                        CardWidget(
                            card = card,
                            onClick = {
                                navController.navigate(Route.CardDetail.create(card.id))
                            }
                        )
                    }
                }
            }

            // Bottom section
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TaxButton(
                    text = "카드 등록하기",
                    onClick = { navController.navigate(Route.CardTypeSelect.path) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "일반·사업자 카드 최대 5장까지 등록 가능",
                    style = Typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CardWidget(
    card: MockCard,
    onClick: () -> Unit
) {
    val bgColor = if (card.type == "personal") CardGold else CardBlue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = card.name,
                style = Typography.labelLarge,
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = card.cardNumber,
                    style = Typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = card.expiry,
                    style = Typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
