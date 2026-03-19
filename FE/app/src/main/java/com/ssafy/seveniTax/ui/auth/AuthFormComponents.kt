package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun FormFieldReadOnly(label: String, value: String) {
    Column {
        Text(label, style = Typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = Typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Divider)
    }
}

fun buildSsnDisplay(front: String, back: String): String {
    return "$front-$back${"●".repeat(6)}"
}

fun formatPhoneForm(raw: String): String {
    return when {
        raw.length <= 3 -> raw
        raw.length <= 7 -> "${raw.substring(0, 3)}-${raw.substring(3)}"
        else -> "${raw.substring(0, 3)}-${raw.substring(3, 7)}-${raw.substring(7)}"
    }
}
