package com.ssafy.tax7i.export.controller;

import com.ssafy.tax7i.export.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExcelExportService excelExportService;

    // ── PDF ──

    @GetMapping("/simple-ledger/pdf")
    public ResponseEntity<byte[]> exportSimpleLedgerPdf(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        byte[] pdfBytes = excelExportService.generateSimpleLedgerPdf(userId, targetYear);
        return pdfResponse(pdfBytes, "간편장부_" + targetYear + ".pdf");
    }

    @GetMapping("/income-tax/pdf")
    public ResponseEntity<byte[]> exportIncomeTaxPdf(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        byte[] pdfBytes = excelExportService.generateSimpleLedgerPdf(userId, targetYear);
        return pdfResponse(pdfBytes, "간편장부_" + targetYear + ".pdf");
    }

    // ── Excel ──

    @GetMapping("/vat/excel")
    public ResponseEntity<byte[]> exportVatExcel(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "1") Integer half) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        byte[] excelBytes = excelExportService.exportVatExcel(userId, targetYear, half);
        return excelResponse(excelBytes, "부가세_" + targetYear + "_" + half + "기.xlsx");
    }

    @GetMapping("/income-tax/excel")
    public ResponseEntity<byte[]> exportIncomeTaxExcel(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        byte[] excelBytes = excelExportService.exportIncomeTaxExcel(userId, targetYear);
        return excelResponse(excelBytes, "종합소득세_" + targetYear + ".xlsx");
    }

    // ── Response Helpers ──

    private String buildContentDisposition(String filename) {
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded;
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(filename))
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(filename))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
