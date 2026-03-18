package com.ssafy.seveniTax.data.model.auth

data class PhoneVerifyRequest(
    val phone: String,
    val carrier: String,
    val name: String,
    val birthDate: String,
    val gender: String
)
