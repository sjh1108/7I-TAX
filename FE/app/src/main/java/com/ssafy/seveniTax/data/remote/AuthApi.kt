package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.auth.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {

    @POST("auth/verify-identity")
    suspend fun verifyIdentity(@Body body: VerifyIdentityRequest): Response<ApiResponse<VerifyIdentityResponse>>

    @POST("auth/setup-pin")
    suspend fun setupPin(
        @Query("userId") userId: Long,
        @Body body: SetupPinRequest
    ): Response<ApiResponse<TokenResponse>>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/consents")
    suspend fun submitConsents(@Body body: List<ConsentItem>): Response<ApiResponse<Unit>>

    @POST("auth/reissue")
    suspend fun reissue(@Body body: ReissueRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
}
