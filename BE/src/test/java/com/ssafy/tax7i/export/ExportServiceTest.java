package com.ssafy.tax7i.export;

import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.export.service.ExportService;
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
class ExportServiceTest {

    @Mock
    private BookEntryRepository bookEntryRepository;

    @Mock
    private TaxEstimationService taxEstimationService;

    @InjectMocks
    private ExportService exportService;

    @Test
    @DisplayName("간편장부 CSV: 헤더가 국세청 양식 기준으로 생성된다")
    void csvHeaderMatchesFormat() {
        given(bookEntryRepository.findByUserIdAndEntryDateBetween(
                eq(1L), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        String csv = exportService.exportBookEntriesToCsv(1L, 2026);

        assertThat(csv).contains("일자,계정과목,거래내용,거래처");
        assertThat(csv).contains("수입금액,수입부가세,비용금액,비용부가세");
        assertThat(csv).contains("자산증감금액,자산부가세,사업용여부,비고");
    }

    @Test
    @DisplayName("간편장부 CSV: 수입 항목은 수입 열에만 금액이 들어간다")
    void incomeEntryInCorrectColumn() {
        BookEntry income = BookEntry.builder()
                .userId(1L)
                .entryDate(LocalDate.of(2026, 3, 1))
                .description("웹개발 용역대금")
                .merchantName("클라이언트A")
                .entryType(EntryType.INCOME)
                .incomeAmount(5_000_000L)
                .vatAmount(500_000L)
                .categoryCode("01")
                .categoryName("매출")
                .build();

        given(bookEntryRepository.findByUserIdAndEntryDateBetween(
                eq(1L), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(income)));

        String csv = exportService.exportBookEntriesToCsv(1L, 2026);
        String[] lines = csv.split("\n");
        String dataLine = lines[1]; // 첫 번째 데이터 행

        assertThat(dataLine).contains("2026-03-01");
        assertThat(dataLine).contains("매출");
        assertThat(dataLine).contains("5000000");
        assertThat(dataLine).contains("500000");
        assertThat(dataLine).contains("사업용");
    }

    @Test
    @DisplayName("간편장부 CSV: 비용 항목은 비용 열에만 금액이 들어간다")
    void expenseEntryInCorrectColumn() {
        BookEntry expense = BookEntry.builder()
                .userId(1L)
                .entryDate(LocalDate.of(2026, 3, 15))
                .description("AWS 서버비")
                .merchantName("Amazon Web Services")
                .entryType(EntryType.EXPENSE)
                .expenseAmount(100_000L)
                .vatAmount(9_091L)
                .categoryCode("12")
                .categoryName("지급수수료")
                .build();

        given(bookEntryRepository.findByUserIdAndEntryDateBetween(
                eq(1L), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(expense)));

        String csv = exportService.exportBookEntriesToCsv(1L, 2026);
        String[] lines = csv.split("\n");
        String dataLine = lines[1];

        assertThat(dataLine).contains("지급수수료");
        assertThat(dataLine).contains("100000");
    }

    @Test
    @DisplayName("부가세 요약 CSV: 매출세액-매입세액 계산")
    void vatSummaryCsv() {
        BookEntry income = BookEntry.builder()
                .userId(1L)
                .entryDate(LocalDate.of(2026, 3, 1))
                .entryType(EntryType.INCOME)
                .incomeAmount(10_000_000L)
                .supplyPrice(9_090_909L)
                .vatAmount(909_091L)
                .build();
        income.confirm();

        BookEntry expense = BookEntry.builder()
                .userId(1L)
                .entryDate(LocalDate.of(2026, 2, 1))
                .entryType(EntryType.EXPENSE)
                .expenseAmount(3_000_000L)
                .supplyPrice(2_727_273L)
                .vatAmount(272_727L)
                .build();
        expense.confirm();

        given(bookEntryRepository.findByUserIdAndEntryDateBetween(
                eq(1L), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(income, expense)));

        String csv = exportService.exportVatSummaryCsv(1L, 2026, 1);

        assertThat(csv).contains("부가가치세 신고 요약");
        assertThat(csv).contains("2026년 1기");
        assertThat(csv).contains("매출");
        assertThat(csv).contains("매입(공제)");
        assertThat(csv).contains("납부(환급) 예상세액");
    }

    @Test
    @DisplayName("CSV: 쉼표 포함 문자열은 따옴표로 감싼다")
    void csvEscapeComma() {
        BookEntry entry = BookEntry.builder()
                .userId(1L)
                .entryDate(LocalDate.of(2026, 3, 1))
                .description("커피, 케이크 구매")
                .merchantName("스타벅스")
                .entryType(EntryType.EXPENSE)
                .expenseAmount(15_000L)
                .vatAmount(1_364L)
                .build();

        given(bookEntryRepository.findByUserIdAndEntryDateBetween(
                eq(1L), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(entry)));

        String csv = exportService.exportBookEntriesToCsv(1L, 2026);
        assertThat(csv).contains("\"커피, 케이크 구매\"");
    }
}
