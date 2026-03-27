package com.ssafy.seveniTax.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.payment.QrTokenCreateRequest
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

    companion object {
        private const val TAG = "PaymentVM"
    }

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun createQrToken(cardId: Long, amount: Long, merchantId: Long, merchantName: String) = viewModelScope.launch {
        Log.d(TAG, "▶ createQrToken() cardId=$cardId, amount=$amount, merchantId=$merchantId, merchant=$merchantName")
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val request = QrTokenCreateRequest(
                cardId = cardId,
                amount = amount,
                merchantId = merchantId,
                merchantName = merchantName,
                purpose = "BUSINESS"
            )
            Log.d(TAG, "  요청: $request")
            val response = paymentApi.createQrToken(request)
            val body = response.body()
            Log.d(TAG, "  응답: code=${response.code()}, status=${body?.status}, token=${body?.data?.token}")
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
                val errMsg = body?.message ?: "QR 토큰 생성 실패 (${response.code()})"
                Log.e(TAG, "  createQrToken 실패: $errMsg")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = errMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  createQrToken 에러: ${e.message}", e)
            _uiState.update {
                it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류")
            }
        }
    }

    fun checkPaymentStatus(token: String) = viewModelScope.launch {
        Log.d(TAG, "▶ checkPaymentStatus() token=$token")
        try {
            val response = paymentApi.getQrPaymentStatus(token)
            val body = response.body()
            Log.d(TAG, "  상태: ${body?.data?.status}")
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        paymentStatus = body.data.status,
                        paymentComplete = body.data.status == "CAPTURED"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  checkPaymentStatus 에러: ${e.message}", e)
        }
    }

    fun lookupQrPayment(token: String) = viewModelScope.launch {
        Log.d(TAG, "▶ lookupQrPayment() token=$token")
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val response = paymentApi.getQrPaymentInfo(token)
            val body = response.body()
            Log.d(TAG, "  조회 응답: ${body?.data}")
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        qrToken = token,
                        amount = body.data.amount,
                        payerName = body.data.merchantName
                    )
                }
            } else {
                Log.e(TAG, "  조회 실패: ${body?.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = body?.message ?: "결제 정보 조회 실패") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  조회 에러: ${e.message}", e)
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류") }
        }
    }

    fun confirmQrPayment(token: String) = viewModelScope.launch {
        Log.d(TAG, "▶ confirmQrPayment() token=$token")
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val response = paymentApi.confirmQrPayment(token)
            val body = response.body()
            val errorBody = response.errorBody()?.string()
            Log.d(TAG, "  결제 응답: code=${response.code()}, body=${body}, errorBody=$errorBody")
            if (response.isSuccessful && body?.status == "success" && body.data != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        paymentComplete = true,
                        paymentStatus = body.data.status
                    )
                }
            } else {
                val errMsg = body?.message ?: errorBody ?: "결제 승인 실패 (${response.code()})"
                Log.e(TAG, "  결제 실패: $errMsg")
                _uiState.update { it.copy(isLoading = false, errorMessage = errMsg) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "  결제 에러: ${e.message}", e)
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류") }
        }
    }

    fun resetPayment() {
        _uiState.update { PaymentUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
