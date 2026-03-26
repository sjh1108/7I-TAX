package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import com.ssafy.seveniTax.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

data class RegisteredCard(
    val id: String = UUID.randomUUID().toString(),
    val cardNumber: String,
    val expiry: String,
    val type: String,
    val isDefault: Boolean = false
)

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

    fun selectCardType(type: String) {
        _uiState.update { it.copy(selectedCardType = type) }
    }

    fun updateCardNumber(value: String) {
        _uiState.update { it.copy(cardNumber = value) }
    }

    fun updateExpiry(value: String) {
        _uiState.update { it.copy(expiry = value) }
    }

    fun completeRegistration() {
        val state = _uiState.value
        val last4 = if (state.cardNumber.length >= 4) state.cardNumber.takeLast(4) else state.cardNumber
        val maskedNumber = if (state.cardNumber.length == 16) {
            "${state.cardNumber.substring(0, 4)}  ••••  ••••  $last4"
        } else {
            "••••  ••••  ••••  $last4"
        }
        val expiryDisplay = if (state.expiry.length == 4) {
            "${state.expiry.substring(0, 2)}/${state.expiry.substring(2)}"
        } else state.expiry

        val isFirst = state.cards.isEmpty()
        val newCard = RegisteredCard(
            cardNumber = maskedNumber,
            expiry = expiryDisplay,
            type = state.selectedCardType.ifEmpty { "personal" },
            isDefault = isFirst
        )

        _uiState.update {
            it.copy(
                cards = it.cards + newCard,
                lastRegisteredCard = newCard,
                registerComplete = true
            )
        }
    }

    fun deleteCard(cardId: String) {
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

    fun setDefaultCard(cardId: String) {
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map { it.copy(isDefault = it.id == cardId) }
            )
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
