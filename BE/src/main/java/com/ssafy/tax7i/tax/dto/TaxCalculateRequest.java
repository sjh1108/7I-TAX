package com.ssafy.tax7i.tax.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TaxCalculateRequest(
        @NotNull int taxYear,
        Long prepaidTax,
        Map<String, Long> deductions
) {
}
