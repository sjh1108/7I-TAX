package com.ssafy.tax7i.export.controller;

import com.ssafy.tax7i.export.service.ExportService;
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

    private final ExportService exportService;

    @GetMapping("/book-entries")
    public ResponseEntity<byte[]> exportBookEntries(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        String csv = exportService.exportBookEntriesToCsv(userId, targetYear);
        return csvResponse(csv, "간편장부_" + targetYear + ".csv");
    }

    @GetMapping("/vat")
    public ResponseEntity<byte[]> exportVat(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "1") Integer half) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        String csv = exportService.exportVatSummaryCsv(userId, targetYear, half);
        return csvResponse(csv, "부가세_" + targetYear + "_" + half + "기.csv");
    }

    @GetMapping("/income-tax")
    public ResponseEntity<byte[]> exportIncomeTax(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        String csv = exportService.exportIncomeTaxSummaryCsv(userId, targetYear);
        return csvResponse(csv, "종합소득세_" + targetYear + ".csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] content = csv.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, bytes, 0, bom.length);
        System.arraycopy(content, 0, bytes, bom.length, content.length);
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
