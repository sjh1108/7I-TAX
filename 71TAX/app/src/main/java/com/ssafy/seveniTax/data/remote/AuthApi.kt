package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.auth.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<ApiResponse<RefreshResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("terms")
    suspend fun getTerms(): Response<ApiResponse<List<TermItem>>>

    @POST("terms/agree")
    suspend fun agreeTerms(@Body body: TermsAgreeRequest): Response<ApiResponse<TermsAgreeResponse>>

    @POST("verification/phone/request")
    suspend fun requestPhoneVerify(@Body body: PhoneVerifyRequest): Response<ApiResponse<PhoneVerifyResponse>>

    @POST("verification/phone/confirm")
    suspend fun confirmPhoneVerify(@Body body: PhoneConfirmRequest): Response<ApiResponse<PhoneConfirmResponse>>
}
