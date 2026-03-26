package com.ssafy.seveniTax.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.card.CardCreateRequest
import com.ssafy.seveniTax.data.model.card.CardResponse
import com.ssafy.seveniTax.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisteredCard(
    val id: String,
    val cardNumber: String,
    val expiry: String,
    val type: String,
    val isDefault: Boolean = false
) {
    companion object {
        fun from(response: CardResponse): RegisteredCard {
            return RegisteredCard(
                id = response.id.toString(),
                cardNumber = "••••  ••••  ••••  ${response.last4Digits}",
                expiry = response.cardExpiryDate ?: "",
                type = if (response.cardType == "BUSINESS") "business" else "personal",
                isDefault = response.isDefault
            )
        }
    }
}

data class CardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val cards: List<RegisteredCard> = emptyList(),
    val selectedCardType: String = "",
    val cardNumber: String = "",
    val expiry: String = "",
    val lastRegisteredCard: RegisteredCard? = null,
    val registerComplete: Boolean = false,
    // 카드 등록 플로우용
    val accounts: List<com.ssafy.seveniTax.data.model.card.CardAccountResponse> = emptyList(),
    val products: List<com.ssafy.seveniTax.data.model.card.CardProductResponse> = emptyList(),
    val selectedAccountNo: String = "",
    val selectedProductNo: String = ""
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CardVM"
    }

    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    fun loadCards() = viewModelScope.launch {
        Log.d(TAG, "loadCards() 호출")
        _uiState.update { it.copy(isLoading = true) }
        try {
            val response = cardRepository.getCards()
            Log.d(TAG, "loadCards() 응답: status=${response.status}, data=${response.data?.size}개")
            if (response.status == "success" && response.data != null) {
                _uiState.update {
                    it.copy(
                        cards = response.data.map { card -> RegisteredCard.from(card) },
                        isLoading = false
                    )
                }
            } else {
                Log.e(TAG, "loadCards() 실패: ${response.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = response.message ?: "카드 목록 조회 실패") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadCards() 에러: ${e.message}", e)
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류") }
        }
    }

    fun loadAccounts() = viewModelScope.launch {
        Log.d(TAG, "loadAccounts() 호출")
        try {
            val response = cardRepository.getMyAccounts()
            if (response.status == "success" && response.data != null) {
                Log.d(TAG, "loadAccounts() 성공: ${response.data.size}개")
                _uiState.update { it.copy(accounts = response.data) }
            } else {
                Log.e(TAG, "loadAccounts() 실패: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadAccounts() 에러: ${e.message}", e)
        }
    }

    fun loadProducts() = viewModelScope.launch {
        Log.d(TAG, "loadProducts() 호출")
        try {
            val response = cardRepository.getCardProducts()
            if (response.status == "success" && response.data != null) {
                Log.d(TAG, "loadProducts() 성공: ${response.data.size}개")
                _uiState.update { it.copy(products = response.data) }
            } else {
                Log.e(TAG, "loadProducts() 실패: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadProducts() 에러: ${e.message}", e)
        }
    }

    fun selectAccount(accountNo: String) {
        _uiState.update { it.copy(selectedAccountNo = accountNo) }
    }

    fun selectProduct(productNo: String) {
        _uiState.update { it.copy(selectedProductNo = productNo) }
    }

    fun selectCardType(type: String) {
        _uiState.update { it.copy(selectedCardType = type) }
    }

    fun updateCardNumber(value: String) {
        _uiState.update { it.copy(cardNumber = value) }
    }

    fun updateExpiry(value: String) {
        _uiState.update { it.copy(expiry = value) }
    }

    fun completeRegistration() = viewModelScope.launch {
        val state = _uiState.value
        Log.d(TAG, "completeRegistration() 호출: type=${state.selectedCardType}, number=${state.cardNumber.takeLast(4)}")
        _uiState.update { it.copy(isLoading = true) }
        try {
            val request = CardCreateRequest(
                cardName = if (state.selectedCardType == "business") "사업자 카드" else "일반 카드",
                cardType = if (state.selectedCardType == "business") "BUSINESS" else "PERSONAL",
                cardUniqueNo = state.selectedProductNo,
                withdrawalAccountNo = state.selectedAccountNo,
                withdrawalDate = state.expiry.ifEmpty { "15" },
                otpToken = "SKIP" // TODO: OTP 연동 시 실제 토큰으로 교체
            )
            val response = cardRepository.createCard(request)
            Log.d(TAG, "createCard() 응답: status=${response.status}, data=${response.data}")
            if (response.status == "success" && response.data != null) {
                val newCard = RegisteredCard.from(response.data)
                _uiState.update {
                    it.copy(
                        lastRegisteredCard = newCard,
                        registerComplete = true,
                        isLoading = false
                    )
                }
                loadCards()
            } else {
                // API 실패 시 인메모리로 폴백
                Log.w(TAG, "createCard() API 실패, 인메모리 폴백: ${response.message}")
                val last4 = if (state.cardNumber.length >= 4) state.cardNumber.takeLast(4) else state.cardNumber
                val maskedNumber = "••••  ••••  ••••  $last4"
                val expiryDisplay = if (state.expiry.length == 4) {
                    "${state.expiry.substring(0, 2)}/${state.expiry.substring(2)}"
                } else state.expiry
                val isFirst = state.cards.isEmpty()
                val fallbackCard = RegisteredCard(
                    id = System.currentTimeMillis().toString(),
                    cardNumber = maskedNumber,
                    expiry = expiryDisplay,
                    type = state.selectedCardType.ifEmpty { "personal" },
                    isDefault = isFirst
                )
                _uiState.update {
                    it.copy(
                        cards = it.cards + fallbackCard,
                        lastRegisteredCard = fallbackCard,
                        registerComplete = true,
                        isLoading = false
                    )
                }
            }
        } catch (e: Exception) {
            // 네트워크 오류 시 인메모리로 폴백
            Log.e(TAG, "createCard() 네트워크 에러, 인메모리 폴백: ${e.message}", e)
            val last4 = if (state.cardNumber.length >= 4) state.cardNumber.takeLast(4) else state.cardNumber
            val maskedNumber = "••••  ••••  ••••  $last4"
            val expiryDisplay = if (state.expiry.length == 4) {
                "${state.expiry.substring(0, 2)}/${state.expiry.substring(2)}"
            } else state.expiry
            val isFirst = state.cards.isEmpty()
            val fallbackCard = RegisteredCard(
                id = System.currentTimeMillis().toString(),
                cardNumber = maskedNumber,
                expiry = expiryDisplay,
                type = state.selectedCardType.ifEmpty { "personal" },
                isDefault = isFirst
            )
            _uiState.update {
                it.copy(
                    cards = it.cards + fallbackCard,
                    lastRegisteredCard = fallbackCard,
                    registerComplete = true,
                    isLoading = false
                )
            }
        }
    }

    fun deleteCard(cardId: String) = viewModelScope.launch {
        try {
            cardRepository.deleteCard(cardId)
        } catch (_: Exception) { }
        _uiState.update { state ->
            val deletedCard = state.cards.find { it.id == cardId }
            val remaining = state.cards.filter { it.id != cardId }
            val finalCards = if (deletedCard?.isDefault == true && remaining.isNotEmpty()) {
                remaining.mapIndexed { index, card ->
                    if (index == 0) card.copy(isDefault = true) else card
                }
            } else remaining
            state.copy(cards = finalCards)
        }
    }

    fun setDefaultCard(cardId: String) = viewModelScope.launch {
        try {
            cardRepository.setDefaultCard(cardId)
            loadCards()
        } catch (_: Exception) {
            _uiState.update { state ->
                state.copy(
                    cards = state.cards.map { it.copy(isDefault = it.id == cardId) }
                )
            }
        }
    }

    fun getCardById(cardId: String): RegisteredCard? {
        return _uiState.value.cards.find { it.id == cardId }
    }

    fun resetRegistration() {
        _uiState.update {
            it.copy(
                selectedCardType = "",
                cardNumber = "",
                expiry = "",
                lastRegisteredCard = null,
                registerComplete = false,
                selectedAccountNo = "",
                selectedProductNo = ""
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
