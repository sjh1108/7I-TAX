package com.ssafy.seveniTax.util

import com.ssafy.seveniTax.BuildConfig

object Constants {
    val API_BASE_URL = if (BuildConfig.DEBUG)
        "http://10.0.2.2:8080/api/"
    else
        "https://api.taxsave.app/api/"

    val WEBVIEW_BASE_URL = if (BuildConfig.DEBUG)
        "http://10.0.2.2:3000"
    else
        "https://7itax.com"

    // 개발 중 토큰 하드코딩 분기
    const val DEV_TOKEN = "dev_access_token_placeholder"

    // SecureStorage keys
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_PIN_HASH = "pin_hash"
    const val KEY_USER_ID = "user_id"

    // PIN
    const val PIN_LENGTH = 6

    // SMS
    const val SMS_TIMEOUT_SECONDS = 180
}
