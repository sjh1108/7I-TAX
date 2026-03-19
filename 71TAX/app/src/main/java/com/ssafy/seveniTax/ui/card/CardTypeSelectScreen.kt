package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun CardTypeSelectScreen(navController: NavController) {
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
                text = "카드 등록",
                style = Typography.titleLarge
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "등록할 카드 유형을\n선택하세요",
                style = Typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 일반 카드 option
            CardTypeOption(
                backgroundColor = CardGold,
                title = "일반 카드",
                description = "개인 결제용 카드 등록",
                onClick = {
                    navController.navigate(Route.CardInput.create("personal"))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 사업자 카드 option
            CardTypeOption(
                backgroundColor = CardBlue,
                title = "사업자 카드",
                description = "사업 관련 결제 및 추후 증빙/관리용\n카드 등록",
                onClick = {
                    navController.navigate(Route.CardBusinessInfo.path)
                }
            )
        }
    }
}

@Composable
private fun CardTypeOption(
    backgroundColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = Typography.titleLarge,
                color = Color.White
            )
            Text(
                text = description,
                style = Typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
