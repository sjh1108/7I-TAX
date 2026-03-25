package com.ssafy.tax7i.taxestimation.service;

import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.taxestimation.dto.MonthlyTaxEstimationResponse;
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

        // 부가세 반기별 (부가가치세법 §5, §49)
        TaxEstimationResponse.VatByPeriod vatByPeriod = buildVatByPeriod(userId, year);

        // 연간 총 예상 세금
        long totalAnnualTax = vatByPeriod.totalVat() + incomeTax + localTax;

        // 세율 구간 상세 (소득세법 §55)
        TaxEstimationResponse.BracketDetail bracketDetail = buildBracketDetail(taxableIncome);

        return new TaxEstimationResponse(
                year,
                totalIncome, totalExpense, totalAssetPurchase, taxableIncome,
                salesVat, purchaseVat, estimatedVat,
                incomeTax, bracket, localTax,
                deductibleExpenses, taxSaving,
                vatByPeriod, totalAnnualTax, bracketDetail
        );
    }

    /**
     * 월별 세금 추정
     * - 부가세: 해당 과세기간(1기/2기) 누적 매출세액-매입세액 (부가가치세법 §49)
     * - 종소세: 현재까지 수입/비용을 연환산 (소득세법 §70)
     * - 지방세: 종소세 결정세액 × 10% (지방세법 §92, §95)
     */
    public MonthlyTaxEstimationResponse estimateMonthly(Long userId, int year, int month) {
        // 부가세: 해당 과세기간 누적 (1기: 1~6월, 2기: 7~12월)
        boolean isFirstHalf = month <= 6;
        LocalDate vatStart = isFirstHalf ? LocalDate.of(year, 1, 1) : LocalDate.of(year, 7, 1);
        LocalDate vatEnd = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());
        String vatPeriod = isFirstHalf ? "1기 (1~6월)" : "2기 (7~12월)";
        String vatDueDate = isFirstHalf ? year + "-07-25" : (year + 1) + "-01-25";

        Long salesVatRaw = bookEntryRepository.sumSalesVat(userId, vatStart, vatEnd);
        Long purchaseVatRaw = bookEntryRepository.sumDeductiblePurchaseVat(userId, vatStart, vatEnd);
        long salesTax = salesVatRaw != null ? salesVatRaw : 0L;
        long purchaseTax = purchaseVatRaw != null ? purchaseVatRaw : 0L;
        long vatPayable = Math.max(0, salesTax - purchaseTax);

        var vatEstimation = new MonthlyTaxEstimationResponse.VatEstimation(
                vatPeriod, salesTax, purchaseTax, vatPayable, vatDueDate);

        // 종소세: 현재까지 수입/비용을 연환산
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate monthEnd = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());

        Object[] rawAgg = bookEntryRepository.aggregateByUserIdAndDateRange(userId, yearStart, monthEnd);
        Object[] agg = rawAgg;
        if (rawAgg != null && rawAgg.length > 0 && rawAgg[0] instanceof Object[]) agg = (Object[]) rawAgg[0];
        long currentIncome = agg != null && agg.length > 0 && agg[0] != null ? ((Number) agg[0]).longValue() : 0L;
        long currentExpense = agg != null && agg.length > 3 && agg[3] != null ? ((Number) agg[3]).longValue() : 0L;

        // 연환산 = 현재 누적 × (12 / 경과월수)
        long projectedIncome = month > 0 ? currentIncome * 12 / month : 0;
        long projectedExpense = month > 0 ? currentExpense * 12 / month : 0;
        long projectedTaxable = Math.max(0, projectedIncome - projectedExpense);
        long incomeTax = calculateIncomeTax(projectedTaxable);
        String incomeTaxDueDate = (year + 1) + "-05-31";

        var incomeTaxEstimation = new MonthlyTaxEstimationResponse.IncomeTaxEstimation(
                currentIncome, currentExpense, projectedIncome, projectedExpense,
                incomeTax, incomeTaxDueDate);

        // 지방세: 종소세 결정세액 × 10% (지방세법 §92)
        long localTax = (long) Math.floor(incomeTax * 0.1);
        var localTaxEstimation = new MonthlyTaxEstimationResponse.LocalTaxEstimation(
                localTax, incomeTaxDueDate);

        long totalTax = vatPayable + incomeTax + localTax;

        return new MonthlyTaxEstimationResponse(
                year, month, vatEstimation, incomeTaxEstimation, localTaxEstimation, totalTax);
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

    /** 부가세 반기별 계산 — 부가가치세법 §5(과세기간), §49(확정신고), §39①1(접대비 불공제) */
    private TaxEstimationResponse.VatByPeriod buildVatByPeriod(Long userId, int year) {
        var p1 = buildVatPeriodDetail(userId, year, 1, 6,
                "1기 (1~6월)", year + "-07-25");
        var p2 = buildVatPeriodDetail(userId, year, 7, 12,
                "2기 (7~12월)", (year + 1) + "-01-25");
        return new TaxEstimationResponse.VatByPeriod(p1, p2, p1.estimatedPayable() + p2.estimatedPayable());
    }

    private TaxEstimationResponse.VatPeriodDetail buildVatPeriodDetail(
            Long userId, int year, int startMonth, int endMonth, String label, String dueDate) {
        LocalDate s = LocalDate.of(year, startMonth, 1);
        LocalDate e = LocalDate.of(year, endMonth, LocalDate.of(year, endMonth, 1).lengthOfMonth());
        Long svRaw = bookEntryRepository.sumSalesVat(userId, s, e);
        Long pvRaw = bookEntryRepository.sumDeductiblePurchaseVat(userId, s, e);
        long sv = svRaw != null ? svRaw : 0L;
        long pv = pvRaw != null ? pvRaw : 0L;
        return new TaxEstimationResponse.VatPeriodDetail(label, sv, pv, Math.max(0, sv - pv), dueDate);
    }

    /** 세율 구간 상세 — 소득세법 §55 */
    private TaxEstimationResponse.BracketDetail buildBracketDetail(long taxableIncome) {
        long prevMax = 0;
        for (int i = 0; i < TAX_BRACKETS.length; i++) {
            if (taxableIncome <= TAX_BRACKETS[i][0]) {
                long bracketMax = TAX_BRACKETS[i][0];
                String currentRate = TAX_BRACKETS[i][1] + "%";
                Long toNext = (bracketMax == Long.MAX_VALUE) ? null : bracketMax - taxableIncome;
                String nextRate = (i + 1 < TAX_BRACKETS.length && bracketMax != Long.MAX_VALUE)
                        ? TAX_BRACKETS[i + 1][1] + "%" : null;
                return new TaxEstimationResponse.BracketDetail(
                        currentRate, prevMax, bracketMax, taxableIncome, toNext, nextRate);
            }
            prevMax = TAX_BRACKETS[i][0];
        }
        // 최고 구간
        return new TaxEstimationResponse.BracketDetail(
                "45%", 1_000_000_000L, Long.MAX_VALUE, taxableIncome, null, null);
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
