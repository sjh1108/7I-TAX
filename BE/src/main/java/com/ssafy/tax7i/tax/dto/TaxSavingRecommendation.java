package com.ssafy.tax7i.tax.dto;

public record TaxSavingRecommendation(
        String type,
        String name,
        String description,
        long maxAmount,
        long estimatedSaving,
        boolean isApplied
) {
}
