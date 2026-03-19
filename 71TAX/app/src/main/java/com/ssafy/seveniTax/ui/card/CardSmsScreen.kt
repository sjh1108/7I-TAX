package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CardSmsScreen(navController: NavController) {
    var smsCode by remember { mutableStateOf("") }
    var remainingSeconds by remember { mutableIntStateOf(180) }

    // Timer
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

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
                text = "SMS 인증",
                style = Typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "인증 번호를 발송했어요",
                style = Typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "카드 명의자 휴대폰으로 발송된 인증번호를\n아래에 입력해 주세요",
                style = Typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 발송 번호
            Text(text = "발송 번호", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "010-****-1234",
                    style = Typography.bodyLarge,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 인증번호 입력
            Text(text = "인증번호 입력", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = smsCode,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) smsCode = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("인증번호 6자리", color = TextSecondary) },
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Disabled
                    ),
                    singleLine = true,
                    textStyle = Typography.bodyLarge
                )
                // Timer badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (remainingSeconds > 0) Accent else Error)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = timerText,
                        style = Typography.labelLarge,
                        color = Background
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "인증번호 재발송",
                style = Typography.bodyMedium,
                color = Accent,
                modifier = Modifier.clickable {
                    remainingSeconds = 180
                    smsCode = ""
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "인증 번호가 오지 않으면 재발송을 눌러주세요",
                style = Typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            TaxButton(
                text = "인증하기",
                onClick = {
                    navController.navigate(Route.CardComplete.path)
                },
                enabled = smsCode.length == 6 && remainingSeconds > 0
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
