package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateAccountResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyDepositResponse;
import com.ssafy.tax7i.card.dto.*;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardTransaction;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.card.repository.CardTransactionRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;

    @Transactional
    public CardResponse createCard(Long userId, CreateCardRequest request) {
        User user = getUser(userId);
        String userKey = getUserKey(user);

        SsafyCreateAccountResponse accountResponse = ssafyFinanceClient.createAccount(userKey, request.accountTypeUniqueNo());
        String accountNo = accountResponse.rec().accountNo();
        String last4 = accountNo.substring(accountNo.length() - 4);

        Card card = Card.builder()
                .user(user)
                .cardName(request.cardName())
                .cardType(request.cardType())
                .last4Digits(last4)
                .ssafyAccountNo(accountNo)
                .build();

        cardRepository.save(card);
        return CardResponse.from(card);
    }

    public List<CardResponse> getCards(Long userId) {
        return cardRepository.findByUser_Id(userId).stream()
                .map(CardResponse::from)
                .toList();
    }

    public CardResponse getCard(Long userId, Long cardId) {
        Card card = getCardWithOwnership(userId, cardId);
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse setDefaultCard(Long userId, Long cardId) {
        Card card = getCardWithOwnership(userId, cardId);
        cardRepository.findByUser_IdAndIsDefaultTrue(userId)
                .ifPresent(Card::unmarkDefault);
        card.markDefault();
        return CardResponse.from(card);
    }

    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        Card card = getCardWithOwnership(userId, cardId);
        cardRepository.delete(card);
    }

    @Transactional
    public CardDepositResponse deposit(Long userId, Long cardId, CardDepositRequest request) {
        User user = getUser(userId);
        Card card = getCardWithOwnership(userId, cardId);
        String userKey = getUserKey(user);

        ssafyFinanceClient.withdraw(userKey, request.sourceAccountNo(), request.amount(), "카드 충전");
        SsafyDepositResponse depositResponse = ssafyFinanceClient.deposit(userKey, card.getSsafyAccountNo(), request.amount(), "카드 충전");

        long balanceAfter = Long.parseLong(depositResponse.rec().accountBalance());

        CardTransaction transaction = CardTransaction.builder()
                .card(card)
                .transactionType(CardTransactionType.CHARGE)
                .amount(request.amount())
                .balanceAfter(balanceAfter)
                .description("카드 충전")
                .build();
        cardTransactionRepository.save(transaction);

        return new CardDepositResponse(card.getId(), request.amount(), balanceAfter);
    }

    public CardBalanceResponse getBalance(Long userId, Long cardId) {
        User user = getUser(userId);
        Card card = getCardWithOwnership(userId, cardId);
        String userKey = getUserKey(user);

        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, card.getSsafyAccountNo());
        long balance = Long.parseLong(balanceResponse.rec().accountBalance());

        return new CardBalanceResponse(card.getId(), card.getCardName(), balance);
    }

    public Page<CardTransactionResponse> getTransactions(Long userId, Long cardId, CardTransactionType type, Pageable pageable) {
        getCardWithOwnership(userId, cardId);
        if (type != null) {
            return cardTransactionRepository.findByCard_IdAndTransactionTypeOrderByCreatedAtDesc(cardId, type, pageable)
                    .map(CardTransactionResponse::from);
        }
        return cardTransactionRepository.findByCard_IdOrderByCreatedAtDesc(cardId, pageable)
                .map(CardTransactionResponse::from);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Card getCardWithOwnership(Long userId, Long cardId) {
        return cardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
    }

    private String getUserKey(User user) {
        if (user.getSsafyUserKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 금융망 사용자 키가 등록되지 않았습니다.");
        }
        return user.getSsafyUserKey();
    }
}
