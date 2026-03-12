package com.ssafy.tax7i.banking.dto;

import java.time.LocalDateTime;

public record BalanceResponse(
        Long accountId,
        Long balance,
        LocalDateTime asOf
) {
}
