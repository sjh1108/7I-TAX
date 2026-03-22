package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.auth.*
import com.ssafy.seveniTax.data.model.common.ApiResponse

interface AuthRepository {
    suspend fun verifyIdentity(request: VerifyIdentityRequest): ApiResponse<VerifyIdentityResponse>
    suspend fun setupPin(verifyToken: String, pin: String): ApiResponse<TokenResponse>
    suspend fun login(phoneNumber: String, pin: String): ApiResponse<TokenResponse>
    suspend fun submitConsents(consents: List<ConsentItem>): ApiResponse<Unit>
    suspend fun reissue(refreshToken: String): ApiResponse<TokenResponse>
    suspend fun logout()

    fun saveTokens(accessToken: String, refreshToken: String)
    fun saveUserInfo(userId: Long, phoneNumber: String)
    fun getStoredPhoneNumber(): String?
    fun hasStoredCredentials(): Boolean
    fun clearSession()
}
