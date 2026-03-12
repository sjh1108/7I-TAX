package com.ssafy.tax7i.banking.dto;

import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountStatus;
import com.ssafy.tax7i.banking.entity.AccountType;

import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        AccountType accountType,
        String bankCode,
        String accountNumber,
        String alias,
        Long balance,
        AccountStatus status,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountType(),
                account.getBankCode(),
                maskAccountNumber(account.getAccountNumber()),
                account.getAlias(),
                null,
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public static AccountResponse of(Account account, Long balance) {
        return new AccountResponse(
                account.getId(),
                account.getAccountType(),
                account.getBankCode(),
                maskAccountNumber(account.getAccountNumber()),
                account.getAlias(),
                balance,
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        int visibleCount = 4;
        String masked = "*".repeat(accountNumber.length() - visibleCount);
        return masked + accountNumber.substring(accountNumber.length() - visibleCount);
    }
}
