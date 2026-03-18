package com.ssafy.seveniTax.data.model.auth

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val authMethod: String = "local_auth"
)
