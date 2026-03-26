package com.ssafy.seveniTax.viewmodel

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
                type = if (response.cardType == "CREDIT") "business" else "personal",
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
    val registerComplete: Boolean = false
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    fun loadCards() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val response = cardRepository.getCards()
            if (response.status == "success" && response.data != null) {
                _uiState.update {
                    it.copy(
                        cards = response.data.map { card -> RegisteredCard.from(card) },
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = response.message ?: "카드 목록 조회 실패") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "네트워크 오류") }
        }
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
        _uiState.update { it.copy(isLoading = true) }
        try {
            val request = CardCreateRequest(
                cardName = if (state.selectedCardType == "business") "사업자 카드" else "일반 카드",
                cardType = if (state.selectedCardType == "business") "CREDIT" else "DEBIT",
                cardUniqueNo = state.cardNumber,
                withdrawalAccountNo = "",
                withdrawalDate = state.expiry,
                otpToken = ""
            )
            val response = cardRepository.createCard(request)
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
                registerComplete = false
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
