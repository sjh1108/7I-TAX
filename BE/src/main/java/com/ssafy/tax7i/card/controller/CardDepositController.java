package com.ssafy.tax7i.card.controller;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateAccountResponse;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountStatus;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardDepositController {

    private static final String ACCOUNT_TYPE_UNIQUE_NO = "001-1-991862afe4ab4a";

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;

    @PostMapping("/{cardId}/deposit")
    @Transactional
    public ResponseEntity<SuccessResponse<Map<String, Object>>> deposit(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @RequestBody Map<String, Long> request) {

        Long amount = request.get("amount");
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "입금 금액은 0보다 커야 합니다.");
        }

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));

        Account account = getOrCreateAccount(card, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String userKey = user.getSsafyUserKey();
        if (userKey == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "금융망 사용자 키가 없습니다.");
        }

        ssafyFinanceClient.deposit(userKey, account.getAccountNumber(), amount, "카드 충전");

        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, account.getAccountNumber());
        long balance = Long.parseLong(balanceResponse.rec().accountBalance());

        return ResponseEntity.ok(SuccessResponse.of(Map.of(
                "cardId", card.getId(),
                "cardName", card.getCardName(),
                "depositAmount", amount,
                "balance", balance
        )));
    }

    @GetMapping("/{cardId}/balance")
    @Transactional
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getBalance(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));

        Account account = getOrCreateAccount(card, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String userKey = user.getSsafyUserKey();
        if (userKey == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "금융망 사용자 키가 없습니다.");
        }

        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, account.getAccountNumber());
        long balance = Long.parseLong(balanceResponse.rec().accountBalance());

        return ResponseEntity.ok(SuccessResponse.of(Map.of(
                "cardId", card.getId(),
                "cardName", card.getCardName(),
                "balance", balance
        )));
    }

    private Account getOrCreateAccount(Card card, Long userId) {
        if (card.getAccountId() != null) {
            return accountRepository.findById(card.getAccountId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        }

        // 계좌가 없으면 카드용 계좌 자동 개설
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String userKey = user.getSsafyUserKey();
        if (userKey == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "금융망 사용자 키가 없습니다.");
        }

        SsafyCreateAccountResponse response = ssafyFinanceClient.createAccount(userKey, ACCOUNT_TYPE_UNIQUE_NO);

        AccountType accountType = card.getCardType() == CardType.BUSINESS
                ? AccountType.BUSINESS : AccountType.PERSONAL;

        Account account = Account.builder()
                .user(user)
                .accountType(accountType)
                .bankCode(response.rec().bankCode())
                .accountNumber(response.rec().accountNo())
                .alias(card.getCardName() + " 연결계좌")
                .build();
        accountRepository.save(account);

        card.setAccountId(account.getId());
        cardRepository.save(card);

        return account;
    }
}
