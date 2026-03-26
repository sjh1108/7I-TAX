package com.ssafy.tax7i.ai;

import com.ssafy.tax7i.ai.client.AiServiceClient;
import com.ssafy.tax7i.ai.client.dto.AiChatHistoryResponse;
import com.ssafy.tax7i.ai.client.dto.AiChatMessage;
import com.ssafy.tax7i.ai.client.dto.AiChatResponse;
import com.ssafy.tax7i.ai.dto.ChatHistoryResponse;
import com.ssafy.tax7i.ai.dto.ChatbotRequest;
import com.ssafy.tax7i.ai.dto.ChatbotResponse;
import com.ssafy.tax7i.ai.repository.ChatMessageRepository;
import com.ssafy.tax7i.ai.repository.ChatSessionRepository;
import com.ssafy.tax7i.ai.service.ChatbotService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private AiServiceClient aiClient;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    @DisplayName("sendMessage 성공 시 정상 응답 매핑")
    void sendMessage_성공() {
        // given
        ChatbotRequest request = new ChatbotRequest("접대비 한도가 얼마인가요?", "session-1");
        AiChatResponse aiResponse = new AiChatResponse(
                "접대비 연간 기본 한도는 1,200만원입니다.", "local_model", "session-1");
        given(aiClient.chat(any())).willReturn(aiResponse);

        // when
        ChatbotResponse response = chatbotService.sendMessage(1L, request);

        // then
        assertThat(response.answer()).isEqualTo("접대비 연간 기본 한도는 1,200만원입니다.");
        assertThat(response.model()).isEqualTo("local_model");
        assertThat(response.sessionId()).isEqualTo("session-1");
    }

    @Test
    @DisplayName("sendMessage AI 서비스 장애 시 BusinessException 전파")
    void sendMessage_AI_장애() {
        // given
        ChatbotRequest request = new ChatbotRequest("테스트 메시지", "session-1");
        given(aiClient.chat(any()))
                .willThrow(new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE));

        // when & then
        assertThatThrownBy(() -> chatbotService.sendMessage(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("getHistory 성공 시 메시지 리스트 매핑")
    void getHistory_성공() {
        // given
        List<AiChatMessage> aiMessages = List.of(
                new AiChatMessage("user", "부가세 신고 기간은?"),
                new AiChatMessage("assistant", "부가세 신고 기간은 1월, 7월입니다.")
        );
        AiChatHistoryResponse aiResponse = new AiChatHistoryResponse("session-1", aiMessages, 2);
        given(aiClient.getChatHistory("session-1")).willReturn(aiResponse);

        // when
        ChatHistoryResponse response = chatbotService.getHistory("session-1");

        // then
        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(response.messageCount()).isEqualTo(2);
        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).role()).isEqualTo("user");
        assertThat(response.messages().get(1).content()).contains("부가세");
    }

    @Test
    @DisplayName("getHistory AI 서비스 장애 시 BusinessException 전파")
    void getHistory_AI_장애() {
        // given
        given(aiClient.getChatHistory("session-1"))
                .willThrow(new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE));

        // when & then
        assertThatThrownBy(() -> chatbotService.getHistory("session-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE));
    }
}
