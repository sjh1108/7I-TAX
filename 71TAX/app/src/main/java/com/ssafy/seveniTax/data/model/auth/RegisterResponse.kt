package com.ssafy.seveniTax.data.model.auth

data class RegisterResponse(
    val id: String,
    val email: String,
    val name: String,
    val status: String,
    val createdAt: String
)
