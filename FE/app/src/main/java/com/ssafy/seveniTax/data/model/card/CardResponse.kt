package com.ssafy.seveniTax.data.model.card

data class CardResponse(
    val id: String,
    val linkedAccountId: String,
    val cardType: String,
    val cardNumberLast4: String,
    val cardAlias: String?,
    val status: String,
    val createdAt: String
)
