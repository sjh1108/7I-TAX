package com.ssafy.seveniTax.data.model.auth

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val name: String,
    val status: String,
    val plan: String
)
