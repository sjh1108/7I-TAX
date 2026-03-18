package com.ssafy.seveniTax.data.model.auth

data class RefreshResponse(
    val accessToken: String,
    val expiresIn: Long
)
