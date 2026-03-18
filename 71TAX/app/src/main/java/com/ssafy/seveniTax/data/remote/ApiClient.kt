package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.BuildConfig
import com.ssafy.seveniTax.data.local.SecureStorage
import com.ssafy.seveniTax.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApiClient {

    fun buildOkHttpClient(secureStorage: SecureStorage): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                // 저장된 토큰 우선, 없으면 DEBUG에서만 DEV_TOKEN 폴백
                val token = secureStorage.getAccessToken()
                    ?: if (BuildConfig.DEBUG) Constants.DEV_TOKEN else null

                val request = chain.request().newBuilder()
                    .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
