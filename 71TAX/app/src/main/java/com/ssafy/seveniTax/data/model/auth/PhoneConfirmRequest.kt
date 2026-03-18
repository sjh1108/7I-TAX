package com.ssafy.seveniTax.data.model.auth

data class PhoneConfirmRequest(
    val verificationId: String,
    val code: String
)
