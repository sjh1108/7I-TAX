package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.BuildConfig
import com.ssafy.seveniTax.data.local.SecureStorage
import com.ssafy.seveniTax.data.model.auth.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.remote.AuthApi
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage
) : AuthRepository {

    // TODO: 백엔드 연동 시 false로 변경
    private val useMock = BuildConfig.DEBUG

    override suspend fun verifyIdentity(request: VerifyIdentityRequest): ApiResponse<VerifyIdentityResponse> {
        if (useMock) {
            return ApiResponse(
                status = "success",
                data = VerifyIdentityResponse(
                    userId = 1L,
                    isNewUser = true,
                    requiresPinSetup = true,
                    requiresConsent = true
                )
            )
        }
        val response = authApi.verifyIdentity(request)
        return response.body() ?: throw Exception(response.errorBody()?.string() ?: "본인인증 실패")
    }

    override suspend fun setupPin(userId: Long, pin: String): ApiResponse<TokenResponse> {
        if (useMock) {
            val mockToken = TokenResponse("mock_access_token", "mock_refresh_token")
            saveTokens(mockToken.accessToken, mockToken.refreshToken)
            return ApiResponse(status = "success", data = mockToken)
        }
        val response = authApi.setupPin(userId, SetupPinRequest(pin))
        val body = response.body() ?: throw Exception(response.errorBody()?.string() ?: "PIN 설정 실패")
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun login(phoneNumber: String, pin: String): ApiResponse<TokenResponse> {
        if (useMock) {
            val mockToken = TokenResponse("mock_access_token", "mock_refresh_token")
            saveTokens(mockToken.accessToken, mockToken.refreshToken)
            return ApiResponse(status = "success", data = mockToken)
        }
        val response = authApi.login(LoginRequest(phoneNumber, pin))
        val body = response.body() ?: throw Exception(response.errorBody()?.string() ?: "로그인 실패")
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun submitConsents(consents: List<ConsentItem>): ApiResponse<Unit> {
        if (useMock) {
            return ApiResponse(status = "success", data = null)
        }
        val response = authApi.submitConsents(consents)
        return response.body() ?: throw Exception(response.errorBody()?.string() ?: "약관 동의 실패")
    }

    override suspend fun reissue(refreshToken: String): ApiResponse<TokenResponse> {
        if (useMock) {
            val mockToken = TokenResponse("mock_access_token_new", "mock_refresh_token_new")
            saveTokens(mockToken.accessToken, mockToken.refreshToken)
            return ApiResponse(status = "success", data = mockToken)
        }
        val response = authApi.reissue(ReissueRequest(refreshToken))
        val body = response.body() ?: throw Exception(response.errorBody()?.string() ?: "토큰 갱신 실패")
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun logout() {
        if (!useMock) {
            try { authApi.logout() } catch (_: Exception) {}
        }
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
