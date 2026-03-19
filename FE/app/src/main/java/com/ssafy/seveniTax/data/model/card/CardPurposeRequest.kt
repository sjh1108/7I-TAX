package com.ssafy.seveniTax.data.model.card

data class CardPurposeRequest(
    val defaultPurpose: String
)

data class CardPurposeResponse(
    val id: String,
    val defaultPurpose: String,
    val updatedAt: String
)
