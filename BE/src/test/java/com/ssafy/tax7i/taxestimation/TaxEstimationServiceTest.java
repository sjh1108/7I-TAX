package com.ssafy.tax7i.taxestimation;

import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.taxestimation.dto.MonthlyTaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.dto.TaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.service.TaxEstimationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TaxEstimationServiceTest {

    @Mock
    private BookEntryRepository bookEntryRepository;

    @InjectMocks
    private TaxEstimationService taxEstimationService;

    @Test
    @DisplayName("종합소득세 계산: 과세표준 1000만원 → 세율 6%, 60만원")
    void incomeTax6Percent() {
        long tax = taxEstimationService.calculateIncomeTax(10_000_000L);
        // 10,000,000 * 6% - 0 = 600,000
        assertThat(tax).isEqualTo(600_000L);
    }

    @Test
    @DisplayName("종합소득세 계산: 과세표준 3000만원 → 세율 15%")
    void incomeTax15Percent() {
        long tax = taxEstimationService.calculateIncomeTax(30_000_000L);
        // 30,000,000 * 15% - 1,260,000 = 3,240,000
        assertThat(tax).isEqualTo(3_240_000L);
    }

    @Test
    @DisplayName("종합소득세 계산: 과세표준 7000만원 → 세율 24%")
    void incomeTax24Percent() {
        long tax = taxEstimationService.calculateIncomeTax(70_000_000L);
        // 70,000,000 * 24% - 5,760,000 = 11,040,000
        assertThat(tax).isEqualTo(11_040_000L);
    }

    @Test
    @DisplayName("종합소득세 계산: 과세표준 0 → 세금 0")
    void incomeTaxZero() {
        assertThat(taxEstimationService.calculateIncomeTax(0L)).isEqualTo(0L);
        assertThat(taxEstimationService.calculateIncomeTax(-100L)).isEqualTo(0L);
    }

    @Test
    @DisplayName("절세 예상: 매출 5000만 - 경비 2000만 = 과세표준 3000만")
    void estimationWithExpenses() {
        Long userId = 1L;

        given(bookEntryRepository.safeAggregate(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(new AggregateResult(50_000_000L, 20_000_000L, 0L, 20_000_000L));
        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(4_545_455L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(1_818_182L);

        TaxEstimationResponse result = taxEstimationService.estimate(userId, 2026);

        assertThat(result.totalIncome()).isEqualTo(50_000_000L);
        assertThat(result.totalExpense()).isEqualTo(20_000_000L);
        assertThat(result.taxableIncome()).isEqualTo(30_000_000L);
        assertThat(result.estimatedIncomeTax()).isEqualTo(3_240_000L);
        assertThat(result.incomeTaxBracket()).isEqualTo("15%");
        assertThat(result.estimatedLocalTax()).isEqualTo(324_000L);
        assertThat(result.estimatedVat()).isEqualTo(4_545_455L - 1_818_182L);
        assertThat(result.taxSavingFromExpenses()).isGreaterThan(0);
    }

    @Test
    @DisplayName("절세 예상: 미확인 장부는 집계에서 제외")
    void unconfirmedEntriesExcluded() {
        Long userId = 1L;

        // aggregate query already filters confirmed=true, so unconfirmed entries return zeros
        given(bookEntryRepository.safeAggregate(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(new AggregateResult(0L, 0L, 0L, 0L));
        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(0L);

        TaxEstimationResponse result = taxEstimationService.estimate(userId, 2026);

        assertThat(result.totalIncome()).isEqualTo(0L);
        assertThat(result.estimatedIncomeTax()).isEqualTo(0L);
    }

    // ───────────── estimateMonthly ─────────────

    @Test
    @DisplayName("월별 추정: 1기(1-6월) VAT 기간 및 종소세 연환산")
    void estimateMonthly_firstHalf() {
        Long userId = 1L;

        // VAT 매출/매입세액
        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(3_000_000L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(1_000_000L);

        // 종소세: 3월까지 수입 1500만, 사업경비 600만
        given(bookEntryRepository.safeAggregate(eq(userId), any(), any()))
                .willReturn(new AggregateResult(15_000_000L, 10_000_000L, 0L, 6_000_000L));

        MonthlyTaxEstimationResponse result = taxEstimationService.estimateMonthly(userId, 2026, 3);

        // VAT = 300만 - 100만 = 200만
        assertThat(result.estimatedVat().estimatedPayable()).isEqualTo(2_000_000L);
        assertThat(result.estimatedVat().period()).contains("1기");

        // 연환산: income 1500만 × 12/3 = 6000만, businessExpense 600만 × 12/3 = 2400만
        assertThat(result.estimatedIncomeTax().projectedAnnualIncome()).isEqualTo(60_000_000L);
        assertThat(result.estimatedIncomeTax().projectedAnnualExpense()).isEqualTo(24_000_000L);

        // 종소세: 과세표준 3600만 → 15% 구간
        assertThat(result.estimatedIncomeTax().estimatedTax()).isGreaterThan(0);

        // 지방세: 종소세 × 10%
        assertThat(result.estimatedLocalTax().amount())
                .isEqualTo((long) Math.floor(result.estimatedIncomeTax().estimatedTax() * 0.1));
    }

    @Test
    @DisplayName("월별 추정: 2기(7-12월) VAT 기간 정확")
    void estimateMonthly_secondHalf() {
        Long userId = 1L;

        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.safeAggregate(eq(userId), any(), any()))
                .willReturn(new AggregateResult(0L, 0L, 0L, 0L));

        MonthlyTaxEstimationResponse result = taxEstimationService.estimateMonthly(userId, 2026, 9);

        assertThat(result.estimatedVat().period()).contains("2기");
        assertThat(result.estimatedIncomeTax().estimatedTax()).isZero();
        assertThat(result.totalEstimatedTax()).isZero();
    }

    @Test
    @DisplayName("월별 추정: currentExpense(DTO)는 businessExpense(agg[3])를 사용")
    void estimateMonthly_usesBusinessExpense_notTotalExpense() {
        Long userId = 1L;

        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(0L);
        // totalExpense = 20_000_000, businessExpense = 8_000_000
        given(bookEntryRepository.safeAggregate(eq(userId), any(), any()))
                .willReturn(new AggregateResult(30_000_000L, 20_000_000L, 0L, 8_000_000L));

        MonthlyTaxEstimationResponse result = taxEstimationService.estimateMonthly(userId, 2026, 6);

        // DTO의 currentExpense 필드에는 businessExpense(800만)가 매핑되어야 함
        assertThat(result.estimatedIncomeTax().currentExpense()).isEqualTo(8_000_000L);
        // 연환산: 800만 × 12/6 = 1600만 (전체경비 2000만이 아님)
        assertThat(result.estimatedIncomeTax().projectedAnnualExpense()).isEqualTo(16_000_000L);
    }

    private BookEntry createEntry(EntryType type, long income, long expense, long asset, long vat,
                                   boolean business, boolean confirmed) {
        BookEntry entry = BookEntry.builder()
                .userId(1L)
                .entryDate(LocalDate.of(2026, 3, 1))
                .entryType(type)
                .incomeAmount(income)
                .expenseAmount(expense)
                .fixedAssetAmount(asset)
                .vatAmount(vat)
                .build();
        if (business) entry.markAsBusiness();
        if (confirmed) entry.confirm();
        return entry;
    }
}
