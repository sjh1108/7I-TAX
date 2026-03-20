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
                val token = secureStorage.getAccessToken()

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
