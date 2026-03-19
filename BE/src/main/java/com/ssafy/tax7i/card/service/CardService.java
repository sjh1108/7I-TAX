package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateAccountResponse;
import com.ssafy.tax7i.card.dto.CardCreateRequest;
import com.ssafy.tax7i.card.dto.CardResponse;
import com.ssafy.tax7i.card.entity.Card;
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

    private static final String ACCOUNT_TYPE_UNIQUE_NO = "001-1-991862afe4ab4a";

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;

    @Transactional
    public CardResponse createCard(Long userId, CardCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String userKey = user.getSsafyUserKey();
        String ssafyAccountNo = null;
        if (userKey != null) {
            SsafyCreateAccountResponse response = ssafyFinanceClient.createAccount(userKey, ACCOUNT_TYPE_UNIQUE_NO);
            ssafyAccountNo = response.rec().accountNo();
        }

        Card card = Card.builder()
                .userId(userId)
                .cardName(request.cardName())
                .cardType(request.cardType())
                .last4Digits(request.last4Digits())
                .ssafyAccountNo(ssafyAccountNo)
                .build();

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
