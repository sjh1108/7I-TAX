package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.card.CardResponse
import com.ssafy.seveniTax.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val cards: List<CardResponse> = emptyList(),
    // 카드 등록 입력
    val selectedCardType: String = "",   // "debit" | "credit"
    val cardNumber: String = "",
    val expiry: String = "",
    val cvc: String = "",
    val cardAlias: String = "",
    // 소유자 인증
    val ownerVerified: Boolean = false,
    val smsCode: String = "",
    // 등록 결과
    val createdCard: CardResponse? = null,
    val registerComplete: Boolean = false
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    fun loadCards() = viewModelScope.launch {
        TODO("Implement: cardRepository.getCards()")
    }

    fun selectCardType(type: String) {
        _uiState.update { it.copy(selectedCardType = type) }
    }

    fun updateCardNumber(value: String) { _uiState.update { it.copy(cardNumber = value) } }
    fun updateExpiry(value: String) { _uiState.update { it.copy(expiry = value) } }
    fun updateCvc(value: String) { _uiState.update { it.copy(cvc = value) } }
    fun updateSmsCode(value: String) { _uiState.update { it.copy(smsCode = value) } }

    fun createCard(linkedAccountId: String, type: String, alias: String? = null) = viewModelScope.launch {
        TODO("Implement: cardRepository.createCard()")
    }

    fun requestOwnerVerify() = viewModelScope.launch {
        TODO("Implement: 소유자 인증 요청")
    }

    fun registerCard(onSuccess: () -> Unit) = viewModelScope.launch {
        TODO("Implement: SMS 검증 → cardRepository.activateCard() → registerComplete = true → onSuccess()")
    }

    fun activateCard(cardId: String, code: String) = viewModelScope.launch {
        TODO("Implement: cardRepository.activateCard()")
    }

    fun setCardPurpose(cardId: String, purpose: String) = viewModelScope.launch {
        TODO("Implement: cardRepository.setCardPurpose()")
    }

    fun deleteCard(cardId: String) = viewModelScope.launch {
        TODO("Implement: cardRepository.deleteCard() → loadCards()")
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
