package com.ssafy.tax7i.tax.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VatReturnCreateRequest(
        @NotNull int taxYear,
        @NotNull @Min(1) @Max(2) int taxPeriod,
        Long preliminaryPaid
) {
}
