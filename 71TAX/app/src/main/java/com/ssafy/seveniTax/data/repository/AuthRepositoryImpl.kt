package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.local.SecureStorage
import com.ssafy.seveniTax.data.model.auth.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.remote.AuthApi
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage
) : AuthRepository {

    override suspend fun verifyIdentity(request: VerifyIdentityRequest): ApiResponse<VerifyIdentityResponse> {
        val response = authApi.verifyIdentity(request)
        return response.body() ?: throw Exception(response.errorBody()?.string() ?: "본인인증 실패")
    }

    override suspend fun setupPin(userId: Long, pin: String): ApiResponse<TokenResponse> {
        val response = authApi.setupPin(userId, SetupPinRequest(pin))
        val body = response.body() ?: throw Exception(response.errorBody()?.string() ?: "PIN 설정 실패")
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun login(phoneNumber: String, pin: String): ApiResponse<TokenResponse> {
        val response = authApi.login(LoginRequest(phoneNumber, pin))
        val body = response.body() ?: throw Exception(response.errorBody()?.string() ?: "로그인 실패")
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun submitConsents(consents: List<ConsentItem>): ApiResponse<Unit> {
        val response = authApi.submitConsents(consents)
        return response.body() ?: throw Exception(response.errorBody()?.string() ?: "약관 동의 실패")
    }

    override suspend fun reissue(refreshToken: String): ApiResponse<TokenResponse> {
        val response = authApi.reissue(ReissueRequest(refreshToken))
        val body = response.body() ?: throw Exception(response.errorBody()?.string() ?: "토큰 갱신 실패")
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun logout() {
        try { authApi.logout() } catch (_: Exception) {}
        clearSession()
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        secureStorage.saveAccessToken(accessToken)
        secureStorage.saveRefreshToken(refreshToken)
    }

    override fun saveUserInfo(userId: Long, phoneNumber: String) {
        secureStorage.saveUserId(userId.toString())
        secureStorage.savePhoneNumber(phoneNumber)
    }

    override fun getStoredPhoneNumber(): String? = secureStorage.getPhoneNumber()

    override fun hasStoredCredentials(): Boolean =
        secureStorage.getPhoneNumber() != null

    override fun clearSession() {
        secureStorage.clearAll()
    }
}
