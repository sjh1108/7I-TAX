package com.ssafy.seveniTax.data.model.auth

data class PhoneConfirmResponse(
    val verified: Boolean,
    val userStatus: String,
    val ci: String,
    val verifiedAt: String
)
