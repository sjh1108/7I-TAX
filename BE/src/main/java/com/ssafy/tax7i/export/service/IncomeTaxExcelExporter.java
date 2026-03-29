package com.ssafy.tax7i.export.service;

import com.ssafy.tax7i.auth.domain.BusinessProfile;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.classification.entity.TaxCategory;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.service.TaxCalculationEngine;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ssafy.tax7i.export.service.ExcelExportHelper.MOCK_ADDRESS;
import static com.ssafy.tax7i.export.service.ExcelExportHelper.MOCK_BUSINESS_TYPE;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeTaxExcelExporter {

    // ── ①신고서 시트 행/열 인덱스 ──
    private static final int ROW_NAME = 5;
    private static final int ROW_RESIDENT = 6;
    private static final int ROW_ADDRESS = 7;
    private static final int ROW_BIZ_TYPE = 8;
    private static final int ROW_BOOK_TYPE = 9;
    private static final int ROW_REVENUE = 12;
    private static final int ROW_INCOME_AMOUNT = 13;
    private static final int ROW_COMPREHENSIVE_INCOME = 16;
    private static final int ROW_TOTAL_DEDUCTIONS = 17;
    private static final int ROW_TAXABLE_INCOME = 18;
    private static final int ROW_TAX_RATE = 19;
    private static final int ROW_CALCULATED_TAX = 20;
    private static final int ROW_TAX_CREDITS = 21;
    private static final int ROW_DETERMINED_TAX = 22;
    private static final int ROW_PREPAID_TAX = 23;
    private static final int ROW_FINAL_TAX = 24;
    private static final int COL_VALUE = 2;

    // ── ②수입_경비명세서 행 인덱스 ──
    private static final int ROW_EXPENSE_REVENUE = 5;
    private static final int ROW_EXPENSE_TOTAL = 24;
    private static final int COL_EXPENSE_VALUE = 3;

    // ── ③소득금액계산 행 인덱스 ──
    private static final int ROW_CALC_REVENUE = 5;
    private static final int ROW_CALC_RAW_EXPENSE = 6;
    private static final int ROW_CALC_DIFF_INCOME = 7;
    private static final int ROW_CALC_ADJUSTMENT = 11;
    private static final int ROW_CALC_INCOME_AMOUNT = 12;
    private static final int COL_CALC_VALUE = 3;

    // 매출원가 카테고리 코드
    private static final Set<String> COST_OF_SALES_CODES = Set.of(
            TaxCategory.PURCHASE.getCode(),
            TaxCategory.INVENTORY.getCode()
    );

    // 카테고리 코드 → Excel 행 인덱스 매핑
    private static final Map<String, Integer> EXPENSE_CODE_TO_ROW = Map.ofEntries(
            Map.entry(TaxCategory.WELFARE.getCode(), 9),
            Map.entry(TaxCategory.TRAVEL.getCode(), 10),
            Map.entry(TaxCategory.ENTERTAINMENT.getCode(), 11),
            Map.entry(TaxCategory.COMMUNICATION.getCode(), 12),
            Map.entry(TaxCategory.UTILITIES.getCode(), 13),
            Map.entry(TaxCategory.TAX_DUES.getCode(), 14),
            Map.entry(TaxCategory.RENT.getCode(), 15),
            Map.entry(TaxCategory.SERVICE_FEE.getCode(), 16),
            Map.entry(TaxCategory.SHIPPING.getCode(), 18),
            Map.entry(TaxCategory.DEPRECIATION.getCode(), 19),
            Map.entry(TaxCategory.SUPPLIES.getCode(), 20),
            Map.entry(TaxCategory.REPAIR.getCode(), 21),
            Map.entry(TaxCategory.VEHICLE.getCode(), 22),
            Map.entry(TaxCategory.BOOKS.getCode(), 23)
    );

    private final BookEntryRepository bookEntryRepository;
    private final TaxCalculationEngine taxCalculationEngine;
    private final TaxParameterService taxParameterService;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final ExcelExportHelper helper;

    public byte[] export(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        BusinessProfile profile = businessProfileRepository.findByUserId(userId).orElse(null);

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        double withholdingRate = taxParameterService.getWithholdingRate(year);
        long estimatedPrepaid = (long) Math.floor(agg.totalIncome() * withholdingRate);

        TaxCalculationResult result = taxCalculationEngine.calculateForUser(userId, year, estimatedPrepaid, null);
        List<Object[]> categoryExpenses = bookEntryRepository.sumExpenseByCategoryAndYear(userId, year);
        long expenseAdjustment = taxCalculationEngine.computeExpenseAdjustment(userId, year, result.totalRevenue());

        Workbook workbook = helper.loadTemplate("/templates/excel/income_tax_template.xlsx");
        fillDeclarationSheet(workbook.getSheetAt(0), user, profile, year, result);
        fillRevenueExpenseSheet(workbook.getSheetAt(1), result, categoryExpenses);
        fillIncomeCalculationSheet(workbook.getSheetAt(2), result, expenseAdjustment);
        workbook.removeSheetAt(3);

        return helper.workbookToBytes(workbook);
    }

    private void fillDeclarationSheet(Sheet sheet, User user, BusinessProfile profile,
                                       int year, TaxCalculationResult result) {
        helper.setCellValue(sheet, ROW_NAME, COL_VALUE, user.getName());
        String residentPartial = user.getBirthDate() != null
                ? user.getBirthDate().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-*******"
                : "-";
        helper.setCellValue(sheet, ROW_RESIDENT, COL_VALUE, residentPartial);
        helper.setCellValue(sheet, ROW_ADDRESS, COL_VALUE, MOCK_ADDRESS);
        helper.setCellValue(sheet, ROW_BIZ_TYPE, COL_VALUE, MOCK_BUSINESS_TYPE + " / 간편장부");
        helper.setCellValue(sheet, ROW_BOOK_TYPE, COL_VALUE, "간편장부");

        helper.setCellValue(sheet, ROW_REVENUE, COL_VALUE, result.totalRevenue());
        helper.setCellValue(sheet, ROW_INCOME_AMOUNT, COL_VALUE, result.incomeAmount());

        helper.setCellValue(sheet, ROW_COMPREHENSIVE_INCOME, COL_VALUE, result.incomeAmount());
        helper.setCellValue(sheet, ROW_TOTAL_DEDUCTIONS, COL_VALUE, result.totalDeductions());
        helper.setCellValue(sheet, ROW_TAXABLE_INCOME, COL_VALUE, result.taxableIncome());
        helper.setCellValue(sheet, ROW_TAX_RATE, COL_VALUE,
                Math.round(result.taxRate() * 100) + "%");
        helper.setCellValue(sheet, ROW_CALCULATED_TAX, COL_VALUE, result.calculatedTax());
        helper.setCellValue(sheet, ROW_TAX_CREDITS, COL_VALUE, result.totalTaxCredits());
        helper.setCellValue(sheet, ROW_DETERMINED_TAX, COL_VALUE, result.determinedTax());
        helper.setCellValue(sheet, ROW_PREPAID_TAX, COL_VALUE, result.prepaidTax());
        helper.setCellValue(sheet, ROW_FINAL_TAX, COL_VALUE, result.finalTax());
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

        helper.setCellValue(sheet, ROW_EXPENSE_REVENUE, COL_EXPENSE_VALUE, result.totalRevenue());
        for (var entry : EXPENSE_CODE_TO_ROW.entrySet()) {
            long amount = expenseMap.getOrDefault(entry.getKey(), 0L);
            helper.setCellValue(sheet, entry.getValue(), COL_EXPENSE_VALUE, amount);
        }
        helper.setCellValue(sheet, ROW_EXPENSE_TOTAL, COL_EXPENSE_VALUE, expenseTotal);
    }

    private void fillIncomeCalculationSheet(Sheet sheet, TaxCalculationResult result,
                                             long expenseAdjustment) {
        long rawExpense = result.totalExpense() + expenseAdjustment;
        helper.setCellValue(sheet, ROW_CALC_REVENUE, COL_CALC_VALUE, result.totalRevenue());
        helper.setCellValue(sheet, ROW_CALC_RAW_EXPENSE, COL_CALC_VALUE, rawExpense);
        helper.setCellValue(sheet, ROW_CALC_DIFF_INCOME, COL_CALC_VALUE, result.totalRevenue() - rawExpense);
        helper.setCellValue(sheet, ROW_CALC_ADJUSTMENT, COL_CALC_VALUE, expenseAdjustment);
        helper.setCellValue(sheet, ROW_CALC_INCOME_AMOUNT, COL_CALC_VALUE, result.incomeAmount());
    }
}
