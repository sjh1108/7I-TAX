package com.ssafy.tax7i.export.service;

import com.ssafy.tax7i.auth.domain.BusinessProfile;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.repository.VatReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VatPdfExporter {

    private final BookEntryRepository bookEntryRepository;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final VatReturnRepository vatReturnRepository;
    private final TemplateEngine templateEngine;
    private final ExcelExportHelper helper;

    public byte[] export(Long userId, int year, int half) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        BusinessProfile profile = businessProfileRepository.findByUserId(userId).orElse(null);

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
        long assetAmount = 0, assetVat = 0;

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
                } else if (e.getEntryType() == EntryType.ASSET && e.getIsVatDeductible()) {
                    assetAmount += e.getSupplyPrice();
                    assetVat += e.getVatAmount();
                }
            }
            page++;
        } while (entryPage.hasNext());

        long totalPurchaseAmount = purchaseAmount + assetAmount;
        long totalPurchaseVat = purchaseVat + assetVat;
        long vatPayable = salesVat - totalPurchaseVat;

        long preliminaryPaid = vatReturnRepository
                .findByUserIdAndTaxYearAndTaxPeriod(userId, year, half)
                .map(vr -> vr.getPreliminaryPaid())
                .orElse(0L);
        long finalVat = vatPayable - preliminaryPaid;

        String periodLabel = half == 1
                ? year + ".01.01 ~ " + year + ".06.30"
                : year + ".07.01 ~ " + year + ".12.31";

        Context context = new Context();
        context.setVariable("year", year);
        context.setVariable("half", half);
        context.setVariable("periodLabel", periodLabel);
        context.setVariable("userName", user.getName());
        context.setVariable("businessName", helper.profileValue(profile, "businessName"));
        context.setVariable("regNumber", helper.profileValue(profile, "businessRegNumber"));
        context.setVariable("salesAmount", salesAmount);
        context.setVariable("salesVat", salesVat);
        context.setVariable("purchaseAmount", purchaseAmount);
        context.setVariable("purchaseVat", purchaseVat);
        context.setVariable("assetAmount", assetAmount);
        context.setVariable("assetVat", assetVat);
        context.setVariable("totalPurchaseAmount", totalPurchaseAmount);
        context.setVariable("totalPurchaseVat", totalPurchaseVat);
        context.setVariable("vatPayable", vatPayable);
        context.setVariable("preliminaryPaid", preliminaryPaid);
        context.setVariable("finalVat", finalVat);

        String html = templateEngine.process("pdf/vat-return", context);
        return helper.htmlToPdf(html);
    }
}
