package com.ssafy.tax7i.tax.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.entity.ExpenseDetail;
import com.ssafy.tax7i.tax.entity.TaxPayment;
import com.ssafy.tax7i.tax.entity.TaxReturn;
import com.ssafy.tax7i.tax.repository.ExpenseDetailRepository;
import com.ssafy.tax7i.tax.repository.TaxPaymentRepository;
import com.ssafy.tax7i.tax.repository.TaxReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;
    private final TaxReturnRepository taxReturnRepository;
    private final ExpenseDetailRepository expenseDetailRepository;
    private final TaxPaymentRepository taxPaymentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generates a PDF for the tax return declaration (종합소득세 확정신고서).
     */
    public byte[] generateTaxReturnPdf(Long userId, Long returnId) {
        TaxReturn taxReturn = taxReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_RETURN_NOT_FOUND));
        List<ExpenseDetail> details = expenseDetailRepository.findByTaxReturn_Id(returnId);

        Context context = new Context();
        context.setVariable("r", taxReturn);
        context.setVariable("details", details);
        context.setVariable("deductions", parseDeductions(taxReturn.getDeductionsJson()));

        String html = templateEngine.process("pdf/tax-return", context);
        return htmlToPdf(html);
    }

    /**
     * Generates a PDF receipt for tax payment (납부확인서).
     */
    public byte[] generateReceiptPdf(Long userId, Long returnId) {
        TaxReturn taxReturn = taxReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_RETURN_NOT_FOUND));
        List<TaxPayment> payments = taxPaymentRepository.findByTaxReturn_Id(returnId);

        Context context = new Context();
        context.setVariable("r", taxReturn);
        context.setVariable("payments", payments);

        String html = templateEngine.process("pdf/tax-receipt", context);
        return htmlToPdf(html);
    }

    /**
     * Converts an HTML string to PDF bytes using openhtmltopdf.
     *
     * Note on Korean font support: openhtmltopdf requires explicit font
     * registration for CJK characters. If Korean text does not render,
     * register a font (e.g. NanumGothic) via PdfRendererBuilder.useFont().
     */
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

    /**
     * Parses the deductions JSON string into a Map.
     * Returns an empty map if the input is null, blank, or invalid JSON.
     */
    private Map<String, Long> parseDeductions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Long>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
