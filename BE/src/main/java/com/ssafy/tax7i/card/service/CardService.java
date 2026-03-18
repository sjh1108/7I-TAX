package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.card.dto.CardCreateRequest;
import com.ssafy.tax7i.card.dto.CardResponse;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public CardResponse createCard(Long userId, CardCreateRequest request) {
        // 카드 유형에 맞는 계좌 찾기 (있으면 연결, 없으면 null)
        AccountType accountType = request.cardType() == CardType.BUSINESS
                ? AccountType.BUSINESS : AccountType.PERSONAL;

        Long accountId = accountRepository.findByUserIdAndAccountType(userId, accountType)
                .stream().findFirst()
                .map(Account::getId)
                .orElse(null);

        Card card = Card.builder()
                .userId(userId)
                .accountId(accountId)
                .cardName(request.cardName())
                .cardType(request.cardType())
                .last4Digits(request.last4Digits())
                .build();

        // 첫 카드는 기본 카드로 설정
        if (cardRepository.findByUserId(userId).isEmpty()) {
            card.setDefault(true);
        }

        cardRepository.save(card);
        return CardResponse.from(card);
    }

    public List<CardResponse> getCards(Long userId) {
        return cardRepository.findByUserId(userId).stream()
                .map(CardResponse::from)
                .toList();
    }

    public CardResponse getCard(Long userId, Long cardId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse setDefault(Long userId, Long cardId) {
        // 기존 기본 카드 해제
        cardRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(c -> c.setDefault(false));

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        card.setDefault(true);
        return CardResponse.from(card);
    }

    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        cardRepository.delete(card);
    }
}
