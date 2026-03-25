package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.dto.TaxSavingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TaxSavingServiceTest {

    @Mock private BookEntryRepository bookEntryRepository;
    @Mock private TaxCalculationEngine taxCalculationEngine;
    @InjectMocks private TaxSavingService taxSavingService;

    @Test
    @DisplayName("매출 3000만 이하 → 노란우산공제 한도 500만원")
    void recommendations_lowRevenue_noranLimit500() {
        setupMocks(30_000_000L, 10_000_000L, 0.06);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("노란우산"))
                .findFirst().orElseThrow()
                .maxAmount()).isEqualTo(5_000_000L);
    }

    @Test
    @DisplayName("매출 5000만 → 노란우산공제 한도 300만원")
    void recommendations_highRevenue_noranLimit300() {
        setupMocks(50_000_000L, 20_000_000L, 0.15);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("노란우산"))
                .findFirst().orElseThrow()
                .maxAmount()).isEqualTo(3_000_000L);
    }

    @Test
    @DisplayName("접대비 한도 미달 시 추가 사용 가능 안내")
    void recommendations_entertainmentUnderLimit_showsRemaining() {
        setupMocks(50_000_000L, 20_000_000L, 0.15);
        given(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("접대비"), eq(2026)))
                .willReturn(5_000_000L);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("접대비"))
                .findFirst()).isPresent();
    }

    @Test
    @DisplayName("접대비 한도 초과 시 접대비 추천 미표시")
    void recommendations_entertainmentOverLimit_noRecommendation() {
        setupMocks(50_000_000L, 20_000_000L, 0.15);
        given(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("접대비"), eq(2026)))
                .willReturn(12_000_000L);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("접대비"))
                .findFirst()).isEmpty();
    }

    @Test
    @DisplayName("totalSummary 집계가 올바르게 계산됨")
    void recommendations_totalSummary_notNull() {
        setupMocks(30_000_000L, 10_000_000L, 0.06);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.totalSummary()).isNotNull();
        assertThat(response.totalSummary().totalMaxAmount()).isGreaterThan(0);
    }

    private void setupMocks(long income, long expense, double taxRate) {
        given(bookEntryRepository.safeAggregate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(new AggregateResult(income, expense, 0L, expense));

        given(taxCalculationEngine.calculate(anyInt(), anyLong(), anyLong(), anyLong(), any()))
                .willReturn(new TaxCalculationResult(
                        income, expense, income - expense,
                        1_500_000L, income - expense - 1_500_000L,
                        taxRate, 1_000_000L, 1_000_000L,
                        0L, 1_000_000L, 100_000L, false));

        // 기본: 사용금액 0 (테스트별 오버라이드 가능하도록 lenient)
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("접대비"), anyInt()))
                .thenReturn(0L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("교육훈련비"), anyInt()))
                .thenReturn(0L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("도서인쇄비"), anyInt()))
                .thenReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(0L);
    }
}
