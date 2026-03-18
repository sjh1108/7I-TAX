package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.auth.Carrier
import com.ssafy.seveniTax.data.model.auth.TermItem
import com.ssafy.seveniTax.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AutoLoginResult { NONE, SUCCESS, FAILURE }

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    // 회원가입 입력
    val phone: String = "",
    val residentFront: String = "",
    val residentBack: String = "",
    val carrier: Carrier? = null,
    val name: String = "",
    // 약관
    val terms: List<TermItem> = emptyList(),
    val agreedTermIds: Set<String> = emptySet(),
    val allRequiredTermsAgreed: Boolean = false,
    // SMS
    val smsCode: String = "",
    val smsVerified: Boolean = false,
    // PIN
    val pin: String = "",
    val pinConfirm: String = "",
    // 완료
    val authComplete: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _autoLoginResult = MutableStateFlow(AutoLoginResult.NONE)
    val autoLoginResult: StateFlow<AutoLoginResult> = _autoLoginResult.asStateFlow()

    fun checkAutoLogin() = viewModelScope.launch {
        TODO("Implement: SecureStorage 토큰 확인 → SUCCESS or FAILURE")
    }

    // Phone
    fun updatePhone(phone: String) { _uiState.update { it.copy(phone = phone) } }

    // Resident
    fun updateResidentFront(value: String) { _uiState.update { it.copy(residentFront = value) } }
    fun updateResidentBack(value: String) { _uiState.update { it.copy(residentBack = value) } }

    // Carrier
    fun updateCarrier(carrier: Carrier) { _uiState.update { it.copy(carrier = carrier) } }

    // Name
    fun updateName(name: String) { _uiState.update { it.copy(name = name) } }

    // Terms
    fun loadTerms() = viewModelScope.launch {
        TODO("Implement: authRepository.getTerms()")
    }

    fun toggleTerm(termId: String) {
        _uiState.update { state ->
            val newSet = if (state.agreedTermIds.contains(termId))
                state.agreedTermIds - termId
            else
                state.agreedTermIds + termId
            val allRequired = state.terms.filter { it.required }.all { newSet.contains(it.id) }
            state.copy(agreedTermIds = newSet, allRequiredTermsAgreed = allRequired)
        }
    }

    fun submitTerms() = viewModelScope.launch {
        TODO("Implement: authRepository.agreeTerms()")
    }

    // SMS
    fun updateSmsCode(code: String) { _uiState.update { it.copy(smsCode = code) } }

    fun requestSmsVerification() = viewModelScope.launch {
        TODO("Implement: authRepository.requestPhoneVerify()")
    }

    fun verifySms(onSuccess: () -> Unit) = viewModelScope.launch {
        TODO("Implement: authRepository.confirmPhoneVerify() → smsVerified = true → onSuccess()")
    }

    // PIN Setup
    fun appendPin(digit: String) {
        _uiState.update { if (it.pin.length < 6) it.copy(pin = it.pin + digit) else it }
    }

    fun deletePin() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1)) }
    }

    // PIN Confirm
    fun appendPinConfirm(digit: String) {
        _uiState.update { if (it.pinConfirm.length < 6) it.copy(pinConfirm = it.pinConfirm + digit) else it }
    }

    fun deletePinConfirm() {
        _uiState.update { it.copy(pinConfirm = it.pinConfirm.dropLast(1)) }
    }

    fun resetPinConfirm() {
        _uiState.update { it.copy(pinConfirm = "", errorMessage = "PIN이 일치하지 않습니다.") }
    }

    fun confirmPin(onSuccess: () -> Unit, onMismatch: () -> Unit) {
        TODO("Implement: pin == pinConfirm → 해시 저장 → onSuccess() else onMismatch()")
    }

    fun register() = viewModelScope.launch {
        TODO("Implement: authRepository.register() → login() → 토큰 저장")
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
