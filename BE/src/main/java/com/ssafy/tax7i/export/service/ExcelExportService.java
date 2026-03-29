package com.ssafy.tax7i.export.service;

import com.ssafy.tax7i.auth.domain.BusinessProfile;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.classification.entity.TaxCategory;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.repository.VatReturnRepository;
import com.ssafy.tax7i.tax.service.TaxCalculationEngine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelExportService {

    private final BookEntryRepository bookEntryRepository;
    private final TaxCalculationEngine taxCalculationEngine;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final VatReturnRepository vatReturnRepository;
    private final TemplateEngine templateEngine;

    // 목업: 사업장 소재지 / 업태·종목 (BusinessProfile에 해당 필드 없음)
    private static final String MOCK_ADDRESS = "서울특별시 강남구 테헤란로 212, 7층";
    private static final String MOCK_BUSINESS_TYPE = "정보통신업 / 소프트웨어 개발";

    // 원천징수 세율: 사업소득 3% (소득세법 §129①5)
    private static final double WITHHOLDING_RATE = 0.03;

    // 매출원가 카테고리 코드 (매입 컬럼에 기록)
    private static final Set<String> COST_OF_SALES_CODES = Set.of(
            TaxCategory.PURCHASE.getCode(),   // "02"
            TaxCategory.INVENTORY.getCode()   // "03"
    );

    // ②수입_경비명세서: 카테고리 코드 → Excel 행 인덱스(0-based) 매핑
    private static final Map<String, Integer> EXPENSE_CODE_TO_ROW = Map.ofEntries(
            Map.entry(TaxCategory.WELFARE.getCode(), 9),        // 14 → 복리후생비 row10
            Map.entry(TaxCategory.TRAVEL.getCode(), 10),        // 17 → 여비교통비 row11
            Map.entry(TaxCategory.ENTERTAINMENT.getCode(), 11), // 08 → 접대비 row12
            Map.entry(TaxCategory.COMMUNICATION.getCode(), 12), // 21 → 통신비 row13
            Map.entry(TaxCategory.UTILITIES.getCode(), 13),     // 24 → 수도광열비 row14
            Map.entry(TaxCategory.TAX_DUES.getCode(), 14),      // 05 → 세금과공과 row15
            Map.entry(TaxCategory.RENT.getCode(), 15),          // 06 → 임차료 row16
            Map.entry(TaxCategory.SERVICE_FEE.getCode(), 16),   // 12 → 지급수수료 row17
            // row18 = 보험료 (해당 카테고리 없음, 0)
            Map.entry(TaxCategory.SHIPPING.getCode(), 18),      // 15 → 운반비 row19
            Map.entry(TaxCategory.DEPRECIATION.getCode(), 19),  // 10 → 감가상각비 row20
            Map.entry(TaxCategory.SUPPLIES.getCode(), 20),      // 13 → 소모품비 row21
            Map.entry(TaxCategory.REPAIR.getCode(), 21),        // 25 → 수선비 row22
            Map.entry(TaxCategory.VEHICLE.getCode(), 22),       // 11 → 차량유지비 row23
            Map.entry(TaxCategory.BOOKS.getCode(), 23)          // 23 → 도서인쇄비 row24
    );

    /**
     * 부가가치세 확정신고 Excel 생성
     */
    public byte[] exportVatExcel(Long userId, int year, int half) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        BusinessProfile profile = businessProfileRepository.findByUserId(userId).orElse(null);

        // 과세기간 결정
        LocalDate start, end;
        if (half == 1) {
            start = LocalDate.of(year, 1, 1);
            end = LocalDate.of(year, 6, 30);
        } else {
            start = LocalDate.of(year, 7, 1);
            end = LocalDate.of(year, 12, 31);
        }

        // 매출/매입/고정자산 집계
        long salesAmount = 0, salesVat = 0, salesCount = 0;
        long purchaseAmount = 0, purchaseVat = 0, purchaseCount = 0;
        long assetAmount = 0, assetVat = 0, assetCount = 0;

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
                    salesCount++;
                } else if (e.getEntryType() == EntryType.EXPENSE && e.getIsVatDeductible()) {
                    purchaseAmount += e.getSupplyPrice();
                    purchaseVat += e.getVatAmount();
                    purchaseCount++;
                } else if (e.getEntryType() == EntryType.ASSET && e.getIsVatDeductible()) {
                    assetAmount += e.getSupplyPrice();
                    assetVat += e.getVatAmount();
                    assetCount++;
                }
            }
            page++;
        } while (entryPage.hasNext());

        // 매입세액 합계 = 일반매입 + 고정자산매입
        long totalPurchaseAmount = purchaseAmount + assetAmount;
        long totalPurchaseVat = purchaseVat + assetVat;
        long vatPayable = salesVat - totalPurchaseVat;

        // 예정고지세액: VatReturn에 기록된 값 조회
        long preliminaryPaid = vatReturnRepository
                .findByUserIdAndTaxYearAndTaxPeriod(userId, year, half)
                .map(vr -> vr.getPreliminaryPaid())
                .orElse(0L);

        long finalVat = vatPayable - preliminaryPaid;

        // 템플릿 로드 및 데이터 채우기
        Workbook workbook = loadTemplate("/templates/excel/vat_template.xlsx");

        fillVatDeclarationSheet(workbook.getSheetAt(0), user, profile, year, half,
                salesAmount, salesVat,
                purchaseAmount, purchaseVat,
                assetAmount, assetVat,
                totalPurchaseAmount, totalPurchaseVat,
                vatPayable, preliminaryPaid, finalVat);
        fillSalesInvoiceSummarySheet(workbook.getSheetAt(1), user, profile, year, half,
                salesCount, salesAmount, salesVat);
        fillPurchaseInvoiceSummarySheet(workbook.getSheetAt(2), user, profile, year, half,
                purchaseCount, purchaseAmount, purchaseVat,
                assetCount, assetAmount, assetVat);

        return workbookToBytes(workbook);
    }

    /**
     * 종합소득세 확정신고 Excel 생성
     */
    public byte[] exportIncomeTaxExcel(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        BusinessProfile profile = businessProfileRepository.findByUserId(userId).orElse(null);

        // 기납부세액 추정: 사업소득 원천징수 3% (소득세법 §129①5)
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long estimatedPrepaid = (long) Math.floor(agg.totalIncome() * WITHHOLDING_RATE);

        // 세금 계산 (기납부세액 반영)
        TaxCalculationResult result = taxCalculationEngine.calculateForUser(userId, year, estimatedPrepaid, null);

        // 카테고리별 경비
        List<Object[]> categoryExpenses = bookEntryRepository.sumExpenseByCategoryAndYear(userId, year);

        // 경비 조정액 (소득세법 §33·§35·§33의2)
        long expenseAdjustment = taxCalculationEngine.computeExpenseAdjustment(userId, year, result.totalRevenue());

        // 템플릿 로드
        Workbook workbook = loadTemplate("/templates/excel/income_tax_template.xlsx");

        fillIncomeTaxDeclarationSheet(workbook.getSheetAt(0), user, profile, year, result);
        fillRevenueExpenseSheet(workbook.getSheetAt(1), result, categoryExpenses);
        fillIncomeCalculationSheet(workbook.getSheetAt(2), result, expenseAdjustment);

        // ④간편장부 시트 제거 (PDF로 별도 내보내기)
        workbook.removeSheetAt(3);

        return workbookToBytes(workbook);
    }

    /**
     * 간편장부 PDF 생성 (국세청고시 제2024-19호 양식)
     */
    public byte[] generateSimpleLedgerPdf(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        BusinessProfile profile = businessProfileRepository.findByUserId(userId).orElse(null);

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<LedgerEntry> entries = new ArrayList<>();
        long totalIncome = 0, totalIncomeVat = 0;
        long totalExpense = 0, totalExpenseVat = 0;
        long totalAsset = 0, totalAssetVat = 0;

        int pageNum = 0;
        Page<BookEntry> entryPage;
        do {
            entryPage = bookEntryRepository.findByUserIdAndEntryDateBetween(
                    userId, start, end, PageRequest.of(pageNum, 500, Sort.by("entryDate")));

            for (BookEntry e : entryPage.getContent()) {
                if (!e.getConfirmed() || !e.getIsBusinessExpense()) continue;

                LedgerEntry le = new LedgerEntry();
                le.date = e.getEntryDate().format(dateFmt);
                le.description = e.getDescription() != null ? e.getDescription() : "";
                le.note = e.getNote() != null ? e.getNote() : "";

                if (e.getEntryType() == EntryType.INCOME) {
                    le.incomeAmount = e.getIncomeAmount();
                    le.incomeVat = e.getVatAmount();
                    totalIncome += e.getIncomeAmount();
                    totalIncomeVat += e.getVatAmount();
                } else if (e.getEntryType() == EntryType.EXPENSE) {
                    le.expenseAmount = e.getExpenseAmount();
                    le.expenseVat = e.getVatAmount();
                    totalExpense += e.getExpenseAmount();
                    totalExpenseVat += e.getVatAmount();
                } else if (e.getEntryType() == EntryType.ASSET) {
                    le.assetAmount = e.getFixedAssetAmount();
                    le.assetVat = e.getVatAmount();
                    totalAsset += e.getFixedAssetAmount();
                    totalAssetVat += e.getVatAmount();
                }

                entries.add(le);
            }
            pageNum++;
        } while (entryPage.hasNext());

        Context context = new Context();
        context.setVariable("year", year);
        context.setVariable("userName", user.getName());
        context.setVariable("businessName", profileValue(profile, "businessName"));
        context.setVariable("regNumber", profileValue(profile, "businessRegNumber"));
        context.setVariable("entries", entries);
        context.setVariable("entryCount", entries.size());
        context.setVariable("totalIncome", totalIncome);
        context.setVariable("totalIncomeVat", totalIncomeVat);
        context.setVariable("totalExpense", totalExpense);
        context.setVariable("totalExpenseVat", totalExpenseVat);
        context.setVariable("totalAsset", totalAsset);
        context.setVariable("totalAssetVat", totalAssetVat);

        String html = templateEngine.process("pdf/simple-ledger", context);
        return htmlToPdf(html);
    }

    /** 간편장부 PDF용 DTO */
    public static class LedgerEntry {
        private String date;
        private String description;
        private long incomeAmount;
        private long incomeVat;
        private long expenseAmount;
        private long expenseVat;
        private long assetAmount;
        private long assetVat;
        private String note;

        public String getDate() { return date; }
        public String getDescription() { return description; }
        public long getIncomeAmount() { return incomeAmount; }
        public long getIncomeVat() { return incomeVat; }
        public long getExpenseAmount() { return expenseAmount; }
        public long getExpenseVat() { return expenseVat; }
        public long getAssetAmount() { return assetAmount; }
        public long getAssetVat() { return assetVat; }
        public String getNote() { return note; }
    }

    // ===== 부가세 시트 채우기 =====

    private void fillVatDeclarationSheet(Sheet sheet, User user, BusinessProfile profile,
                                          int year, int half,
                                          long salesAmount, long salesVat,
                                          long purchaseAmount, long purchaseVat,
                                          long assetAmount, long assetVat,
                                          long totalPurchaseAmount, long totalPurchaseVat,
                                          long vatPayable, long preliminaryPaid, long finalVat) {
        // 기간 텍스트 덮어쓰기
        if (half == 1) {
            setCellValue(sheet, 2, 0,
                    "대상기간: " + year + "년 1.1 ~ 6.30 | 마감: 7월 25일 | 제출처: 홈택스 전자신고");
        }

        // 인적사항 (C열 = col 2, 머지셀 좌상단)
        setCellValue(sheet, 5, 2, profileValue(profile, "businessName"));
        setCellValue(sheet, 6, 2, profileValue(profile, "businessRegNumber"));
        setCellValue(sheet, 7, 2, user.getName());
        setCellValue(sheet, 8, 2, MOCK_ADDRESS);
        setCellValue(sheet, 9, 2, MOCK_BUSINESS_TYPE);
        String periodLabel = half == 1
                ? year + "년 1기 (1.1 ~ 6.30)"
                : year + "년 2기 (7.1 ~ 12.31)";
        setCellValue(sheet, 10, 2, periodLabel);

        // 매출세액: ③ 신용카드 발행분 (row 17 → idx 16)
        setCellValue(sheet, 16, 3, salesAmount);
        setCellValue(sheet, 16, 5, salesVat);
        // ⑨ 합계 (row 23 → idx 22)
        setCellValue(sheet, 22, 3, salesAmount);
        setCellValue(sheet, 22, 5, salesVat);

        // 매입세액: ⑩ 일반매입 (row 27 → idx 26)
        setCellValue(sheet, 26, 3, purchaseAmount);
        setCellValue(sheet, 26, 5, purchaseVat);
        // ⑪ 고정자산매입 (row 28 → idx 27)
        setCellValue(sheet, 27, 3, assetAmount);
        setCellValue(sheet, 27, 5, assetVat);
        // ⑮ 합계 (row 32 → idx 31) — 일반매입 + 고정자산매입
        setCellValue(sheet, 31, 3, totalPurchaseAmount);
        setCellValue(sheet, 31, 5, totalPurchaseVat);

        // 납부세액 계산 (E:F 머지됨 → 좌상단 E열 = col 4에 기록)
        // ⑰ 차감 납부세액 (row 36 → idx 35) = 매출세액 - 매입세액
        setCellValue(sheet, 35, 4, vatPayable);
        // ㉑ 예정고지세액 (row 40 → idx 39)
        setCellValue(sheet, 39, 4, preliminaryPaid);
        // ㉔ 최종 납부세액 (row 43 → idx 42) = 차감납부세액 - 예정고지세액
        setCellValue(sheet, 42, 4, finalVat);
    }

    private void fillSalesInvoiceSummarySheet(Sheet sheet, User user, BusinessProfile profile,
                                               int year, int half,
                                               long salesCount, long salesAmount, long salesVat) {
        setCellValue(sheet, 4, 2, profileValue(profile, "businessRegNumber"));
        setCellValue(sheet, 5, 2, profileValue(profile, "businessName"));
        setCellValue(sheet, 6, 2, user.getName());
        String periodLabel = half == 1
                ? year + "년 1기 (1.1 ~ 6.30)"
                : year + "년 2기 (7.1 ~ 12.31)";
        setCellValue(sheet, 7, 2, periodLabel);

        // 사업자 발급분 (row 12 → idx 11)
        setCellValue(sheet, 11, 2, salesCount);
        setCellValue(sheet, 11, 3, salesAmount);
        setCellValue(sheet, 11, 4, salesVat);
        // 합계 (row 14 → idx 13)
        setCellValue(sheet, 13, 2, salesCount);
        setCellValue(sheet, 13, 3, salesAmount);
        setCellValue(sheet, 13, 4, salesVat);
    }

    private void fillPurchaseInvoiceSummarySheet(Sheet sheet, User user, BusinessProfile profile,
                                                  int year, int half,
                                                  long purchaseCount, long purchaseAmount, long purchaseVat,
                                                  long assetCount, long assetAmount, long assetVat) {
        setCellValue(sheet, 4, 2, profileValue(profile, "businessRegNumber"));
        setCellValue(sheet, 5, 2, profileValue(profile, "businessName"));
        setCellValue(sheet, 6, 2, user.getName());
        String periodLabel = half == 1
                ? year + "년 1기 (1.1 ~ 6.30)"
                : year + "년 2기 (7.1 ~ 12.31)";
        setCellValue(sheet, 7, 2, periodLabel);

        // 일반매입 (row 12 → idx 11)
        setCellValue(sheet, 11, 2, purchaseCount);
        setCellValue(sheet, 11, 3, purchaseAmount);
        setCellValue(sheet, 11, 4, purchaseVat);
        // 고정자산매입 (row 13 → idx 12)
        setCellValue(sheet, 12, 2, assetCount);
        setCellValue(sheet, 12, 3, assetAmount);
        setCellValue(sheet, 12, 4, assetVat);
        // 합계 (row 14 → idx 13)
        setCellValue(sheet, 13, 2, purchaseCount + assetCount);
        setCellValue(sheet, 13, 3, purchaseAmount + assetAmount);
        setCellValue(sheet, 13, 4, purchaseVat + assetVat);
    }

    // ===== 소득세 시트 채우기 =====

    private void fillIncomeTaxDeclarationSheet(Sheet sheet, User user, BusinessProfile profile,
                                                int year, TaxCalculationResult result) {
        setCellValue(sheet, 5, 2, user.getName());
        String residentPartial = user.getBirthDate() != null
                ? user.getBirthDate().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-*******"
                : "-";
        setCellValue(sheet, 6, 2, residentPartial);
        setCellValue(sheet, 7, 2, MOCK_ADDRESS);
        setCellValue(sheet, 8, 2, MOCK_BUSINESS_TYPE + " / 간편장부");
        setCellValue(sheet, 9, 2, "간편장부");

        // 사업소득명세 (C열)
        setCellValue(sheet, 12, 2, result.totalRevenue());
        setCellValue(sheet, 13, 2, result.incomeAmount());

        // 세액계산 (C열, C-D 머지됨 → 좌상단 C열에 기록)
        setCellValue(sheet, 16, 2, result.incomeAmount());            // ❶ 종합소득금액
        setCellValue(sheet, 17, 2, result.totalDeductions());          // ❷ 소득공제 합계
        setCellValue(sheet, 18, 2, result.taxableIncome());            // ❸ 과세표준
        setCellValue(sheet, 19, 2,
                Math.round(result.taxRate() * 100) + "%");             // 세율
        setCellValue(sheet, 20, 2, result.calculatedTax());            // ❹ 산출세액
        setCellValue(sheet, 21, 2, result.totalTaxCredits());          // ❺ 세액공제·감면
        setCellValue(sheet, 22, 2, result.determinedTax());            // ❻ 결정세액
        setCellValue(sheet, 23, 2, result.prepaidTax());               // ❼ 기납부세액 (원천징수 3%)
        setCellValue(sheet, 24, 2, result.finalTax());                 // ❽ 납부할 세액
    }

    private void fillRevenueExpenseSheet(Sheet sheet, TaxCalculationResult result,
                                          List<Object[]> categoryExpenses) {
        Map<String, Long> expenseMap = new HashMap<>();
        long expenseTotal = 0;
        for (Object[] row : categoryExpenses) {
            String code = row[0] != null ? row[0].toString() : "";
            long amount = ((Number) row[2]).longValue();
            expenseMap.put(code, amount);
            expenseTotal += amount;
        }

        setCellValue(sheet, 5, 3, result.totalRevenue());

        for (var entry : EXPENSE_CODE_TO_ROW.entrySet()) {
            long amount = expenseMap.getOrDefault(entry.getKey(), 0L);
            setCellValue(sheet, entry.getValue(), 3, amount);
        }

        setCellValue(sheet, 24, 3, expenseTotal);
    }

    private void fillIncomeCalculationSheet(Sheet sheet, TaxCalculationResult result,
                                             long expenseAdjustment) {
        // result.totalExpense()는 조정 후 경비(adjustedExpense)이므로 원본 경비를 복원
        long rawExpense = result.totalExpense() + expenseAdjustment;

        setCellValue(sheet, 5, 3, result.totalRevenue());                              // ❶ 총수입금액
        setCellValue(sheet, 6, 3, rawExpense);                                         // ❷ 필요경비 계 (원본)
        setCellValue(sheet, 7, 3, result.totalRevenue() - rawExpense);                 // ❸ 차감소득금액
        setCellValue(sheet, 11, 3, expenseAdjustment);                                 // ❹ 순금불산입
        setCellValue(sheet, 12, 3, result.incomeAmount());                             // ❺ 소득금액 = ❸ + ❹
    }

    // ===== 유틸 메서드 =====

    private Workbook loadTemplate(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "Excel 템플릿을 찾을 수 없습니다: " + resourcePath);
            }
            return WorkbookFactory.create(is);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Excel 템플릿 로드 실패: " + e.getMessage());
        }
    }

    private byte[] workbookToBytes(Workbook workbook) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Excel 파일 생성 실패: " + e.getMessage());
        } finally {
            try { workbook.close(); } catch (IOException ignored) {}
        }
    }

    private void setCellValue(Sheet sheet, int rowIdx, int colIdx, Object value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);

        if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof String str) {
            cell.setCellValue(str);
        } else if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private byte[] htmlToPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "PDF 생성 실패: " + e.getMessage());
        }
    }

    private String profileValue(BusinessProfile profile, String field) {
        if (profile == null) return "-";
        return switch (field) {
            case "businessName" -> profile.getBusinessName() != null ? profile.getBusinessName() : "-";
            case "businessRegNumber" -> profile.getBusinessRegNumber() != null ? profile.getBusinessRegNumber() : "-";
            case "industryCode" -> profile.getIndustryCode() != null ? profile.getIndustryCode() : "-";
            default -> "-";
        };
    }
}
