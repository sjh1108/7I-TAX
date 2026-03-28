package com.ssafy.seveniTax.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.chatbot.ChatbotRequest
import com.ssafy.seveniTax.data.repository.ChatbotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val time: String = currentTime(),
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sessionId: String? = null,
    val isSending: Boolean = false
)

private fun currentTime(): String {
    val now = java.util.Calendar.getInstance()
    val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = now.get(java.util.Calendar.MINUTE)
    val ampm = if (hour < 12) "오전" else "오후"
    val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
    return "$ampm ${h}:${minute.toString().padStart(2, '0')}"
}

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatbotVM"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // 초기 인사 메시지
        _uiState.value = ChatUiState(
            messages = listOf(
                ChatMessage(
                    text = "안녕하세요!\n세무 관련 궁금한 점이 있으시면 편하게 물어보세요.",
                    isUser = false
                )
            )
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isSending) return

        val userMsg = ChatMessage(text = text, isUser = true)
        val loadingMsg = ChatMessage(text = "", isUser = false, isLoading = true)

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + loadingMsg,
            isSending = true
        )

        viewModelScope.launch {
            try {
                val request = ChatbotRequest(
                    message = text,
                    sessionId = _uiState.value.sessionId
                )
                Log.d(TAG, "▶ sendMessage: $text, session=${request.sessionId}")

                val response = chatbotRepository.sendMessage(request)
                val code = response.code()
                val body = response.body()
                val errorBody = if (!response.isSuccessful) response.errorBody()?.string() else null

                Log.d(TAG, "  HTTP $code | success=${response.isSuccessful}")
                Log.d(TAG, "  body=$body")
                if (errorBody != null) Log.e(TAG, "  errorBody=$errorBody")

                // 로딩 메시지 제거
                val msgs = _uiState.value.messages.filter { !it.isLoading }

                if (response.isSuccessful && body?.data != null) {
                    val data = body.data
                    Log.d(TAG, "  응답: ${data.answer.take(100)}...")
                    Log.d(TAG, "  sessionId=${data.sessionId}, model=${data.model}")
                    _uiState.value = _uiState.value.copy(
                        messages = msgs + ChatMessage(text = data.answer, isUser = false),
                        sessionId = data.sessionId,
                        isSending = false
                    )
                } else {
                    val errMsg = body?.message?.ifEmpty { null }
                        ?: errorBody?.take(200)
                        ?: "응답 실패 (HTTP $code)"
                    Log.e(TAG, "  실패: $errMsg")
                    _uiState.value = _uiState.value.copy(
                        messages = msgs + ChatMessage(
                            text = errMsg,
                            isUser = false,
                            isError = true
                        ),
                        isSending = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "  에러: ${e.message}", e)
                val msgs = _uiState.value.messages.filter { !it.isLoading }
                _uiState.value = _uiState.value.copy(
                    messages = msgs + ChatMessage(
                        text = "네트워크 오류가 발생했어요. 다시 시도해주세요.",
                        isUser = false,
                        isError = true
                    ),
                    isSending = false
                )
            }
        }
    }

    fun retry() {
        val msgs = _uiState.value.messages
        // 마지막 에러 메시지 제거 후, 마지막 유저 메시지 재전송
        val withoutError = msgs.filter { !it.isError }
        val lastUserMsg = withoutError.lastOrNull { it.isUser }
        if (lastUserMsg != null) {
            _uiState.value = _uiState.value.copy(messages = withoutError)
            sendMessage(lastUserMsg.text)
        }
    }

    fun newSession() {
        _uiState.value = ChatUiState(
            messages = listOf(
                ChatMessage(
                    text = "새 대화를 시작합니다.\n세무 관련 궁금한 점을 물어보세요!",
                    isUser = false
                )
            )
        )
    }
}
