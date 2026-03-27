package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.payment.QrTokenCreateRequest
import com.ssafy.seveniTax.data.model.payment.QrTokenCreateResponse
import com.ssafy.seveniTax.data.remote.PaymentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val qrToken: String = "",
    val paymentId: Long? = null,
    val amount: Long = 0,
    val payerName: String = "",
    val paymentComplete: Boolean = false,
    val paymentStatus: String = ""
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentApi: PaymentApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun createQrToken(cardId: Long, amount: Long, merchantId: Long, merchantName: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val request = QrTokenCreateRequest(
                cardId = cardId,
                amount = amount,
                merchantId = merchantId,
                merchantName = merchantName,
                purpose = "BUSINESS"
            )
            val response = paymentApi.createQrToken(request)
            val body = response.body()
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        qrToken = body.data.token,
                        paymentId = body.data.paymentId,
                        amount = body.data.amount,
                        payerName = body.data.payerName
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = body?.message ?: "QR 토큰 생성 실패")
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류")
            }
        }
    }

    fun checkPaymentStatus(token: String) = viewModelScope.launch {
        try {
            val response = paymentApi.getQrPaymentStatus(token)
            val body = response.body()
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        paymentStatus = body.data.status,
                        paymentComplete = body.data.status == "CAPTURED"
                    )
                }
            }
        } catch (_: Exception) { }
    }

    fun resetPayment() {
        _uiState.update { PaymentUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
