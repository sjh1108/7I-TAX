package com.ssafy.tax7i.export;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 내보내기 샘플 파일 생성기.
 * 실제 엔드포인트와 동일한 양식으로 샘플 데이터를 채운 파일을 생성합니다.
 * 실행: ./gradlew test --tests "com.ssafy.tax7i.export.SampleExportGenerator"
 */
class SampleExportGenerator {

    private static final Path OUTPUT_DIR = Paths.get("../docs/samples");
    private static final String FONT_REGULAR = "/fonts/NanumGothic-Regular.ttf";
    private static final String FONT_BOLD = "/fonts/NanumGothic-Bold.ttf";

    @Test
    void generateAllSamples() throws Exception {
        Files.createDirectories(OUTPUT_DIR);

        generateBookEntriesCsv();
        generateVatCsv();
        generateIncomeTaxCsv();
        generateLocalTaxCsv();
        generateSimpleLedgerPdf();
        generateVatExcel();
        generateIncomeTaxExcel();
        generateTaxReturnPdf();
        generateTaxReceiptPdf();

        System.out.println("=== 샘플 파일 생성 완료: " + OUTPUT_DIR.toAbsolutePath() + " ===");
    }

    // ──────────────────────────────────────────
    // 1. 간편장부 CSV
    // ──────────────────────────────────────────
    @Test
    void generateBookEntriesCsv() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        StringBuilder sb = new StringBuilder();
        sb.append("일자,계정과목,거래내용,거래처,수입금액,수입부가세,비용금액,비용부가세,자산증감금액,자산부가세,사업용여부,비고\n");
        sb.append("2025-01-05,매출,웹개발 용역대금,클라이언트A,5000000,500000,0,0,0,0,사업용,1월 계약분\n");
        sb.append("2025-01-10,지급수수료,클라우드 서버비,AWS Korea,0,0,330000,33000,0,0,사업용,월정액\n");
        sb.append("2025-01-15,접대비,거래처 식사,강남레스토랑,0,0,150000,15000,0,0,사업용,신년회식\n");
        sb.append("2025-02-01,매출,앱개발 착수금,클라이언트B,10000000,1000000,0,0,0,0,사업용,2월 프로젝트\n");
        sb.append("2025-02-05,소모품비,사무용품 구입,오피스디포,0,0,85000,8500,0,0,사업용,\n");
        sb.append("2025-02-10,차량유지비,주유비,GS칼텍스,0,0,80000,8000,0,0,사업용,업무용 차량\n");
        sb.append("2025-03-01,매출,유지보수 월정액,클라이언트A,2000000,200000,0,0,0,0,사업용,\n");
        sb.append("2025-03-15,통신비,인터넷/전화요금,KT,0,0,55000,5500,0,0,사업용,\n");
        sb.append("2025-03-20,임차료,사무실 월세,강남빌딩,0,0,1000000,100000,0,0,사업용,3월분\n");
        sb.append("2025-04-01,고정자산,노트북 구입,삼성전자,0,0,0,0,2200000,220000,사업용,업무용 장비\n");
        sb.append("2025-04-10,매출,컨설팅 수수료,클라이언트C,3000000,300000,0,0,0,0,사업용,\n");
        sb.append("2025-05-15,복리후생비,직원 식대,배민,0,0,200000,20000,0,0,사업용,5월분\n");

        writeCsvWithBom("간편장부_2025.csv", sb.toString());
    }

    // ──────────────────────────────────────────
    // 2. 부가가치세 CSV
    // ──────────────────────────────────────────
    @Test
    void generateVatCsv() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        StringBuilder sb = new StringBuilder();
        sb.append("부가가치세 신고 요약,2025년 1기\n\n");
        sb.append("구분,공급가액,부가세액\n");
        sb.append("매출,20000000,2000000\n");
        sb.append("매입(공제),1700000,170000\n");
        sb.append("\n납부(환급) 예상세액,,1830000\n");

        writeCsvWithBom("부가세_2025_1기.csv", sb.toString());
    }

    // ──────────────────────────────────────────
    // 3. 종합소득세 CSV
    // ──────────────────────────────────────────
    @Test
    void generateIncomeTaxCsv() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        StringBuilder sb = new StringBuilder();
        sb.append("종합소득세 신고 요약,2025년\n\n");
        sb.append("항목,금액\n");
        sb.append("총수입금액,80000000\n");
        sb.append("필요경비,45000000\n");
        sb.append("과세표준,33500000\n");
        sb.append("세율구간,24%\n");
        sb.append("산출세액,4800000\n");
        sb.append("지방소득세,480000\n");
        sb.append("경비처리 절세효과,2700000\n");

        writeCsvWithBom("종합소득세_2025.csv", sb.toString());
    }

    // ──────────────────────────────────────────
    // 4. 지방소득세 CSV
    // ──────────────────────────────────────────
    @Test
    void generateLocalTaxCsv() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        StringBuilder sb = new StringBuilder();
        sb.append("개인지방소득세 신고 요약,2025년\n");
        sb.append("법적근거,\"지방세법 §92(세율) §95(신고기한)\"\n\n");
        sb.append("항목,내용\n");
        sb.append("귀속연도,2025\n");
        sb.append("성명,홍길동\n");
        sb.append("사업자등록번호,123-45-67890\n");
        sb.append("종합소득세 결정세액,4800000\n");
        sb.append("개인지방소득세(결정세액×10%),480000\n");
        sb.append("신고기한,2026년 5월 31일\n");
        sb.append("납부처,납세지 관할 지방자치단체 (위택스)\n");

        writeCsvWithBom("지방소득세_2025.csv", sb.toString());
    }

    // ──────────────────────────────────────────
    // 5. 간편장부 PDF
    // ──────────────────────────────────────────
    @Test
    void generateSimpleLedgerPdf() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        String html = """
        <!DOCTYPE html>
        <html><head><meta charset="UTF-8"/>
        <style>
            @page { size: A4 landscape; margin: 15mm; }
            body { font-family: 'NanumGothic', sans-serif; font-size: 9pt; line-height: 1.3; color: #000; }
            h1 { text-align: center; font-size: 16pt; margin-bottom: 4px; }
            .sub-header { text-align: center; font-size: 9pt; color: #555; margin-bottom: 12px; }
            .info-table { width: 100%%; border-collapse: collapse; margin-bottom: 12px; }
            .info-table td { padding: 3px 8px; border: 1px solid #999; }
            .info-table td.label { background-color: #f0f0f0; font-weight: bold; width: 12%%; }
            table.ledger { width: 100%%; border-collapse: collapse; margin-bottom: 8px; }
            table.ledger th, table.ledger td { border: 1px solid #333; padding: 3px 6px; }
            table.ledger th { background-color: #d9e2f3; text-align: center; font-weight: bold; font-size: 8pt; }
            table.ledger th.group { background-color: #b4c6e7; font-size: 9pt; }
            table.ledger td { font-size: 8pt; }
            .right { text-align: right; } .center { text-align: center; }
            .total-row td { font-weight: bold; background-color: #f5f5f5; border-top: 2px solid #000; }
            .summary { margin-top: 16px; } .summary table { width: 50%%; border-collapse: collapse; }
            .summary td { padding: 4px 8px; border: 1px solid #999; }
            .summary td.label { background-color: #f0f0f0; font-weight: bold; width: 50%%; }
            .legal { margin-top: 12px; font-size: 7pt; color: #777; }
            .disclaimer { margin-top: 16px; padding: 6px; border: 1px solid #999; font-size: 8pt; color: #555; text-align: center; }
        </style></head><body>
        <h1>간  편  장  부</h1>
        <p class="sub-header">■ 국세청고시 제2024-19호 | 소득세법 §160 | 2025년 귀속</p>
        <table class="info-table"><tr>
            <td class="label">성명</td><td>홍길동</td>
            <td class="label">상호</td><td>길동소프트</td>
            <td class="label">사업자등록번호</td><td>123-45-67890</td>
            <td class="label">기간</td><td>2025.01.01 ~ 2025.12.31</td>
        </tr></table>
        <table class="ledger"><thead>
            <tr>
                <th rowspan="2" style="width:8%%">날짜</th>
                <th rowspan="2" style="width:18%%">거래내용</th>
                <th class="group" colspan="2">수입(매출)</th>
                <th class="group" colspan="2">비용(매입/경비)</th>
                <th class="group" colspan="3">고정자산</th>
                <th rowspan="2" style="width:8%%">비고</th>
            </tr><tr>
                <th style="width:8%%">매출액</th><th style="width:7%%">부가세</th>
                <th style="width:8%%">매입/경비</th><th style="width:7%%">부가세</th>
                <th style="width:8%%">금액</th><th style="width:7%%">부가세</th>
                <th style="width:5%%">증감</th>
            </tr></thead><tbody>
            <tr><td class="center">2025-01-05</td><td>웹개발 용역대금</td><td class="right">5,000,000</td><td class="right">500,000</td><td></td><td></td><td></td><td></td><td></td><td>1월 계약분</td></tr>
            <tr><td class="center">2025-01-10</td><td>클라우드 서버비</td><td></td><td></td><td class="right">330,000</td><td class="right">33,000</td><td></td><td></td><td></td><td>월정액</td></tr>
            <tr><td class="center">2025-01-15</td><td>거래처 식사</td><td></td><td></td><td class="right">150,000</td><td class="right">15,000</td><td></td><td></td><td></td><td>신년회식</td></tr>
            <tr><td class="center">2025-02-01</td><td>앱개발 착수금</td><td class="right">10,000,000</td><td class="right">1,000,000</td><td></td><td></td><td></td><td></td><td></td><td>2월 프로젝트</td></tr>
            <tr><td class="center">2025-02-05</td><td>사무용품 구입</td><td></td><td></td><td class="right">85,000</td><td class="right">8,500</td><td></td><td></td><td></td><td></td></tr>
            <tr><td class="center">2025-02-10</td><td>주유비</td><td></td><td></td><td class="right">80,000</td><td class="right">8,000</td><td></td><td></td><td></td><td>업무용</td></tr>
            <tr><td class="center">2025-03-01</td><td>유지보수 월정액</td><td class="right">2,000,000</td><td class="right">200,000</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
            <tr><td class="center">2025-03-15</td><td>인터넷/전화요금</td><td></td><td></td><td class="right">55,000</td><td class="right">5,500</td><td></td><td></td><td></td><td></td></tr>
            <tr><td class="center">2025-03-20</td><td>사무실 월세</td><td></td><td></td><td class="right">1,000,000</td><td class="right">100,000</td><td></td><td></td><td></td><td>3월분</td></tr>
            <tr><td class="center">2025-04-01</td><td>노트북 구입</td><td></td><td></td><td></td><td></td><td class="right">2,200,000</td><td class="right">220,000</td><td class="center">증가</td><td>업무용 장비</td></tr>
            <tr><td class="center">2025-04-10</td><td>컨설팅 수수료</td><td class="right">3,000,000</td><td class="right">300,000</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
            <tr><td class="center">2025-05-15</td><td>직원 식대</td><td></td><td></td><td class="right">200,000</td><td class="right">20,000</td><td></td><td></td><td></td><td>5월분</td></tr>
            <tr class="total-row">
                <td colspan="2" class="center">합  계</td>
                <td class="right">20,000,000</td><td class="right">2,000,000</td>
                <td class="right">1,900,000</td><td class="right">190,000</td>
                <td class="right">2,200,000</td><td class="right">220,000</td>
                <td></td><td></td>
            </tr>
        </tbody></table>
        <div class="summary"><table>
            <tr><td class="label">총수입금액 (매출)</td><td class="right">20,000,000 원</td></tr>
            <tr><td class="label">총비용 (매입+경비)</td><td class="right">1,900,000 원</td></tr>
            <tr><td class="label">고정자산 매입</td><td class="right">2,200,000 원</td></tr>
            <tr><td class="label">총 건수</td><td class="right">12 건</td></tr>
        </table></div>
        <p class="legal">
            ※ 근거: 소득세법 §160 장부의 비치·기록 | 국세청고시 제2024-19호 간편장부 서식<br/>
            ※ 5년 보관의무: 소득세법 §160의2 "확정신고기한이 지난 날부터 5년간 보존"<br/>
            ※ 미기장 가산세: 산출세액 × 20%
        </p>
        <div class="disclaimer">본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며, 실제 납세증빙으로 사용할 수 없습니다.</div>
        </body></html>
        """;
        writePdf("간편장부_2025.pdf", html);
    }

    // ──────────────────────────────────────────
    // 6. 부가가치세 Excel
    // ──────────────────────────────────────────
    @Test
    void generateVatExcel() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Workbook wb = loadTemplate("/templates/excel/vat_template.xlsx");
        Sheet s0 = wb.getSheetAt(0);

        setCellValue(s0, 2, 0, "대상기간: 2025년 1.1 ~ 6.30 | 마감: 7월 25일 | 제출처: 홈택스 전자신고");
        setCellValue(s0, 5, 2, "길동소프트");
        setCellValue(s0, 6, 2, "123-45-67890");
        setCellValue(s0, 7, 2, "홍길동");
        setCellValue(s0, 8, 2, "서울특별시 강남구 테헤란로 212, 7층");
        setCellValue(s0, 9, 2, "정보통신업 / 소프트웨어 개발");
        setCellValue(s0, 10, 2, "2025년 1기 (1.1 ~ 6.30)");

        // 매출세액
        setCellValue(s0, 16, 3, 20000000); setCellValue(s0, 16, 5, 2000000);
        setCellValue(s0, 22, 3, 20000000); setCellValue(s0, 22, 5, 2000000);
        // 매입세액
        setCellValue(s0, 26, 3, 1500000); setCellValue(s0, 26, 5, 150000);
        setCellValue(s0, 27, 3, 2200000); setCellValue(s0, 27, 5, 220000);
        setCellValue(s0, 31, 3, 3700000); setCellValue(s0, 31, 5, 370000);
        // 납부세액
        setCellValue(s0, 35, 4, 1630000);
        setCellValue(s0, 39, 4, 0);
        setCellValue(s0, 42, 4, 1630000);

        // 시트2: 매출명세서
        Sheet s1 = wb.getSheetAt(1);
        setCellValue(s1, 4, 2, "123-45-67890");
        setCellValue(s1, 5, 2, "길동소프트");
        setCellValue(s1, 6, 2, "홍길동");
        setCellValue(s1, 7, 2, "2025년 1기 (1.1 ~ 6.30)");
        setCellValue(s1, 11, 2, 4); setCellValue(s1, 11, 3, 20000000); setCellValue(s1, 11, 4, 2000000);
        setCellValue(s1, 13, 2, 4); setCellValue(s1, 13, 3, 20000000); setCellValue(s1, 13, 4, 2000000);

        // 시트3: 매입명세서
        Sheet s2 = wb.getSheetAt(2);
        setCellValue(s2, 4, 2, "123-45-67890");
        setCellValue(s2, 5, 2, "길동소프트");
        setCellValue(s2, 6, 2, "홍길동");
        setCellValue(s2, 7, 2, "2025년 1기 (1.1 ~ 6.30)");
        setCellValue(s2, 11, 2, 7); setCellValue(s2, 11, 3, 1500000); setCellValue(s2, 11, 4, 150000);
        setCellValue(s2, 12, 2, 1); setCellValue(s2, 12, 3, 2200000); setCellValue(s2, 12, 4, 220000);
        setCellValue(s2, 13, 2, 8); setCellValue(s2, 13, 3, 3700000); setCellValue(s2, 13, 4, 370000);

        writeWorkbook(wb, "부가세_2025_1기.xlsx");
    }

    // ──────────────────────────────────────────
    // 7. 종합소득세 Excel
    // ──────────────────────────────────────────
    @Test
    void generateIncomeTaxExcel() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Workbook wb = loadTemplate("/templates/excel/income_tax_template.xlsx");

        // 시트1: 신고서
        Sheet s0 = wb.getSheetAt(0);
        setCellValue(s0, 5, 2, "홍길동");
        setCellValue(s0, 6, 2, "900101-*******");
        setCellValue(s0, 7, 2, "서울특별시 강남구 테헤란로 212, 7층");
        setCellValue(s0, 8, 2, "정보통신업 / 소프트웨어 개발 / 간편장부");
        setCellValue(s0, 9, 2, "간편장부");
        setCellValue(s0, 12, 2, 80000000);  // 총수입
        setCellValue(s0, 13, 2, 35000000);  // 소득금액
        setCellValue(s0, 16, 2, 35000000);  // 종합소득금액
        setCellValue(s0, 17, 2, 1500000);   // 소득공제합계
        setCellValue(s0, 18, 2, 33500000);  // 과세표준
        setCellValue(s0, 19, 2, "24%");     // 세율
        setCellValue(s0, 20, 2, 4780000);   // 산출세액
        setCellValue(s0, 21, 2, 200000);    // 세액공제
        setCellValue(s0, 22, 2, 4580000);   // 결정세액
        setCellValue(s0, 23, 2, 2400000);   // 기납부세액
        setCellValue(s0, 24, 2, 2180000);   // 납부할 세금

        // 시트2: 수입/경비명세서
        Sheet s1 = wb.getSheetAt(1);
        setCellValue(s1, 5, 3, 80000000);   // 총수입
        setCellValue(s1, 9, 3, 2400000);    // 복리후생비
        setCellValue(s1, 10, 3, 1200000);   // 여비교통비
        setCellValue(s1, 11, 3, 3600000);   // 접대비
        setCellValue(s1, 12, 3, 660000);    // 통신비
        setCellValue(s1, 13, 3, 480000);    // 수도광열비
        setCellValue(s1, 14, 3, 350000);    // 세금과공과
        setCellValue(s1, 15, 3, 12000000);  // 임차료
        setCellValue(s1, 16, 3, 3960000);   // 지급수수료
        setCellValue(s1, 18, 3, 200000);    // 운반비
        setCellValue(s1, 19, 3, 4400000);   // 감가상각비
        setCellValue(s1, 20, 3, 1020000);   // 소모품비
        setCellValue(s1, 21, 3, 500000);    // 수선비
        setCellValue(s1, 22, 3, 9600000);   // 차량유지비
        setCellValue(s1, 23, 3, 630000);    // 도서인쇄비
        setCellValue(s1, 24, 3, 41000000);  // 경비 합계

        // 시트3: 소득금액계산서
        Sheet s2 = wb.getSheetAt(2);
        setCellValue(s2, 5, 3, 80000000);   // 총수입
        setCellValue(s2, 6, 3, 45000000);   // 필요경비 원액
        setCellValue(s2, 7, 3, 35000000);   // 차액 소득
        setCellValue(s2, 11, 3, 0);         // 경비 조정액
        setCellValue(s2, 12, 3, 35000000);  // 최종 소득금액

        // 시트4 제거 (실제 코드와 동일)
        if (wb.getNumberOfSheets() > 3) {
            wb.removeSheetAt(3);
        }

        writeWorkbook(wb, "종합소득세_2025.xlsx");
    }

    // ──────────────────────────────────────────
    // 8. 종합소득세 확정신고서 PDF (3페이지)
    // ──────────────────────────────────────────
    @Test
    void generateTaxReturnPdf() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        String html = """
        <!DOCTYPE html>
        <html><head><meta charset="UTF-8"/>
        <style>
            @page { size: A4; margin: 20mm; }
            body { font-family: 'NanumGothic', sans-serif; font-size: 10pt; line-height: 1.4; color: #000; }
            h1 { text-align: center; font-size: 16pt; margin-bottom: 4px; }
            h2 { text-align: center; font-size: 13pt; margin-top: 0; margin-bottom: 16px; }
            h3 { font-size: 12pt; margin-top: 20px; margin-bottom: 8px; border-bottom: 1px solid #000; padding-bottom: 4px; }
            table { width: 100%%; border-collapse: collapse; margin-bottom: 12px; }
            th, td { border: 1px solid #333; padding: 4px 8px; }
            th { background-color: #e8e8e8; text-align: center; font-weight: bold; }
            td.label { background-color: #f5f5f5; width: 30%%; font-weight: bold; }
            td.value { text-align: right; width: 20%%; }
            .center { text-align: center; } .right { text-align: right; }
            .page-break { page-break-before: always; }
            .sub-header { text-align: center; font-size: 9pt; color: #555; margin-bottom: 16px; }
            .disclaimer { margin-top: 20px; padding: 8px; border: 1px solid #999; font-size: 8pt; color: #555; text-align: center; }
        </style></head><body>

        <h1>종합소득세 확정신고서</h1>
        <p class="sub-header">7iTAX Simulation</p>

        <h3>납세자 정보</h3>
        <table>
            <tr><td class="label">성명</td><td>홍길동</td><td class="label">주민등록번호</td><td>900101-*******</td></tr>
            <tr><td class="label">귀속연도</td><td>2025년</td><td class="label">접수번호</td><td>TAX-2025-00042</td></tr>
            <tr><td class="label">신고상태</td><td>SUBMITTED</td><td class="label">제출일</td><td>2026-05-15 14:30</td></tr>
        </table>

        <h3>소득 내역</h3>
        <table>
            <tr><td class="label">총수입금액</td><td class="value">80,000,000 원</td><td class="label">필요경비</td><td class="value">45,000,000 원</td></tr>
            <tr><td class="label">소득금액</td><td class="value" colspan="3">35,000,000 원</td></tr>
        </table>

        <h3>소득공제 내역</h3>
        <table>
            <thead><tr><th style="width:60%%">공제 항목</th><th style="width:40%%">공제 금액</th></tr></thead>
            <tbody>
                <tr><td>기본공제 (본인)</td><td class="right">1,500,000 원</td></tr>
                <tr><td class="label">소득공제 합계</td><td class="value">1,500,000 원</td></tr>
            </tbody>
        </table>

        <h3>세액 계산</h3>
        <table>
            <tr><td class="label">과세표준</td><td class="value">33,500,000 원</td><td class="label">적용세율</td><td class="value">24%%</td></tr>
            <tr><td class="label">산출세액</td><td class="value">4,780,000 원</td><td class="label">결정세액</td><td class="value">4,580,000 원</td></tr>
            <tr><td class="label">기납부세액</td><td class="value">2,400,000 원</td><td class="label">납부할 국세</td><td class="value">2,180,000 원</td></tr>
            <tr><td class="label">지방소득세</td><td class="value">458,000 원</td><td class="label">총 납부세액</td><td class="value">2,638,000 원</td></tr>
        </table>

        <div class="disclaimer">본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며, 실제 납세증빙으로 사용할 수 없습니다.</div>

        <!-- 2페이지: 필요경비명세서 -->
        <div class="page-break"></div>
        <h1>필요경비명세서</h1>
        <p class="sub-header">홍길동 | 2025년 귀속</p>
        <table>
            <thead><tr><th style="width:10%%">번호</th><th style="width:20%%">경비코드</th><th style="width:40%%">경비항목</th><th style="width:30%%">금액</th></tr></thead>
            <tbody>
                <tr><td class="center">1</td><td class="center">WELFARE</td><td>복리후생비</td><td class="right">2,400,000 원</td></tr>
                <tr><td class="center">2</td><td class="center">TRAVEL</td><td>여비교통비</td><td class="right">1,200,000 원</td></tr>
                <tr><td class="center">3</td><td class="center">ENTERTAINMENT</td><td>접대비</td><td class="right">3,600,000 원</td></tr>
                <tr><td class="center">4</td><td class="center">COMMUNICATION</td><td>통신비</td><td class="right">660,000 원</td></tr>
                <tr><td class="center">5</td><td class="center">RENT</td><td>임차료</td><td class="right">12,000,000 원</td></tr>
                <tr><td class="center">6</td><td class="center">SERVICE_FEE</td><td>지급수수료</td><td class="right">3,960,000 원</td></tr>
                <tr><td class="center">7</td><td class="center">DEPRECIATION</td><td>감가상각비</td><td class="right">4,400,000 원</td></tr>
                <tr><td class="center">8</td><td class="center">SUPPLIES</td><td>소모품비</td><td class="right">1,020,000 원</td></tr>
                <tr><td class="center">9</td><td class="center">VEHICLE</td><td>차량유지비</td><td class="right">9,600,000 원</td></tr>
                <tr><td class="center">10</td><td class="center">BOOKS</td><td>도서인쇄비</td><td class="right">630,000 원</td></tr>
                <tr><td class="label" colspan="3">필요경비 합계</td><td class="value">45,000,000 원</td></tr>
            </tbody>
        </table>

        <!-- 3페이지: 소득금액계산서 -->
        <div class="page-break"></div>
        <h1>간편장부소득금액계산서</h1>
        <p class="sub-header">홍길동 | 2025년 귀속</p>
        <table>
            <thead><tr><th style="width:60%%">항목</th><th style="width:40%%">금액</th></tr></thead>
            <tbody>
                <tr><td class="label">1. 총수입금액</td><td class="value">80,000,000 원</td></tr>
                <tr><td class="label">2. 필요경비 합계</td><td class="value">45,000,000 원</td></tr>
                <tr><td class="label">3. 소득금액 (1 - 2)</td><td class="value">35,000,000 원</td></tr>
                <tr><td class="label">4. 소득공제 합계</td><td class="value">1,500,000 원</td></tr>
                <tr><td class="label">5. 과세표준 (3 - 4)</td><td class="value">33,500,000 원</td></tr>
                <tr><td class="label">6. 적용세율</td><td class="value">24%%</td></tr>
                <tr><td class="label">7. 산출세액</td><td class="value">4,780,000 원</td></tr>
                <tr><td class="label">8. 결정세액</td><td class="value">4,580,000 원</td></tr>
                <tr><td class="label">9. 기납부세액</td><td class="value">2,400,000 원</td></tr>
                <tr><td class="label">10. 납부할 국세 (8 - 9)</td><td class="value">2,180,000 원</td></tr>
                <tr><td class="label">11. 지방소득세</td><td class="value">458,000 원</td></tr>
                <tr><td class="label">12. 총 납부세액 (10 + 11)</td><td class="value">2,638,000 원</td></tr>
            </tbody>
        </table>
        <div class="disclaimer">본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며, 실제 납세증빙으로 사용할 수 없습니다.</div>
        </body></html>
        """;
        writePdf("종합소득세_확정신고서_샘플.pdf", html);
    }

    // ──────────────────────────────────────────
    // 9. 납부확인서 PDF
    // ──────────────────────────────────────────
    @Test
    void generateTaxReceiptPdf() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        String html = """
        <!DOCTYPE html>
        <html><head><meta charset="UTF-8"/>
        <style>
            @page { size: A4; margin: 20mm; }
            body { font-family: 'NanumGothic', sans-serif; font-size: 10pt; line-height: 1.4; color: #000; }
            h1 { text-align: center; font-size: 18pt; margin-bottom: 4px; }
            h3 { font-size: 12pt; margin-top: 20px; margin-bottom: 8px; border-bottom: 1px solid #000; padding-bottom: 4px; }
            table { width: 100%%; border-collapse: collapse; margin-bottom: 12px; }
            th, td { border: 1px solid #333; padding: 6px 10px; }
            th { background-color: #e8e8e8; text-align: center; font-weight: bold; }
            td.label { background-color: #f5f5f5; width: 35%%; font-weight: bold; }
            td.value { text-align: right; }
            .center { text-align: center; } .right { text-align: right; }
            .sub-header { text-align: center; font-size: 9pt; color: #555; margin-bottom: 20px; }
            .disclaimer { margin-top: 30px; padding: 10px; border: 1px solid #999; font-size: 8pt; color: #555; text-align: center; }
            .footer { margin-top: 40px; font-size: 9pt; text-align: center; color: #333; }
            .total-row td { font-weight: bold; background-color: #f0f0f0; }
        </style></head><body>

        <h1>납부확인서</h1>
        <p class="sub-header">7iTAX Simulation</p>

        <h3>납세자 정보</h3>
        <table>
            <tr><td class="label">성명</td><td>홍길동</td></tr>
            <tr><td class="label">주민등록번호</td><td>900101-*******</td></tr>
            <tr><td class="label">귀속연도</td><td>2025년</td></tr>
            <tr><td class="label">접수번호</td><td>TAX-2025-00042</td></tr>
        </table>

        <h3>납부 내역</h3>
        <table>
            <thead><tr>
                <th style="width:20%%">구분</th><th style="width:25%%">납부금액</th>
                <th style="width:20%%">납부상태</th><th style="width:35%%">납부일시</th>
            </tr></thead>
            <tbody>
                <tr><td class="center">국세</td><td class="right">2,180,000 원</td><td class="center">COMPLETED</td><td class="center">2026-05-20 10:15</td></tr>
                <tr><td class="center">지방세</td><td class="right">458,000 원</td><td class="center">COMPLETED</td><td class="center">2026-05-20 10:16</td></tr>
            </tbody>
        </table>

        <h3>세액 요약</h3>
        <table>
            <tr><td class="label">납부할 국세</td><td class="value">2,180,000 원</td></tr>
            <tr><td class="label">지방소득세</td><td class="value">458,000 원</td></tr>
            <tr class="total-row"><td class="label">총 납부세액</td><td class="value">2,638,000 원</td></tr>
        </table>

        <div class="disclaimer">본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며, 실제 납세증빙으로 사용할 수 없습니다.</div>
        <div class="footer">
            <p>본 확인서는 전자문서로 발급되었으며, 7iTAX 시스템에서 보관됩니다.</p>
            <p>발급기관: 7iTAX (SSAFY 시뮬레이션)</p>
        </div>
        </body></html>
        """;
        writePdf("납부확인서_샘플.pdf", html);
    }

    // ──────────────────────────────────────────
    // 유틸리티
    // ──────────────────────────────────────────

    private void writeCsvWithBom(String filename, String content) throws Exception {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + data.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(data, 0, result, bom.length, data.length);
        Files.write(OUTPUT_DIR.resolve(filename), result);
        System.out.println("  CSV 생성: " + filename);
    }

    private void writePdf(String filename, String html) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> getClass().getResourceAsStream(FONT_REGULAR),
                    "NanumGothic", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> getClass().getResourceAsStream(FONT_BOLD),
                    "NanumGothic", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            Files.write(OUTPUT_DIR.resolve(filename), os.toByteArray());
            System.out.println("  PDF 생성: " + filename);
        }
    }

    private Workbook loadTemplate(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) throw new FileNotFoundException("템플릿 없음: " + resourcePath);
            return WorkbookFactory.create(is);
        }
    }

    private void writeWorkbook(Workbook wb, String filename) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            wb.write(baos);
            Files.write(OUTPUT_DIR.resolve(filename), baos.toByteArray());
            System.out.println("  Excel 생성: " + filename);
        } finally {
            wb.close();
        }
    }

    private void setCellValue(Sheet sheet, int row, int col, Object value) {
        Row r = sheet.getRow(row);
        if (r == null) r = sheet.createRow(row);
        Cell c = r.getCell(col);
        if (c == null) c = r.createCell(col);
        if (value instanceof Number n) c.setCellValue(n.doubleValue());
        else if (value instanceof String s) c.setCellValue(s);
        else if (value != null) c.setCellValue(value.toString());
    }
}
