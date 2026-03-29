package com.ssafy.tax7i.export.controller;

import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.export.service.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class ExportControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ExportService exportService;
    @MockitoBean private com.ssafy.tax7i.export.service.ExcelExportService excelExportService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── GET /api/export/book-entries ─────────────

    @Test
    void exportBookEntries_200() throws Exception {
        String csvContent = "일자,계정과목,거래내용,거래처,수입금액,수입부가세,비용금액,비용부가세,자산증감금액,자산부가세,사업용여부,비고\n"
                + "2026-01-15,복리후생비,점심식사,스타벅스,0,0,11000,1000,0,0,사업용,\n";
        given(exportService.exportBookEntriesToCsv(any(), anyInt())).willReturn(csvContent);

        mockMvc.perform(get("/api/export/book-entries")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("2026")));
    }

    @Test
    void exportBookEntries_200_날짜범위() throws Exception {
        String csvContent = "일자,계정과목,거래내용,거래처,수입금액,수입부가세,비용금액,비용부가세,자산증감금액,자산부가세,사업용여부,비고\n";
        given(exportService.exportBookEntriesToCsv(any(), any(java.time.LocalDate.class), any(java.time.LocalDate.class)))
                .willReturn(csvContent);

        mockMvc.perform(get("/api/export/book-entries")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void exportBookEntries_200_파라미터없음_현재연도() throws Exception {
        String csvContent = "일자,계정과목,거래내용,거래처,수입금액,수입부가세,비용금액,비용부가세,자산증감금액,자산부가세,사업용여부,비고\n";
        given(exportService.exportBookEntriesToCsv(any(), anyInt())).willReturn(csvContent);

        mockMvc.perform(get("/api/export/book-entries"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));
    }

    // ───────────── GET /api/export/vat ─────────────

    @Test
    void exportVat_200() throws Exception {
        String csvContent = "부가가치세 신고 요약,2026년 1기\n\n구분,공급가액,부가세액\n매출,1000000,100000\n";
        given(exportService.exportVatSummaryCsv(any(), anyInt(), anyInt())).willReturn(csvContent);

        mockMvc.perform(get("/api/export/vat")
                        .param("year", "2026")
                        .param("half", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("2026")));
    }

    @Test
    void exportVat_200_2기() throws Exception {
        String csvContent = "부가가치세 신고 요약,2026년 2기\n\n구분,공급가액,부가세액\n";
        given(exportService.exportVatSummaryCsv(any(), anyInt(), anyInt())).willReturn(csvContent);

        mockMvc.perform(get("/api/export/vat")
                        .param("year", "2026")
                        .param("half", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void exportVat_200_파라미터없음_기본값1기() throws Exception {
        // half 기본값 1 (defaultValue = "1")
        String csvContent = "부가가치세 신고 요약,2026년 1기\n";
        given(exportService.exportVatSummaryCsv(any(), anyInt(), anyInt())).willReturn(csvContent);

        mockMvc.perform(get("/api/export/vat"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));
    }

    // ───────────── GET /api/export/income-tax ─────────────

    @Test
    void exportIncomeTax_200() throws Exception {
        String csvContent = "종합소득세 신고 요약,2025년\n\n항목,금액\n총수입금액,5000000\n";
        given(exportService.exportIncomeTaxSummaryCsv(any(), anyInt())).willReturn(csvContent);

        mockMvc.perform(get("/api/export/income-tax")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void exportIncomeTax_200_파라미터없음_현재연도() throws Exception {
        String csvContent = "종합소득세 신고 요약,2026년\n";
        given(exportService.exportIncomeTaxSummaryCsv(any(), anyInt())).willReturn(csvContent);

        mockMvc.perform(get("/api/export/income-tax"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));
    }
}
