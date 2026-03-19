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
    // 본인인증 입력
    val phone: String = "",
    val residentFront: String = "",   // 생년월일 6자리
    val residentBack: String = "",    // 뒷자리 1자리 (성별)
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
    val pinFailCount: Int = 0,
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
        // TODO: SecureStorage에서 accessToken + pinHash 확인
        // hasToken && hasPin → SUCCESS (PIN 입력으로)
        // else → FAILURE (본인인증 플로우로)
        kotlinx.coroutines.delay(1500)
        _autoLoginResult.value = AutoLoginResult.FAILURE
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

    // ── [4] CarrierSelect ─────────────────────────────────
    fun updateCarrier(carrier: Carrier) {
        _uiState.update { it.copy(carrier = carrier) }
    }

    // ── [5] NameInput ─────────────────────────────────────
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    // ── [6] Terms ─────────────────────────────────────────
    fun loadTerms() = viewModelScope.launch {
        // TODO: authRepository.getTerms()로 서버에서 약관 목록 로드
        // 임시: 하드코딩 약관 목록
        val mockTerms = listOf(
            TermItem("term_service", "기택스 회원 약관 및 동의사항", "1.0", true, "", ""),
            TermItem("term_privacy", "본인 확인 서비스 약관 및 동의사항", "1.0", true, "", ""),
            TermItem("term_marketing", "기택스 세무 서비스 제공 동의", "1.0", false, "", "")
        )
        _uiState.update { it.copy(terms = mockTerms) }
    }

    fun toggleTerm(termId: String) {
        _uiState.update { state ->
            val newSet = if (state.agreedTermIds.contains(termId))
                state.agreedTermIds - termId
            else
                state.agreedTermIds + termId
            val allRequired = state.terms
                .filter { it.required }
                .all { newSet.contains(it.id) }
            state.copy(agreedTermIds = newSet, allRequiredTermsAgreed = allRequired)
        }
    }

    fun toggleAllTerms() {
        _uiState.update { state ->
            val allIds = state.terms.map { it.id }.toSet()
            val allChecked = state.agreedTermIds.containsAll(allIds)
            if (allChecked) {
                state.copy(agreedTermIds = emptySet(), allRequiredTermsAgreed = false)
            } else {
                val allRequired = state.terms.filter { it.required }.all { allIds.contains(it.id) }
                state.copy(agreedTermIds = allIds, allRequiredTermsAgreed = allRequired)
            }
        }
    }

    fun submitTerms() = viewModelScope.launch {
        // TODO: authRepository.agreeTerms() 서버 호출
        // 임시: 성공 처리
        _uiState.update { it.copy(isLoading = false) }
    }

    // ── [7] SmsVerify ─────────────────────────────────────
    fun updateSmsCode(code: String) {
        _uiState.update { it.copy(smsCode = code) }
    }

    fun requestSmsVerification() = viewModelScope.launch {
        // TODO: authRepository.requestPhoneVerify(phone, carrier, name, birthDate, gender)
        // 임시: SMS 발송된 것으로 처리
        _uiState.update { it.copy(isLoading = false, errorMessage = "") }
    }

    fun verifySms(onSuccess: () -> Unit) = viewModelScope.launch {
        // TODO: authRepository.confirmPhoneVerify() → 토큰 저장 → smsVerified
        // 임시: 아무 코드나 입력하면 성공
        _uiState.update { it.copy(smsVerified = true) }
        onSuccess()
    }

    // ── [8] PinSetup ──────────────────────────────────────
    fun appendPin(digit: String) {
        _uiState.update {
            if (it.pin.length < 6) it.copy(pin = it.pin + digit) else it
        }
    }

    fun deletePin() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1)) }
    }

    // ── [9] PinConfirm ────────────────────────────────────
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
            _uiState.update { it.copy(authComplete = true, pinFailCount = 0) }
            onSuccess()
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

    fun resetPinConfirm() {
        _uiState.update { it.copy(pinConfirm = "", errorMessage = "비밀번호가 일치하지 않습니다") }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
