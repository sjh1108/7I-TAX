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

    override suspend fun register(request: RegisterRequest): ApiResponse<RegisterResponse> =
        TODO("Implement")

    override suspend fun login(request: LoginRequest): ApiResponse<LoginResponse> =
        TODO("Implement")

    override suspend fun refresh(refreshToken: String): ApiResponse<RefreshResponse> =
        TODO("Implement")

    override suspend fun logout() =
        TODO("Implement")

    override suspend fun getTerms(): ApiResponse<List<TermItem>> =
        TODO("Implement")

    override suspend fun agreeTerms(request: TermsAgreeRequest): ApiResponse<TermsAgreeResponse> =
        TODO("Implement")

    override suspend fun requestPhoneVerify(request: PhoneVerifyRequest): ApiResponse<PhoneVerifyResponse> =
        TODO("Implement")

    override suspend fun confirmPhoneVerify(request: PhoneConfirmRequest): ApiResponse<PhoneConfirmResponse> =
        TODO("Implement")
}
