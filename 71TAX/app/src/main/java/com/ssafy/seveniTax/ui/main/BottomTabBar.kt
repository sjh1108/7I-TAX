package com.ssafy.seveniTax.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.seveniTax.ui.theme.BrandPurple

private val TabInactiveColor = Color(0x80343434)

@Composable
fun BottomTabBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                ambientColor = Color(0x123629B7),
                spotColor = Color(0x123629B7)
            )
            .background(Color.White)
            .padding(top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) }
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    painter = painterResource(tab.iconRes),
                    contentDescription = tab.label,
                    tint = if (isSelected) BrandPurple else TabInactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tab.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) BrandPurple else TabInactiveColor
                )
            }
        }
    }
}
