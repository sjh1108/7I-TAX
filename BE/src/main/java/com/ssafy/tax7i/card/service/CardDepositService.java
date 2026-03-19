package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateAccountResponse;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CardDepositService {

    private static final String ACCOUNT_TYPE_UNIQUE_NO = "001-1-991862afe4ab4a";

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;

    @Transactional
    public Map<String, Object> deposit(Long userId, Long cardId, Long amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "입금 금액은 0보다 커야 합니다.");
        }

        Card card = findCard(cardId, userId);
        String userKey = getUserKey(userId);

        ensureAccountNo(card, userKey);

        ssafyFinanceClient.deposit(userKey, card.getSsafyAccountNo(), amount, "카드 충전");

        long balance = getCardBalance(userKey, card.getSsafyAccountNo());

        return Map.of(
                "cardId", card.getId(),
                "cardName", card.getCardName(),
                "depositAmount", amount,
                "balance", balance
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBalance(Long userId, Long cardId) {
        Card card = findCard(cardId, userId);
        String userKey = getUserKey(userId);

        if (card.getSsafyAccountNo() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "카드에 연결된 계좌가 없습니다.");
        }

        long balance = getCardBalance(userKey, card.getSsafyAccountNo());

        return Map.of(
                "cardId", card.getId(),
                "cardName", card.getCardName(),
                "balance", balance
        );
    }

    private Card findCard(Long cardId, Long userId) {
        return cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
    }

    private String getUserKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String userKey = user.getSsafyUserKey();
        if (userKey == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "금융망 사용자 키가 없습니다.");
        }
        return userKey;
    }

    private long getCardBalance(String userKey, String accountNo) {
        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, accountNo);
        return Long.parseLong(balanceResponse.rec().accountBalance());
    }

    private void ensureAccountNo(Card card, String userKey) {
        if (card.getSsafyAccountNo() != null) return;
        SsafyCreateAccountResponse response = ssafyFinanceClient.createAccount(userKey, ACCOUNT_TYPE_UNIQUE_NO);
        card.setSsafyAccountNo(response.rec().accountNo());
    }
}
