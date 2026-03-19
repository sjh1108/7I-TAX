package com.ssafy.seveniTax.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.seveniTax.ui.theme.*

enum class ButtonVariant { Primary, Secondary }

@Composable
fun TaxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary
) {
    val shape = RoundedCornerShape(12.dp)

    when (variant) {
        ButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(52.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Background,
                disabledContainerColor = Disabled,
                disabledContentColor = TextSecondary
            )
        ) {
            Text(text = text, style = Typography.labelLarge)
        }

        ButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(52.dp),
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, if (enabled) Accent else Disabled),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Accent,
                disabledContentColor = Disabled
            )
        ) {
            Text(text = text, style = Typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaxButtonPreview() {
    TaxButton(text = "시작하기", onClick = {})
}
