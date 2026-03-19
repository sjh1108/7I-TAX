package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.auth.ConsentItem
import com.ssafy.seveniTax.data.model.auth.TermItem
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityRequest
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
    // 본인인증 입력
    val phone: String = "",
    val residentFront: String = "",   // 생년월일 6자리
    val residentBack: String = "",    // 뒷자리 1자리 (성별)
    val name: String = "",
    // 본인인증 결과
    val userId: Long = 0L,
    val requiresPinSetup: Boolean = false,
    val requiresConsent: Boolean = false,
    // 약관
    val terms: List<TermItem> = emptyList(),
    val agreedTermIds: Set<String> = emptySet(),
    val allRequiredTermsAgreed: Boolean = false,
    // PIN
    val pin: String = "",
    val pinConfirm: String = "",
    val pinFailCount: Int = 0,
    // PIN 로그인 (재방문)
    val loginPin: String = "",
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

    // ── [1] Splash ────────────────────────────────────────
    fun checkAutoLogin() = viewModelScope.launch {
        kotlinx.coroutines.delay(1500)
        if (authRepository.hasStoredCredentials()) {
            _autoLoginResult.value = AutoLoginResult.SUCCESS
        } else {
            _autoLoginResult.value = AutoLoginResult.FAILURE
        }
    }

    // ── [2] PhoneInput ────────────────────────────────────
    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    // ── [3] ResidentNumber ────────────────────────────────
    fun updateResidentFront(value: String) {
        _uiState.update { it.copy(residentFront = value) }
    }

    fun updateResidentBack(value: String) {
        _uiState.update { it.copy(residentBack = value) }
    }

    // ── [4] NameInput ─────────────────────────────────────
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun verifyIdentity(onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val state = _uiState.value
            val request = VerifyIdentityRequest(
                name = state.name,
                birthDate = buildBirthDate(state.residentFront, state.residentBack),
                gender = buildGender(state.residentBack),
                phoneNumber = state.phone
            )
            val response = authRepository.verifyIdentity(request)
            val data = response.data!!
            authRepository.saveUserInfo(data.userId, state.phone)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userId = data.userId,
                    requiresPinSetup = data.requiresPinSetup,
                    requiresConsent = data.requiresConsent
                )
            }
            onSuccess()
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "본인인증에 실패했습니다") }
        }
    }

    // ── [5] PinSetup ──────────────────────────────────────
    fun appendPin(digit: String) {
        _uiState.update {
            if (it.pin.length < 6) it.copy(pin = it.pin + digit) else it
        }
    }

    fun deletePin() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1)) }
    }

    // ── [6] PinConfirm ────────────────────────────────────
    fun appendPinConfirm(digit: String) {
        _uiState.update {
            if (it.pinConfirm.length < 6) it.copy(pinConfirm = it.pinConfirm + digit) else it
        }
    }

    fun deletePinConfirm() {
        _uiState.update { it.copy(pinConfirm = it.pinConfirm.dropLast(1)) }
    }

    fun confirmPin(onSuccess: () -> Unit, onMismatch: () -> Unit, onResetRequired: () -> Unit) {
        val state = _uiState.value
        if (state.pin == state.pinConfirm) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, pinFailCount = 0) }
                try {
                    authRepository.setupPin(state.userId, state.pin)
                    _uiState.update { it.copy(isLoading = false, authComplete = true) }
                    onSuccess()
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "PIN 설정에 실패했습니다") }
                }
            }
        } else {
            val newCount = state.pinFailCount + 1
            if (newCount >= 3) {
                _uiState.update {
                    it.copy(pin = "", pinConfirm = "", pinFailCount = 0,
                        errorMessage = "처음부터 다시 설정해주세요")
                }
                onResetRequired()
            } else {
                _uiState.update {
                    it.copy(pinConfirm = "", pinFailCount = newCount,
                        errorMessage = "비밀번호가 일치하지 않습니다")
                }
                onMismatch()
            }
        }
    }

    fun resetPin() {
        _uiState.update { it.copy(pin = "", pinConfirm = "", pinFailCount = 0, errorMessage = "") }
    }

    // ── [7] Terms (Consents) ──────────────────────────────
    fun loadTerms() {
        val consentTerms = listOf(
            TermItem("SERVICE", "서비스 이용약관", true),
            TermItem("PRIVACY", "개인정보 처리방침", true),
            TermItem("FINANCIAL", "금융정보 제공 동의", true)
        )
        _uiState.update { it.copy(terms = consentTerms) }
    }

    fun toggleTerm(consentType: String) {
        _uiState.update { state ->
            val newSet = if (state.agreedTermIds.contains(consentType))
                state.agreedTermIds - consentType
            else
                state.agreedTermIds + consentType
            val allRequired = state.terms
                .filter { it.required }
                .all { newSet.contains(it.consentType) }
            state.copy(agreedTermIds = newSet, allRequiredTermsAgreed = allRequired)
        }
    }

    fun toggleAllTerms() {
        _uiState.update { state ->
            val allIds = state.terms.map { it.consentType }.toSet()
            val allChecked = state.agreedTermIds.containsAll(allIds)
            if (allChecked) {
                state.copy(agreedTermIds = emptySet(), allRequiredTermsAgreed = false)
            } else {
                state.copy(agreedTermIds = allIds, allRequiredTermsAgreed = true)
            }
        }
    }

    fun submitConsents(onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val consents = _uiState.value.agreedTermIds.map { ConsentItem(it, true) }
            authRepository.submitConsents(consents)
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "약관 동의에 실패했습니다") }
        }
    }

    // ── [8] PinLogin (재방문 사용자) ──────────────────────
    fun appendLoginPin(digit: String) {
        _uiState.update {
            if (it.loginPin.length < 6) it.copy(loginPin = it.loginPin + digit) else it
        }
    }

    fun deleteLoginPin() {
        _uiState.update { it.copy(loginPin = it.loginPin.dropLast(1)) }
    }

    fun loginWithPin(onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
        try {
            val phoneNumber = authRepository.getStoredPhoneNumber()!!
            authRepository.login(phoneNumber, _uiState.value.loginPin)
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, loginPin = "", errorMessage = "PIN이 올바르지 않습니다")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }

    // ── Helpers ───────────────────────────────────────────
    private fun buildBirthDate(residentFront: String, residentBack: String): String {
        val yy = residentFront.substring(0, 2)
        val mm = residentFront.substring(2, 4)
        val dd = residentFront.substring(4, 6)
        val century = when (residentBack) {
            "1", "2" -> "19"
            "3", "4" -> "20"
            else -> "19"
        }
        return "$century$yy-$mm-$dd"
    }

    private fun buildGender(residentBack: String): String {
        return when (residentBack) {
            "1", "3" -> "M"
            "2", "4" -> "F"
            else -> "M"
        }
    }
}
