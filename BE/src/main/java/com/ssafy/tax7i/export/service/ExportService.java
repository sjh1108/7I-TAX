package com.ssafy.tax7i.export.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import com.ssafy.tax7i.taxcalendar.entity.TaxDeadline;
import com.ssafy.tax7i.taxcalendar.repository.TaxDeadlineRepository;
import com.ssafy.tax7i.taxestimation.dto.TaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.service.TaxEstimationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private final BookEntryRepository bookEntryRepository;
    private final TaxEstimationService taxEstimationService;
    private final UserRepository userRepository;
    private final TaxParameterService taxParameterService;
    private final TaxDeadlineRepository taxDeadlineRepository;

    /**
     * 간편장부 전체 CSV (국세청 양식 기반)
     */
    public String exportBookEntriesToCsv(Long userId, int year) {
        return exportBookEntriesToCsv(userId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public String exportBookEntriesToCsv(Long userId, LocalDate start, LocalDate end) {

        StringBuilder sb = new StringBuilder();
        // BOM is added by controller // UTF-8 BOM for Excel
        sb.append("일자,계정과목,거래내용,거래처,수입금액,수입부가세,비용금액,비용부가세,자산증감금액,자산부가세,사업용여부,비고\n");

        int page = 0;
        Page<BookEntry> entryPage;
        do {
            entryPage = bookEntryRepository.findByUserIdAndEntryDateBetween(
                    userId, start, end, PageRequest.of(page, 500, Sort.by("entryDate")));

            for (BookEntry e : entryPage.getContent()) {
                sb.append(e.getEntryDate()).append(',');
                sb.append(csvEscape(e.getCategoryName())).append(',');
                sb.append(csvEscape(e.getDescription())).append(',');
                sb.append(csvEscape(e.getMerchantName())).append(',');

                if (e.getEntryType() == EntryType.INCOME) {
                    sb.append(e.getIncomeAmount()).append(',');
                    sb.append(e.getVatAmount()).append(',');
                    sb.append("0,0,0,0,");
                } else if (e.getEntryType() == EntryType.EXPENSE) {
                    sb.append("0,0,");
                    sb.append(e.getExpenseAmount()).append(',');
                    sb.append(e.getVatAmount()).append(',');
                    sb.append("0,0,");
                } else { // ASSET
                    sb.append("0,0,0,0,");
                    sb.append(e.getFixedAssetAmount()).append(',');
                    sb.append(e.getVatAmount()).append(',');
                }

                sb.append(e.getIsBusinessExpense() ? "사업용" : "개인용").append(',');
                sb.append(csvEscape(e.getNote())).append('\n');
            }
            page++;
        } while (entryPage.hasNext());

        return sb.toString();
    }

    /**
     * 부가세 신고 요약 CSV
     */
    public String exportVatSummaryCsv(Long userId, int year, int half) {
        LocalDate start, end;
        if (half == 1) {
            start = LocalDate.of(year, 1, 1);
            end = LocalDate.of(year, 6, 30);
        } else {
            start = LocalDate.of(year, 7, 1);
            end = LocalDate.of(year, 12, 31);
        }

        long salesAmount = 0, salesVat = 0;
        long purchaseAmount = 0, purchaseVat = 0;

        int page = 0;
        Page<BookEntry> entryPage;
        do {
            entryPage = bookEntryRepository.findByUserIdAndEntryDateBetween(
                    userId, start, end, PageRequest.of(page, 500, Sort.by("entryDate")));

            for (BookEntry e : entryPage.getContent()) {
                if (!e.getConfirmed() || !e.getIsBusinessExpense()) continue;
                if (e.getEntryType() == EntryType.INCOME) {
                    salesAmount += e.getSupplyPrice();
                    salesVat += e.getVatAmount();
                } else if (e.getEntryType() == EntryType.EXPENSE && e.getIsVatDeductible()) {
                    purchaseAmount += e.getSupplyPrice();
                    purchaseVat += e.getVatAmount();
                }
            }
            page++;
        } while (entryPage.hasNext());

        long vatPayable = salesVat - purchaseVat;

        StringBuilder sb = new StringBuilder();
        // BOM is added by controller
        sb.append("부가가치세 신고 요약,").append(year).append("년 ").append(half).append("기\n\n");
        sb.append("구분,공급가액,부가세액\n");
        sb.append("매출,").append(salesAmount).append(',').append(salesVat).append('\n');
        sb.append("매입(공제),").append(purchaseAmount).append(',').append(purchaseVat).append('\n');
        sb.append("\n납부(환급) 예상세액,,").append(vatPayable).append('\n');

        return sb.toString();
    }

    /**
     * 종합소득세 요약 CSV
     */
    public String exportIncomeTaxSummaryCsv(Long userId, int year) {
        TaxEstimationResponse est = taxEstimationService.estimate(userId, year);

        StringBuilder sb = new StringBuilder();
        // BOM is added by controller
        sb.append("종합소득세 신고 요약,").append(year).append("년\n\n");
        sb.append("항목,금액\n");
        sb.append("총수입금액,").append(est.totalIncome()).append('\n');
        sb.append("필요경비,").append(est.deductibleExpenses()).append('\n');
        sb.append("과세표준,").append(est.taxableIncome()).append('\n');
        sb.append("세율구간,").append(est.incomeTaxBracket()).append('\n');
        sb.append("산출세액,").append(est.estimatedIncomeTax()).append('\n');
        sb.append("지방소득세,").append(est.estimatedLocalTax()).append('\n');
        sb.append("경비처리 절세효과,").append(est.taxSavingFromExpenses()).append('\n');

        return sb.toString();
    }

    /**
     * 지방소득세 요약 CSV
     * 세법 근거: 지방세법 §92(세율 = 소득세의 1/10), §95(종소세와 동시 신고 5.1~5.31)
     * 주의: 개인지방소득세 = "결정세액"의 10% (납부세액의 10%가 아님)
     */
    public String exportLocalTaxSummaryCsv(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        TaxEstimationResponse est = taxEstimationService.estimate(userId, year);

        // 지방세법 §92: 개인지방소득세 = 종합소득세 결정세액 × 지방세율 (원 단위 절사)
        long incomeTax = est.estimatedIncomeTax();
        double localTaxRate = taxParameterService.getLocalTaxRate(year);
        long localTax = (long) Math.floor(incomeTax * localTaxRate);
        long localTaxPercent = Math.round(localTaxRate * 100);

        // 신고기한 조회
        int deadlineYear = year + 1;
        String deadlineStr = taxDeadlineRepository.findByYearAndName(deadlineYear, "지방소득세 신고")
                .or(() -> taxDeadlineRepository.findByYearAndName(year, "지방소득세 신고"))
                .map(d -> deadlineYear + "년 " + d.getDeadlineMonth() + "월 " + d.getDeadlineDay() + "일")
                .orElse(deadlineYear + "년 5월 31일");

        StringBuilder sb = new StringBuilder();
        sb.append("개인지방소득세 신고 요약,").append(year).append("년\n");
        sb.append("법적근거,\"지방세법 §92(세율) §95(신고기한)\"\n\n");
        sb.append("항목,내용\n");
        sb.append("귀속연도,").append(year).append('\n');
        sb.append("성명,").append(csvEscape(user.getName())).append('\n');
        sb.append("사업자등록번호,-\n");
        sb.append("종합소득세 결정세액,").append(incomeTax).append('\n');
        sb.append("개인지방소득세(결정세액×").append(localTaxPercent).append("%),").append(localTax).append('\n');
        sb.append("신고기한,").append(deadlineStr).append('\n');
        sb.append("납부처,납세지 관할 지방자치단체 (위택스)\n");

        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
