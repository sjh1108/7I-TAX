package com.ssafy.tax7i.ai.service;

import com.ssafy.tax7i.ai.client.AiServiceClient;
import com.ssafy.tax7i.ai.client.dto.AiChatHistoryResponse;
import com.ssafy.tax7i.ai.client.dto.AiChatRequest;
import com.ssafy.tax7i.ai.client.dto.AiChatResponse;
import com.ssafy.tax7i.ai.dto.ChatHistoryResponse;
import com.ssafy.tax7i.ai.dto.ChatMessage;
import com.ssafy.tax7i.ai.dto.ChatbotRequest;
import com.ssafy.tax7i.ai.dto.ChatbotResponse;
import com.ssafy.tax7i.ai.entity.ChatMessageEntity;
import com.ssafy.tax7i.ai.entity.ChatSession;
import com.ssafy.tax7i.ai.repository.ChatMessageRepository;
import com.ssafy.tax7i.ai.repository.ChatSessionRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final AiServiceClient aiClient;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatbotResponse sendMessage(Long userId, ChatbotRequest request) {
        try {
            AiChatRequest aiRequest = new AiChatRequest(
                    request.message(),
                    request.sessionId(),
                    String.valueOf(userId)
            );
            AiChatResponse aiResponse = aiClient.chat(aiRequest);

            // DB에 대화 이력 저장
            persistConversation(userId, aiResponse.sessionId(),
                    request.message(), aiResponse.answer());

            return new ChatbotResponse(
                    aiResponse.answer(),
                    aiResponse.model(),
                    aiResponse.sessionId()
            );
        } catch (BusinessException e) {
            log.error("AI 챗봇 메시지 전송 실패: userId={}, sessionId={}, error={}",
                    userId, request.sessionId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("AI 챗봇 메시지 전송 중 예상치 못한 오류: userId={}, sessionId={}",
                    userId, request.sessionId(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "AI 챗봇 서비스에 일시적인 문제가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(String sessionId) {
        try {
            AiChatHistoryResponse aiResponse = aiClient.getChatHistory(sessionId);
            List<ChatMessage> messages = aiResponse.messages().stream()
                    .map(m -> new ChatMessage(m.role(), m.content()))
                    .toList();
            return new ChatHistoryResponse(
                    aiResponse.sessionId(),
                    messages,
                    aiResponse.messageCount()
            );
        } catch (BusinessException e) {
            log.error("AI 챗봇 이력 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("AI 챗봇 이력 조회 중 예상치 못한 오류: sessionId={}", sessionId, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "AI 챗봇 이력 조회에 일시적인 문제가 발생했습니다.");
        }
    }

    private void persistConversation(Long userId, String sessionId,
                                     String userMessage, String assistantAnswer) {
        try {
            ChatSession session = chatSessionRepository.findBySessionId(sessionId)
                    .orElseGet(() -> chatSessionRepository.save(
                            ChatSession.builder()
                                    .sessionId(sessionId)
                                    .userId(userId)
                                    .build()
                    ));

            chatMessageRepository.save(ChatMessageEntity.builder()
                    .chatSession(session)
                    .role("user")
                    .content(userMessage)
                    .build());

            chatMessageRepository.save(ChatMessageEntity.builder()
                    .chatSession(session)
                    .role("assistant")
                    .content(assistantAnswer)
                    .build());

            session.incrementMessageCount(2);

            log.debug("챗봇 대화 저장: sessionId={}, userId={}, messageCount={}",
                    sessionId, userId, session.getMessageCount());
        } catch (Exception e) {
            // DB 저장 실패가 응답에 영향을 주지 않도록 로깅만 수행
            log.warn("챗봇 대화 DB 저장 실패 (응답은 정상): sessionId={}, error={}",
                    sessionId, e.getMessage());
        }
    }
}
