package com.ssafy.seveniTax.data.model.auth

data class TermsAgreeRequest(
    val agreements: List<TermAgreement>
)

data class TermAgreement(
    val termId: String,
    val agreed: Boolean
)
