package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.card.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.*

interface CardApi {

    @POST("cards")
    suspend fun createCard(@Body body: CardCreateRequest): Response<ApiResponse<CardResponse>>

    @POST("cards/{id}/activate")
    suspend fun activateCard(
        @Path("id") id: String,
        @Body body: CardActivateRequest
    ): Response<ApiResponse<CardActivateResponse>>

    @PATCH("cards/{id}/purpose")
    suspend fun setCardPurpose(
        @Path("id") id: String,
        @Body body: CardPurposeRequest
    ): Response<ApiResponse<CardPurposeResponse>>

    @GET("cards")
    suspend fun getCards(): Response<ApiResponse<List<CardResponse>>>

    @DELETE("cards/{id}")
    suspend fun deleteCard(@Path("id") id: String): Response<Unit>
}
