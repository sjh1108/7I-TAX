package com.ssafy.seveniTax.data.model.auth

data class PhoneConfirmResponse(
    val verified: Boolean,
    val userStatus: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val ci: String,
    val verifiedAt: String
)
