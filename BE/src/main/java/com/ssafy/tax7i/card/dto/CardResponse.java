package com.ssafy.tax7i.card.dto;

import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;

public record CardResponse(
        Long id,
        String cardName,
        CardType cardType,
        String last4Digits,
        Boolean isDefault,
        String cardExpiryDate,
        String withdrawalDate
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getCardName(),
                card.getCardType(),
                card.getLast4Digits(),
                card.isDefault(),
                card.getCardExpiryDate(),
                card.getWithdrawalDate()
        );
    }
}
