package com.ssafy.seveniTax.data.model.pay

data class BalanceResponse(
    val accountId: String,
    val balance: Long,
    val availableBalance: Long,
    val pendingAmount: Long,
    val asOf: String
)
