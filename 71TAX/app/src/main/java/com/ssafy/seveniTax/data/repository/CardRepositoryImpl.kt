package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.card.*
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.remote.CardApi
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val cardApi: CardApi
) : CardRepository {

    override suspend fun createCard(request: CardCreateRequest): ApiResponse<CardResponse> =
        TODO("Implement")

    override suspend fun activateCard(cardId: String, request: CardActivateRequest): ApiResponse<CardActivateResponse> =
        TODO("Implement")

    override suspend fun setCardPurpose(cardId: String, request: CardPurposeRequest): ApiResponse<CardPurposeResponse> =
        TODO("Implement")

    override suspend fun getCards(): ApiResponse<List<CardResponse>> =
        TODO("Implement")

    override suspend fun deleteCard(cardId: String) =
        TODO("Implement")
}
