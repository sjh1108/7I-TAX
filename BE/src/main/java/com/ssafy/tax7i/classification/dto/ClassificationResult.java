package com.ssafy.tax7i.classification.dto;

public record ClassificationResult(
        Confidence confidence,
        String taxCategory,
        String vatDeductible,
        String legalBasis,
        String remark,
        EntertainmentLimitInfo entertainmentLimit // 접대비인 경우에만
) {

    public enum Confidence {
        CONFIRMED,          // Tier A: MCC만으로 확정
        RECOMMENDED,        // Tier B: 조건 매칭으로 추천 (금액/키워드)
        NEEDS_CONFIRMATION  // Tier B: 사용자 확인 필요
    }

    public record EntertainmentLimitInfo(
            long annualLimit,
            long usedAmount,
            long remainingAmount,
            boolean isOverLimit
    ) {
    }

    public static ClassificationResult confirmed(String taxCategory, String vatDeductible,
                                                  String legalBasis, String remark) {
        return new ClassificationResult(
                Confidence.CONFIRMED, taxCategory, vatDeductible, legalBasis, remark, null);
    }

    public static ClassificationResult recommended(String taxCategory, String vatDeductible,
                                                    String legalBasis, String remark) {
        return new ClassificationResult(
                Confidence.RECOMMENDED, taxCategory, vatDeductible, legalBasis, remark, null);
    }

    public static ClassificationResult needsConfirmation(String taxCategory, String vatDeductible,
                                                          String legalBasis, String remark) {
        return new ClassificationResult(
                Confidence.NEEDS_CONFIRMATION, taxCategory, vatDeductible, legalBasis, remark, null);
    }

    public ClassificationResult withEntertainmentLimit(EntertainmentLimitInfo limit) {
        return new ClassificationResult(confidence, taxCategory, vatDeductible, legalBasis, remark, limit);
    }
}
