package com.ssafy.seveniTax.data.model.auth

data class PhoneVerifyResponse(
    val verificationId: String,
    val expiresIn: Int,
    val message: String
)
