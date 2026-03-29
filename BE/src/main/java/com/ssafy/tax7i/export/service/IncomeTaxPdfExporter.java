package com.ssafy.tax7i.export.service;

import com.ssafy.tax7i.auth.domain.BusinessProfile;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.service.TaxCalculationEngine;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.ssafy.tax7i.export.service.ExcelExportHelper.MOCK_BUSINESS_TYPE;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeTaxPdfExporter {

    private final BookEntryRepository bookEntryRepository;
    private final TaxCalculationEngine taxCalculationEngine;
    private final TaxParameterService taxParameterService;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final TemplateEngine templateEngine;
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
        List<Object[]> rawExpenses = bookEntryRepository.sumExpenseByCategoryAndYear(userId, year);

        List<CategoryExpenseDto> categoryExpenses = rawExpenses.stream()
                .map(row -> new CategoryExpenseDto(
                        row[1] != null ? row[1].toString() : "",
                        ((Number) row[2]).longValue()))
                .toList();

        String residentPartial = user.getBirthDate() != null
                ? user.getBirthDate().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-*******"
                : "-";

        double localTaxRate = taxParameterService.getLocalTaxRate(year);
        long localTax = (long) Math.floor(result.determinedTax() * localTaxRate);

        Context context = new Context();
        context.setVariable("year", year);
        context.setVariable("userName", user.getName());
        context.setVariable("residentPartial", residentPartial);
        context.setVariable("businessName", helper.profileValue(profile, "businessName"));
        context.setVariable("regNumber", helper.profileValue(profile, "businessRegNumber"));
        context.setVariable("businessType", MOCK_BUSINESS_TYPE);
        context.setVariable("totalRevenue", result.totalRevenue());
        context.setVariable("totalExpense", result.totalExpense());
        context.setVariable("incomeAmount", result.incomeAmount());
        context.setVariable("totalDeductions", result.totalDeductions());
        context.setVariable("taxableIncome", result.taxableIncome());
        context.setVariable("taxRateLabel", Math.round(result.taxRate() * 100) + "%");
        context.setVariable("calculatedTax", result.calculatedTax());
        context.setVariable("totalTaxCredits", result.totalTaxCredits());
        context.setVariable("determinedTax", result.determinedTax());
        context.setVariable("prepaidTax", result.prepaidTax());
        context.setVariable("finalTax", result.finalTax());
        context.setVariable("localTax", localTax);
        context.setVariable("categoryExpenses", categoryExpenses);

        // 간편장부 거래내역
        List<LedgerEntryDto> ledgerEntries = new ArrayList<>();
        int page = 0;
        Page<BookEntry> entryPage;
        do {
            entryPage = bookEntryRepository.findByUserIdAndEntryDateBetween(
                    userId, start, end, PageRequest.of(page, 500, Sort.by("entryDate")));
            for (BookEntry e : entryPage.getContent()) {
                if (!e.getConfirmed() || !e.getIsBusinessExpense()) continue;
                ledgerEntries.add(new LedgerEntryDto(
                        e.getEntryDate().toString(),
                        e.getCategoryName() != null ? e.getCategoryName() : "",
                        e.getDescription() != null ? e.getDescription() : "",
                        e.getMerchantName() != null ? e.getMerchantName() : "",
                        e.getEntryType() == EntryType.INCOME ? e.getIncomeAmount() : 0,
                        e.getEntryType() == EntryType.EXPENSE ? e.getExpenseAmount() : 0,
                        e.getEntryType() == EntryType.ASSET ? e.getFixedAssetAmount() : 0,
                        e.getVatAmount()
                ));
            }
            page++;
        } while (entryPage.hasNext());
        context.setVariable("ledgerEntries", ledgerEntries);

        String html = templateEngine.process("pdf/income-tax-export", context);
        return helper.htmlToPdf(html);
    }

    public record CategoryExpenseDto(String name, long amount) {}

    public record LedgerEntryDto(String date, String category, String description, String merchant,
                                  long income, long expense, long asset, long vat) {}
}
