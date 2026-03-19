package com.ssafy.seveniTax.data.model.card

data class CardCreateRequest(
    val linkedAccountId: String,
    val cardType: String,
    val cardAlias: String? = null
)
