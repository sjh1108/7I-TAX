package com.ssafy.seveniTax.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun CardInputScreen(
    navController: NavController,
    cardType: String = "personal"
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }

    val isFormComplete = cardNumber.length == 16
            && expiry.length == 4
            && cvc.length == 3
            && password.length == 2
            && birthDate.length == 6

    val previewCardColor = if (cardType == "personal") CardGold else CardBlue

    // Format card number for display
    val displayCardNumber = buildString {
        for (i in cardNumber.indices) {
            if (i > 0 && i % 4 == 0) append("-")
            if (i < 12) append(cardNumber[i])
            else append("•")
        }
        // Pad remaining
        val remaining = 16 - cardNumber.length
        if (remaining > 0) {
            for (i in 0 until remaining) {
                val pos = cardNumber.length + i
                if (pos > 0 && pos % 4 == 0) append("-")
                append("•")
            }
        }
    }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Card preview widget
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewCardColor)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (cardType == "personal") "일반카드" else "사업자카드",
                        style = Typography.labelLarge,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = displayCardNumber,
                            style = Typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = if (expiry.length == 4)
                                "${expiry.substring(0, 2)}/${expiry.substring(2)}"
                            else "",
                            style = Typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 카드 번호
            Text(text = "카드 번호", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) cardNumber = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("카드 번호 16자리 입력", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 유효기간 + CVC
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "유효기간 (MM/YY)", style = Typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) expiry = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("MM/YY", color = TextSecondary) },
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Disabled
                        ),
                        singleLine = true,
                        textStyle = Typography.bodyLarge
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "CVC (숫자 3자리)", style = Typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cvc,
                        onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) cvc = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("CVC", color = TextSecondary) },
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Disabled
                        ),
                        singleLine = true,
                        textStyle = Typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 카드 비밀번호 앞 2자리
            Text(text = "카드 비밀번호 앞 2자리", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("비밀번호 앞 2자리", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 생년월일
            Text(text = "생년월일", style = Typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = birthDate,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) birthDate = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("YYMMDD", color = TextSecondary) },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Disabled
                ),
                singleLine = true,
                textStyle = Typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "카드 정보는 암호화되어 안전하게 보관됩니다.",
                style = Typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            TaxButton(
                text = "다음",
                onClick = { navController.navigate(Route.CardOwnerVerify.path) },
                enabled = isFormComplete
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
