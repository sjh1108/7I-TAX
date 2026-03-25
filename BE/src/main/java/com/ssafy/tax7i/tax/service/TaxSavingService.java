package com.ssafy.tax7i.tax.service;

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
        Object[] rawAgg = bookEntryRepository.aggregateByUserIdAndDateRange(userId, start, end);
        Object[] agg = rawAgg;
        if (rawAgg != null && rawAgg.length > 0 && rawAgg[0] instanceof Object[]) agg = (Object[]) rawAgg[0];
        long totalRevenue = agg != null && agg.length > 0 && agg[0] != null ? ((Number) agg[0]).longValue() : 0L;
        long totalExpense = agg != null && agg.length > 1 && agg[1] != null ? ((Number) agg[1]).longValue() : 0L;

        // 2. Base deductions
        Map<String, Long> baseDeductions = new HashMap<>();
        baseDeductions.put("기본공제_본인", 1_500_000L);

        // 3. Calculate current tax
        TaxCalculationResult currentTax = taxCalculationEngine.calculate(
                taxYear, totalRevenue, totalExpense, 0, baseDeductions);

        List<TaxSavingRecommendation> recommendations = new ArrayList<>();

        // 4. Check 노란우산공제
        long noranLimit = totalRevenue <= 40_000_000L ? 5_000_000L : 3_000_000L;
        Map<String, Long> withNoran = new HashMap<>(baseDeductions);
        withNoran.put("노란우산공제", noranLimit);
        TaxCalculationResult withNoranTax = taxCalculationEngine.calculate(
                taxYear, totalRevenue, totalExpense, 0, withNoran);
        long noranSaving = currentTax.determinedTax() - withNoranTax.determinedTax();
        noranSaving += noranSaving / 10;
        recommendations.add(new TaxSavingRecommendation(
                "DEDUCTION", "노란우산공제",
                "소기업·소상공인 공제부금. 가입 시 연 최대 " + (noranLimit / 10000) + "만원 소득공제.",
                noranLimit, noranSaving, false));

        // 5. Check 연금저축
        double creditRate = totalRevenue <= 55_000_000L ? 0.15 : 0.132;
        long pensionSaving = (long) Math.floor(6_000_000L * creditRate);
        recommendations.add(new TaxSavingRecommendation(
                "CREDIT", "연금저축",
                "연 600만원 한도 세액공제 " + (creditRate * 100) + "%",
                6_000_000L, pensionSaving, false));

        // 6. Check 접대비 한도 여유
        Long entertainmentUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, "접대비", taxYear);
        long entertainmentUsed = entertainmentUsedRaw != null ? entertainmentUsedRaw : 0L;
        long entertainmentLimit = 12_000_000L;
        if (entertainmentUsed < entertainmentLimit) {
            long remaining = entertainmentLimit - entertainmentUsed;
            long entertainSaving = (long) Math.floor(remaining * currentTax.taxRate());
            recommendations.add(new TaxSavingRecommendation(
                    "DEDUCTION", "접대비 한도 여유",
                    "올해 접대비 " + remaining + "원 추가 사용 가능",
                    remaining, entertainSaving, false));
        }

        long potentialTotal = recommendations.stream()
                .mapToLong(TaxSavingRecommendation::estimatedSaving).sum();

        return new TaxSavingResponse(currentTax.finalTax(), recommendations, potentialTotal);
    }
}
