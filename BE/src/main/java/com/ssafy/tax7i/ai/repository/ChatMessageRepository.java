package com.ssafy.tax7i.ai.repository;

import com.ssafy.tax7i.ai.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByChatSessionSessionIdOrderByCreatedAtAsc(String sessionId);
}
