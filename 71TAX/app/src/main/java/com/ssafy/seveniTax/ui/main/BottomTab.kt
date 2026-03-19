package com.ssafy.seveniTax.ui.main

import androidx.annotation.DrawableRes
import com.ssafy.seveniTax.R

enum class BottomTab(
    val label: String,
    @DrawableRes val iconRes: Int
) {
    HOME("홈", R.drawable.ic_24),
    DASHBOARD("대시보드", R.drawable.ic_16),
    SETTINGS("설정", R.drawable.ic_22),
    AI("AI", R.drawable.ic_45)
}
