package com.ssafy.tax7i.taxestimation.dto;

public record TaxEstimationResponse(
        // 기간
        int year,

        // 매출·비용 집계
        long totalIncome,
        long totalExpense,
        long totalAssetPurchase,
        long taxableIncome,

        // 부가세 예상
        long salesVat,
        long purchaseVat,
        long estimatedVat,

        // 종합소득세 예상
        long estimatedIncomeTax,
        String incomeTaxBracket,
        long estimatedLocalTax,

        // 절세 효과
        long deductibleExpenses,
        long taxSavingFromExpenses
) {
}
