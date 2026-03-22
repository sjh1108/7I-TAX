package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.theme.*

private data class ChangeCard(
    val id: String,
    val name: String,
    val cardNumber: String,
    val isDefault: Boolean
)

private val mockChangeCards = listOf(
    ChangeCard("1", "일반카드", "5876-8847-2283-••••", true),
    ChangeCard("2", "사업자카드", "4120-9901-5532-••••", false)
)

@Composable
fun CardChangeScreen(navController: NavController) {
    var selectedCardId by remember { mutableStateOf(mockChangeCards.first { it.isDefault }.id) }

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
                text = "기본 카드 변경",
                style = Typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "결제 시 기본으로 사용할 카드를 선택해 주세요\n선택한 카드는 다음 결제부터 적용됩니다",
                style = Typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mockChangeCards) { card ->
                    val isSelected = card.id == selectedCardId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface)
                            .then(
                                if (isSelected) Modifier.border(
                                    width = 2.dp,
                                    color = Accent,
                                    shape = RoundedCornerShape(12.dp)
                                ) else Modifier
                            )
                            .clickable { selectedCardId = card.id }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radio indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) Accent else Disabled,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Accent)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = card.name,
                                    style = Typography.titleMedium
                                )
                                if (isSelected) {
                                    Text(
                                        text = "현재 기본카드",
                                        style = Typography.bodySmall,
                                        color = Accent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.cardNumber,
                                style = Typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Text(
                text = "카드를 탭하여 기본카드를 변경하세요",
                style = Typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            TaxButton(
                text = "변경하기",
                onClick = {
                    // TODO: 서버에 기본 카드 변경 요청
                    navController.popBackStack()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
