package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.dto.TaxSavingRecommendation;
import com.ssafy.tax7i.tax.dto.TaxSavingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxSavingService {

    private final BookEntryRepository bookEntryRepository;
    private final TaxCalculationEngine taxCalculationEngine;

    public TaxSavingResponse getRecommendations(Long userId, int taxYear) {
        // 1. Aggregate book entries for the year
        LocalDate start = LocalDate.of(taxYear, 1, 1);
        LocalDate end = LocalDate.of(taxYear, 12, 31);
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long totalRevenue = agg.totalIncome();
        long totalExpense = agg.totalExpense();

        // 2. Base deductions
        Map<String, Long> baseDeductions = new HashMap<>();
        baseDeductions.put("기본공제_본인", 1_500_000L);

        // 3. Calculate current tax
        TaxCalculationResult currentTax = taxCalculationEngine.calculate(
                taxYear, totalRevenue, totalExpense, 0, baseDeductions);

        List<TaxSavingRecommendation> recommendations = new ArrayList<>();

        // 4. 노란우산공제
        long noranLimit = totalRevenue <= 40_000_000L ? 5_000_000L : 3_000_000L;
        Map<String, Long> withNoran = new HashMap<>(baseDeductions);
        withNoran.put("노란우산공제", noranLimit);
        TaxCalculationResult withNoranTax = taxCalculationEngine.calculate(
                taxYear, totalRevenue, totalExpense, 0, withNoran);
        long noranSaving = currentTax.determinedTax() - withNoranTax.determinedTax();
        noranSaving += noranSaving / 10; // 지방세 10% 포함 (지방세법 §92)
        recommendations.add(TaxSavingRecommendation.withUsage(
                "DEDUCTION", "노란우산공제",
                "소기업·소상공인 공제부금. 가입 시 연 최대 " + (noranLimit / 10000) + "만원 소득공제.",
                noranLimit, noranSaving, false, 0));

        // 5. 연금저축
        double creditRate = totalRevenue <= 55_000_000L ? 0.15 : 0.132;
        long pensionSaving = (long) Math.floor(6_000_000L * creditRate);
        pensionSaving += pensionSaving / 10; // 지방세 10% 포함 (지방세법 §92)
        recommendations.add(TaxSavingRecommendation.withUsage(
                "CREDIT", "연금저축",
                "연 600만원 한도 세액공제 " + (creditRate * 100) + "%",
                6_000_000L, pensionSaving, false, 0));

        // 6. 접대비 한도 여유
        Long entertainmentUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, "접대비", taxYear);
        long entertainmentUsed = entertainmentUsedRaw != null ? entertainmentUsedRaw : 0L;
        long entertainmentLimit = 12_000_000L;
        if (entertainmentUsed < entertainmentLimit) {
            long entertainRemaining = entertainmentLimit - entertainmentUsed;
            long entertainSaving = (long) Math.floor(entertainRemaining * currentTax.taxRate());
            entertainSaving += entertainSaving / 10; // 지방세 10% 포함 (지방세법 §92)
            recommendations.add(TaxSavingRecommendation.withUsage(
                    "DEDUCTION", "접대비 한도 여유",
                    "올해 접대비 추가 사용 가능 (소득세법 §35)",
                    entertainmentLimit, entertainSaving, false, entertainmentUsed));
        }

        // 7. 사업용카드 매입세액 공제 (부가가치세법 §46, 불공제: §39①1 접대비)
        Long cardBizVatRaw = bookEntryRepository.sumDeductiblePurchaseVat(userId, start, end);
        long cardBizVat = cardBizVatRaw != null ? cardBizVatRaw : 0L;
        if (cardBizVat > 0) {
            recommendations.add(TaxSavingRecommendation.withUsage(
                    "VAT_CREDIT",
                    "사업용카드 매입세액 공제",
                    "사업 관련 카드 결제 시 부가세 환급 (부가가치세법 §46). 접대비는 불공제 (§39①1)",
                    0, cardBizVat, true, cardBizVat));
        }

        // 8. 교육훈련비 경비 활용 (소득세법 §19 필요경비)
        //    ※ 조특법 §104의18(중소기업 교육훈련비 세액공제)은 근로자 대상 → 1인 사업자 본인 미적용
        Long eduUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, "교육훈련비", taxYear);
        Long booksUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, "도서인쇄비", taxYear);
        long eduUsed = (eduUsedRaw != null ? eduUsedRaw : 0L) + (booksUsedRaw != null ? booksUsedRaw : 0L);
        long eduRecommendedLimit = 1_500_000L;
        if (eduUsed < eduRecommendedLimit) {
            long eduRemaining = eduRecommendedLimit - eduUsed;
            long eduSaving = (long) Math.floor(eduRemaining * currentTax.taxRate());
            eduSaving += eduSaving / 10; // 지방세 10% 포함 (지방세법 §92)
            recommendations.add(TaxSavingRecommendation.withUsage(
                    "EXPENSE",
                    "교육훈련비 경비 활용",
                    "인프런, Udemy, 기술서적, 컨퍼런스 경비 처리 가능 (소득세법 §19)",
                    eduRecommendedLimit, eduSaving, false, eduUsed));
        }

        long potentialTotal = recommendations.stream()
                .mapToLong(TaxSavingRecommendation::estimatedSaving).sum();

        // totalSummary (#27)
        long totalMax = recommendations.stream()
                .filter(r -> r.maxAmount() > 0)
                .mapToLong(TaxSavingRecommendation::maxAmount).sum();
        long totalUsed = recommendations.stream()
                .mapToLong(TaxSavingRecommendation::usedAmount).sum();
        long totalRemaining = Math.max(0, totalMax - totalUsed);
        double overallRate = totalMax == 0 ? 0.0
                : Math.round(totalUsed * 1000.0 / totalMax) / 10.0;
        var totalSummary = new TaxSavingResponse.TotalSavingSummary(
                totalMax, totalUsed, totalRemaining, overallRate);

        return new TaxSavingResponse(currentTax.finalTax(), recommendations, potentialTotal, totalSummary);
    }
}
