package com.ssafy.tax7i.taxestimation.service;

import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.taxestimation.dto.TaxEstimationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxEstimationService {

    private final BookEntryRepository bookEntryRepository;

    /**
     * 종합소득세 세율 구간 (2024 기준)
     */
    private static final long[][] TAX_BRACKETS = {
            {14_000_000L, 6},
            {50_000_000L, 15},
            {88_000_000L, 24},
            {150_000_000L, 35},
            {300_000_000L, 38},
            {500_000_000L, 40},
            {1_000_000_000L, 42},
            {Long.MAX_VALUE, 45}
    };

    private static final long[] PROGRESSIVE_DEDUCTION = {
            0L, 1_260_000L, 5_760_000L, 15_440_000L,
            19_940_000L, 25_940_000L, 35_940_000L, 65_940_000L
    };

    public TaxEstimationResponse estimate(Long userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        // DB 집계 쿼리로 전체 데이터 로드 없이 합산
        Object[] rawAgg = bookEntryRepository.aggregateByUserIdAndDateRange(userId, start, end);
        // JPA may return Object[] directly or wrap in Object[][] depending on result
        Object[] agg = rawAgg;
        if (rawAgg != null && rawAgg.length > 0 && rawAgg[0] instanceof Object[]) {
            agg = (Object[]) rawAgg[0];
        }
        long totalIncome = agg != null && agg.length > 0 && agg[0] != null ? ((Number) agg[0]).longValue() : 0L;
        long totalExpense = agg != null && agg.length > 1 && agg[1] != null ? ((Number) agg[1]).longValue() : 0L;
        long totalAssetPurchase = agg != null && agg.length > 2 && agg[2] != null ? ((Number) agg[2]).longValue() : 0L;
        long deductibleExpenses = agg != null && agg.length > 3 && agg[3] != null ? ((Number) agg[3]).longValue() : 0L;

        // 부가세: 매출세액 - 매입세액
        Long salesVatRaw = bookEntryRepository.sumSalesVat(userId, start, end);
        Long purchaseVatRaw = bookEntryRepository.sumDeductiblePurchaseVat(userId, start, end);
        long salesVat = salesVatRaw != null ? salesVatRaw : 0L;
        long purchaseVat = purchaseVatRaw != null ? purchaseVatRaw : 0L;
        long estimatedVat = Math.max(0, salesVat - purchaseVat);

        // 종합소득세: 과세표준 = 수입 - 필요경비
        long taxableIncome = Math.max(0, totalIncome - deductibleExpenses);
        long incomeTax = calculateIncomeTax(taxableIncome);
        String bracket = findBracket(taxableIncome);

        // 지방소득세: 종합소득세의 10%
        long localTax = incomeTax / 10;

        // 절세 효과: 경비 없었을 때 세금 - 현재 세금
        long taxWithoutExpenses = calculateIncomeTax(totalIncome);
        long taxSaving = taxWithoutExpenses - incomeTax;

        return new TaxEstimationResponse(
                year,
                totalIncome, totalExpense, totalAssetPurchase, taxableIncome,
                salesVat, purchaseVat, estimatedVat,
                incomeTax, bracket, localTax,
                deductibleExpenses, taxSaving
        );
    }

    public long calculateIncomeTax(long taxableIncome) {
        if (taxableIncome <= 0) return 0;

        for (int i = 0; i < TAX_BRACKETS.length; i++) {
            if (taxableIncome <= TAX_BRACKETS[i][0]) {
                return taxableIncome * TAX_BRACKETS[i][1] / 100 - PROGRESSIVE_DEDUCTION[i];
            }
        }
        return 0;
    }

    private String findBracket(long taxableIncome) {
        if (taxableIncome <= 14_000_000L) return "6%";
        if (taxableIncome <= 50_000_000L) return "15%";
        if (taxableIncome <= 88_000_000L) return "24%";
        if (taxableIncome <= 150_000_000L) return "35%";
        if (taxableIncome <= 300_000_000L) return "38%";
        if (taxableIncome <= 500_000_000L) return "40%";
        if (taxableIncome <= 1_000_000_000L) return "42%";
        return "45%";
    }
}
