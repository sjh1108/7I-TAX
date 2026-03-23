package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.pay.*
import com.ssafy.seveniTax.data.remote.PayApi
import javax.inject.Inject

class PayRepositoryImpl @Inject constructor(
    private val payApi: PayApi
) : PayRepository {

    override suspend fun createAccount(request: AccountCreateRequest): ApiResponse<AccountResponse> =
        TODO("Implement")

    override suspend fun getAccounts(type: String?): ApiResponse<List<AccountResponse>> =
        TODO("Implement")

    override suspend fun getAccount(id: String): ApiResponse<AccountResponse> =
        TODO("Implement")

    override suspend fun getBalance(accountId: String): ApiResponse<BalanceResponse> =
        TODO("Implement")
}
