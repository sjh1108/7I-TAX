package com.ssafy.tax7i.banking.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateAccountResponse;
import com.ssafy.tax7i.banking.dto.AccountResponse;
import com.ssafy.tax7i.banking.dto.BalanceResponse;
import com.ssafy.tax7i.banking.dto.CreateAccountRequest;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private static final String DEFAULT_ACCOUNT_TYPE_UNIQUE_NO = "001-1-c7a09e90da4a43";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;

    @Transactional
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        User user = findUser(userId);
        String userKey = getUserKey(user);

        SsafyCreateAccountResponse response = ssafyFinanceClient.createAccount(userKey, DEFAULT_ACCOUNT_TYPE_UNIQUE_NO);

        Account account = Account.builder()
                .user(user)
                .accountType(request.accountType())
                .bankCode(response.rec().bankCode())
                .accountNumber(response.rec().accountNo())
                .alias(request.alias())
                .build();

        accountRepository.save(account);
        return AccountResponse.from(account);
    }

    public List<AccountResponse> getAccounts(Long userId, AccountType accountType) {
        List<Account> accounts;
        if (accountType != null) {
            accounts = accountRepository.findByUserIdAndAccountType(userId, accountType);
        } else {
            accounts = accountRepository.findByUserId(userId);
        }
        return accounts.stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse getAccount(Long userId, Long accountId) {
        Account account = findAccountByUser(accountId, userId);
        User user = account.getUser();
        String userKey = getUserKey(user);

        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, account.getAccountNumber());
        long balance = Long.parseLong(balanceResponse.rec().accountBalance());

        return AccountResponse.of(account, balance);
    }

    public BalanceResponse getBalance(Long userId, Long accountId) {
        Account account = findAccountByUser(accountId, userId);
        User user = account.getUser();
        String userKey = getUserKey(user);

        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, account.getAccountNumber());
        long balance = Long.parseLong(balanceResponse.rec().accountBalance());

        return new BalanceResponse(accountId, balance, LocalDateTime.now());
    }

    public Account findAccountByUser(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String getUserKey(User user) {
        if (user.getSsafyUserKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 금융망 사용자 키가 등록되지 않았습니다.");
        }
        return user.getSsafyUserKey();
    }
}
