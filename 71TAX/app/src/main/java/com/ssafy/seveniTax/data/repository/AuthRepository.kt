package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.auth.*
import com.ssafy.seveniTax.data.model.common.ApiResponse

interface AuthRepository {
    suspend fun refresh(refreshToken: String): ApiResponse<RefreshResponse>
    suspend fun logout()
    suspend fun getTerms(): ApiResponse<List<TermItem>>
    suspend fun agreeTerms(request: TermsAgreeRequest): ApiResponse<TermsAgreeResponse>
    suspend fun requestPhoneVerify(request: PhoneVerifyRequest): ApiResponse<PhoneVerifyResponse>
    suspend fun confirmPhoneVerify(request: PhoneConfirmRequest): ApiResponse<PhoneConfirmResponse>
}
