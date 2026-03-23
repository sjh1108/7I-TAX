package com.ssafy.tax7i.card.dto;

import com.ssafy.tax7i.card.entity.CardTransaction;
import com.ssafy.tax7i.card.entity.CardTransactionType;

import java.time.LocalDateTime;

public record CardTransactionResponse(
        Long id,
        CardTransactionType transactionType,
        Long amount,
        Long balanceAfter,
        String description,
        LocalDateTime createdAt
) {
    public static CardTransactionResponse from(CardTransaction tx) {
        return new CardTransactionResponse(
                tx.getId(),
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
