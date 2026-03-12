package com.ssafy.tax7i.banking.dto;

import com.ssafy.tax7i.banking.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull(message = "계좌 유형은 필수입니다.")
        AccountType accountType,

        String alias
) {
}
