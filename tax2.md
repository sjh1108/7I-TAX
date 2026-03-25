# 7iTAX 미구현 30개 — 세법 검증 완료 Claude CLI 프롬프트 (최종판)

> **27개 기능 + 3개 데이터 불일치**
> ⚠ 불일치 3개를 먼저 해결해야 나머지가 안 깨짐.
> 한 번에 하나씩만 CLI에 넘길 것.

---

## ⚠ 선행 작업: 데이터 불일치 해결 (반드시 먼저)

---

### ⚠1. TaxCategory enum 불일치 [세법 검증 필요]

프롬프트.md의 국세청고시 코드(접대비=17, 도서인쇄비=25, 연구비=27)와
코드베이스 TaxCategory(접대비=08, 도서인쇄비·연구비 없음)가 불일치.

세법 근거: 국세청고시 제2024-19호 (간편장부 서식)
간편장부에서 사용하는 세목 분류:
11 매입비용 / 14 임차료 / 15 인건비 / 16 여비교통비 / 17 접대비
18 보험료 / 19 수선비 / 20 소모품비 / 21 지급수수료 / 22 차량유지비
23 복리후생비 / 24 통신비 / 25 도서인쇄비 / 26 광고선전비 / 27 연구비(교육훈련비 포함)
30 감가상각비 / 40 고정자산

```
프로젝트에서 TaxCategory 관련 코드를 전부 읽어줘:
1. TaxCategory enum 또는 엔티티
2. MccTaxRule 엔티티
3. classification 패키지의 모든 파일

현재 TaxCategory enum의 값들을 전부 나열해줘.
그 다음, 국세청고시 제2024-19호 간편장부 세목코드와 비교해서 빠진 항목을 추가해줘.

추가해야 할 세목 (현재 enum에 없는 것):
- 통신비 (국세청 코드 24) — 시안 #4 "KT - 통신비" 표현 필요
- 교육훈련비/연구비 (국세청 코드 27) — 시안 #3 "인프런 - 교육훈련비" 표현 필요
- 도서인쇄비 (국세청 코드 25) — 기술서적 등
- 광고선전비 (국세청 코드 26) — 온라인 광고
- 감가상각비 (국세청 코드 30) — 고정자산 연간 상각
- 기존 enum에 있지만 코드 번호가 다른 것이 있으면 코드를 국세청 기준으로 맞출 것

주의: 기존 코드(접대비=08 등)를 사용하는 곳이 있으면 영향 범위 파악 후 마이그레이션 필요.
기존 데이터가 깨지지 않도록 enum 추가만 하거나, 매핑 테이블 방식 검토.

변경 영향 범위를 먼저 보여주고, 안전한 방법을 제안해줘.
```

---

### ⚠2. User 엔티티 필드 부재

사업자등록번호·업종코드·주소가 없음 → #11(CSV), #14(PDF)에 영향

```
프로젝트에서 User 엔티티와 BusinessProfile 엔티티를 읽어줘.
(make.md에 BusinessProfile이 있으므로 그쪽에 사업자 정보가 있을 수 있음)

확인할 것:
1. 사업자등록번호 (businessNumber / businessRegistrationNumber)
2. 업종코드 (businessCode / industryCode)  
3. 주소 (address)
4. 관할세무서 (taxOfficeCode)

이 필드들이 User에 없고 BusinessProfile에 있다면:
→ 이후 프롬프트에서 "User에서 가져오기" 대신 "BusinessProfile에서 가져오기"로 안내할 것이므로
→ 어느 엔티티에 어떤 필드명으로 있는지 정확히 알려줘.

만약 둘 다 없으면:
→ BusinessProfile에 다음 필드를 추가:
  - businessNumber (String, 사업자등록번호 123-45-67890)
  - businessCode (String, 업종코드 기본값 722000)
  - address (String, 사업장 주소)
  - taxOfficeCode (String, 관할세무서 코드)
```

---

### ⚠3. 한글 폰트 파일 부재

```
프로젝트에서 resources/fonts/ 디렉토리가 있는지 확인해줘.
그리고 build.gradle에 PDF 관련 의존성(openhtmltopdf, itext, openpdf 등)이 있는지 확인.

없으면:
1. src/main/resources/fonts/ 디렉토리 생성
2. README 파일 추가:
   "이 폴더에 NotoSansKR-Regular.ttf를 배치하세요.
    다운로드: https://fonts.google.com/noto/specimen/Noto+Sans+KR
    PDF 생성 시 한글 폰트가 없으면 깨집니다."
3. .gitignore에 *.ttf가 포함되어 있으면 제거 (폰트는 커밋해야 함)
4. Dockerfile에 폰트 COPY 라인 추가 (Docker 사용 시)

이건 구현이 아니라 파일 배치만 하면 됨.
```

---

## A그룹: 장부 목록 필터 (#1~#9)

---

### #1. entryType 필터 [세법 불필요]

```
프로젝트에서 다음을 순서대로 읽어줘:
1. BookEntry 엔티티 (entryType 필드)
2. EntryType enum
3. BookEntryController (GET /api/book-entries)
4. BookEntryRepository
5. BookEntryService (목록 조회)

GET /api/book-entries에 entryType 파라미터 추가.

요구사항:
- 파라미터: entryType (String, optional) — EntryType enum 값
- null이면 전체 조회
- 기존 confirmed 필터와 AND 조합
- 기존 쿼리 패턴(Specification/QueryDSL/@Query) 따를 것

변경 파일 목록 + curl 테스트.
```

---

### #2. 월별 날짜 범위 필터 [세법 불필요]

```
프로젝트에서 BookEntry의 날짜 필드명을 확인해줘 (transactionDate, tradedAt, createdAt 등).

GET /api/book-entries에 날짜 범위 추가.

요구사항:
- 파라미터: startDate, endDate (LocalDate, optional)
- 거래일 필드 기준 (createdAt 아님)
- 기존 필터들과 AND 조합
- 기존 쿼리 패턴 따를 것
```

---

### #3. 교육훈련비 카테고리 추가 [⚠1 선행 필요]

```
⚠1에서 TaxCategory를 수정한 뒤 이 태스크 실행.

TaxCategory에 교육훈련비(연구비, 코드 27)가 추가되었는지 확인.
추가되었으면 → 이 태스크는 완료.
추가 안 되었으면 → ⚠1을 먼저 실행.

확인만 하면 됨.
```

---

### #4. 통신비 카테고리 추가 [⚠1 선행 필요]

```
⚠1에서 TaxCategory에 통신비(코드 24)가 추가되었는지 확인.
추가되었으면 완료. 안 되었으면 ⚠1 먼저.
```

---

### #5. 기간 범위 필터 [#2와 동일]

```
#2에서 이미 구현됨. 필터설정 화면에서도 같은 파라미터 사용.
추가 작업 없음.
```

---

### #6. 카테고리 필터 [세법 참고]

```
프로젝트에서 BookEntry의 카테고리 필드명과 타입 확인.
(⚠1 이후 TaxCategory enum이 수정되었을 수 있으므로 다시 확인)

GET /api/book-entries에 카테고리 필터 추가.

요구사항:
- 파라미터: category (기존 카테고리 필드 타입에 맞춤)
- 기존 필터들과 AND 조합
- 기존 쿼리 패턴 따를 것
```

---

### #7. 거래처 검색 [세법 불필요]

```
프로젝트에서 BookEntry의 거래처/가맹점 필드명 확인.

GET /api/book-entries에 keyword 검색 추가.

요구사항:
- 파라미터: keyword (String, optional)
- LIKE '%keyword%' (PostgreSQL이면 ILIKE)
- 기존 필터들과 AND 조합
```

---

### #8. 증빙 필터 [세법 검증 완료]

세법 근거: 소득세법 시행령 제208조의2 (적격증빙)
적격증빙 4종: 세금계산서, 계산서, 신용카드매출전표, 현금영수증

```
프로젝트에서 BookEntry 엔티티를 읽어줘.
증빙 관련 필드 확인. paymentId만 있고 별도 증빙 필드 없음이 확인됨.

GET /api/book-entries에 증빙 필터 추가.

판단 로직:
- paymentId가 NOT NULL이면 → 카드 결제 건 → 적격증빙 (신용카드매출전표)
- paymentId가 NULL이면 → 수동 등록 건 → 증빙 미확인

요구사항:
- 파라미터: hasEvidence (Boolean, optional)
- true: paymentId IS NOT NULL인 거래만
- false: paymentId IS NULL인 거래만
- null: 전체
- 주석에 세법 근거 표시: "적격증빙 판단 — 소득세법 시행령 §208의2"
```

---

### #9. 미분류 필터 [세법 불필요]

```
프로젝트에서 BookEntry의 카테고리 필드 확인.

GET /api/book-entries에 미분류 필터 추가.

요구사항:
- 파라미터: unclassified (Boolean, optional)
- true: 카테고리가 null인 거래만
- false: 분류 완료된 거래만
- confirmed(사용자 확인)와는 별개 필터임
```

---

## B그룹: 거래 상세 + 내보내기 (#10~#14)

---

### #10. 메모 수정 API [세법 불필요]

```
프로젝트에서 BookEntryController의 기존 PATCH 패턴 확인.

같은 패턴으로 추가:
PATCH /api/book-entries/{entryId}/note
Body: { "note": "거래처 미팅 점심" }

요구사항:
- 소유자 검증
- null/빈문자열이면 삭제
- 200자 이내 @Size
- 기존 PATCH 패턴 동일
```

---

### #11. 지방소득세 CSV [세법 검증 완료]

세법 근거:
- 지방세법 제92조: 세율 = 소득세의 1/10
- 지방세법 제95조: 종소세와 동시 신고(5.1~5.31)

```
프로젝트에서 ExportService, ExportController 패턴 확인.
⚠2에서 확인한 사업자 정보 필드 위치(User 또는 BusinessProfile) 참고.

GET /api/export/local-tax?year=2025

CSV 필드:
- 귀속연도
- 성명 (User에서)
- 사업자등록번호 (⚠2에서 확인한 위치에서)
- 종합소득세 결정세액 (TaxEstimationService)
- 개인지방소득세 = 결정세액 × 10% (원 단위 절사)
  주의: "결정세액"의 10%이지, "납부세액"의 10%가 아님
- 신고기한: "{year+1}년 5월 31일"
- 법적근거: "지방세법 제92조, 제95조"

파일명: 지방소득세_{year}.csv
기존 Export 패턴 따를 것.
```

---

### #12. 내보내기 날짜 범위 [세법 불필요]

```
ExportController 확인. GET /api/export/book-entries에
startDate, endDate 파라미터 추가.
있으면 year 무시. 기존 로직 최소 변경.
```

---

### #13. Excel 내보내기 [세법 불필요]

```
ExportService, build.gradle 확인.

1. 의존성: implementation 'org.apache.poi:poi-ooxml:5.2.5'
2. format=xlsx 파라미터 추가
3. 헤더: 날짜|거래내용|거래처|수입금액|비용금액|고정자산|카테고리|비고
4. 스타일: 헤더 #4472C4 배경, 금액 #,##0, autoSizeColumn
5. Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

---

### #14. PDF 장부 리포트 [세법 검증 완료]

세법 근거:
- 국세청고시 제2024-19호: 간편장부 서식
- 국세기본법 제85조의3: 5년 보관 의무

```
ExportService, build.gradle 확인. ⚠3 한글 폰트 선행 필요.

1. 의존성: implementation 'com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10'
2. format=pdf 파라미터 추가
3. PDF 내용 (국세청고시 제2024-19호 준용):
   제목: "간편장부 ({year}년)"
   사업자 정보: ⚠2 위치에서 가져오기
   테이블: 일자|거래내용|거래처|수입(매출)|수입(기타)|비용(매입)|비용(경비)|고정자산(증가)|고정자산(감소)|비고
   하단 합계행
   푸터: "국세기본법 제85조의3에 따라 5년간 보관 의무"
4. 한글 폰트: resources/fonts/NotoSansKR-Regular.ttf
```

---

## C그룹: 세무리포트 — 이번 달 (#15~#18)

---

### #15. 카테고리별 비율(%) [세법 불필요]

```
BookEntryService의 summary 응답 DTO 확인.
byCategory의 CategorySummary에 percentage 필드 추가.

계산: (amount / 전체 비용 합계) × 100, 소수점 1자리.
0 나누기 방지. 수입/비용 각각 별도 기준.
```

---

### #16. 거래처별 수입 집계 [세법 불필요]

```
프로젝트에서 BookEntryService와 BookEntryRepository 확인.

새로운 엔드포인트 또는 summary 확장:
GET /api/book-entries/summary에 byMerchant 필드 추가
또는 GET /api/book-entries/merchant-summary?year=2025&month=3

응답:
{
  "byMerchant": [
    { "merchantName": "A사", "totalAmount": 3300000, "count": 1, "percentage": 45.2 },
    { "merchantName": "B사", "totalAmount": 2200000, "count": 2, "percentage": 30.1 }
  ]
}

로직:
- BookEntry에서 entryType=INCOME인 건을 merchantName으로 GROUP BY
- SUM(amount), COUNT(*) 집계
- percentage = (해당 거래처 / 전체 수입) × 100

기존 summary 패턴 참고해서 같은 방식으로.
```

---

### #17. 이번 달 세금 추정 [세법 검증 필요]

세법 근거:
- 부가세: 부가가치세법 제49조 — 과세기간(6개월) 종료 후 25일 이내 신고
  1기(1~6월) 확정: 7.25 / 2기(7~12월) 확정: 익년 1.25
  개인사업자 예정고지: 4월, 10월 (직전 납부세액의 50%)
- 종소세: 소득세법 제70조 — 5.1~5.31 확정신고
- 지방세: 지방세법 제95조 — 종소세와 동시 5.1~5.31

```
프로젝트에서 TaxEstimationService를 읽어줘. 현재 연간 단위만 지원하는 것 확인.

월별 세금 추정 기능을 추가해줘.
기존 서비스를 확장하거나 새 메서드 추가.

GET /api/tax-estimation/monthly?year=2025&month=3

응답:
{
  "year": 2025,
  "month": 3,
  "estimatedVat": {
    "period": "1기 (1~6월)",
    "salesTax": 990000,
    "purchaseTax": 398500,
    "estimatedPayable": 591500,
    "dueDate": "2025-07-25",
    "note": "현재까지 3개월분 누적. 6월까지 매출/매입에 따라 변동."
  },
  "estimatedIncomeTax": {
    "currentYearIncome": 9900000,
    "currentYearExpense": 2112500,
    "projectedAnnualIncome": 39600000,
    "projectedAnnualExpense": 8450000,
    "estimatedTax": 1520700,
    "dueDate": "2026-05-31",
    "note": "현재까지 수입/비용을 연간 환산한 추정치."
  },
  "estimatedLocalTax": {
    "amount": 282750,
    "basis": "종소세 결정세액의 10%",
    "dueDate": "2026-05-31"
  }
}

계산 로직:
1. 부가세: 해당 월이 속한 과세기간(1기: 1~6월, 2기: 7~12월)의 누적 매출세액-매입세액
2. 종소세: 현재까지 수입/비용을 (12/경과월수)로 연간 환산 → 기존 TaxEstimationService 계산 로직 재사용
3. 지방세: 종소세 결정세액 × 10%

주의: "추정치"임을 명시. 연간 환산은 단순 비례이므로 정확하지 않을 수 있음.
```

---

### #18. 이번 달 총 예상 세금 합산 [#17 선행]

```
#17의 응답에 totalEstimatedTax 필드 추가.

totalEstimatedTax = estimatedVat.estimatedPayable + estimatedIncomeTax.estimatedTax + estimatedLocalTax.amount

#17 응답에 이 필드만 추가하면 됨.
```

---

## D그룹: 세무리포트 — 연간 (#19~#23)

---

### #19. 전년 대비 비교 [세법 불필요]

```
BookEntryService의 summary 메서드 확인.

전년 대비 응답 필드 추가:
- previousYear: { totalIncome, totalExpense, netProfit }
- comparison: { incomeChangeRate, expenseChangeRate, profitChangeRate }
- 소수점 1자리. 전년 데이터 없으면 null.
```

---

### #20. 부가세 1기/2기 별도 금액 [세법 검증 완료]

세법 근거: 부가가치세법 제49조
- 1기(1~6월) 확정: 7월 25일
- 2기(7~12월) 확정: 익년 1월 25일
- 개인사업자 예정고지: 4월, 10월 (직전 50%)

```
프로젝트에서 TaxEstimationService 또는 VatReport 관련 서비스 확인.
기존 Export의 VAT 로직(half 파라미터)도 참고.

연간 세무리포트에 부가세 반기별 금액을 제공해줘.

GET /api/tax-estimation?year=2025 응답에 추가:
"vatByPeriod": {
  "period1": {
    "label": "1기 (1~6월)",
    "salesTax": 1980000,
    "purchaseTax": 797000,
    "estimatedPayable": 1183000,
    "dueDate": "2025-07-25"
  },
  "period2": {
    "label": "2기 (7~12월)",
    "salesTax": 1980000,
    "purchaseTax": 797000,
    "estimatedPayable": 1183000,
    "dueDate": "2026-01-25"
  },
  "totalVat": 2366000
}

계산: BookEntry에서 1~6월 / 7~12월 나누어 매출세액-매입세액 집계.
매출세액 = 수입금액 ÷ 1.1 × 0.1
매입세액 = 사업용 비용금액 ÷ 1.1 × 0.1 (접대비 제외 — 부가가치세법 §39①1)
```

---

### #21. 연간 총 예상 세금 합산 [#20 선행]

```
GET /api/tax-estimation 응답에 추가:
"totalAnnualTax": vatByPeriod.totalVat + incomeTax + localTax

단순 합산 필드 추가.
```

---

### #22. 다음 세율 구간까지 남은 금액 [세법 검증 완료]

세법 근거: 소득세법 제55조 (8단계 누진세율)
1,400만 이하 6% / 5,000만 이하 15% / 8,800만 이하 24% / ...

```
프로젝트에서 TaxEstimationService 확인.
현재 incomeTaxBracket(문자열)만 반환하는 것 확인.

응답에 다음 필드 추가:
"bracketDetail": {
  "currentBracket": "15%",
  "currentBracketMin": 14000000,
  "currentBracketMax": 50000000,
  "taxableIncome": 27250000,
  "amountToNextBracket": 22750000,
  "nextBracketRate": "24%",
  "message": "과세표준 2,275만원 더 늘면 24% 구간 진입"
}

계산:
1. 현재 과세표준이 어느 구간인지 확인 (TaxBracket 또는 config)
2. amountToNextBracket = 현재 구간 상한 - 현재 과세표준
3. 최고 구간(45%)이면 nextBracketRate = null, message = "최고 세율 구간"

세율표는 DB/config에서 로드 (하드코딩 금지).
```

---

### #23. 거래처별 수입 집계 (연간) [#16과 동일 로직]

```
#16에서 만든 거래처별 집계 로직에 month 파라미터를 optional로.
month가 없으면 연간 전체 집계.
추가 작업 최소화.
```

---

## E그룹: 절세 상세 (#24~#27)

---

### #24. 사업용카드 공제 추천 [세법 검증 완료]

세법 근거:
- 부가가치세법 제46조: 카드 매입세액 공제
- 불공제: 접대비(§39①1), 비영업용 소형승용차(§39①5)

```
절세 추천 서비스 확인. 기존 추천 항목 구조 확인.

추가:
- name: "사업용카드 매입세액 공제"
- 로직: 사업용 카드결제 금액 ÷ 11 = 매입세액 (접대비 제외)
- estimatedSaving = 공제 가능 매입세액
- maxAmount = null (한도 없음)
- description: "부가가치세법 §46. 접대비 관련은 불공제 (§39①1)"
```

---

### #25. 교육훈련비 공제 추천 [세법 검증 완료]

세법 근거:
- 소득세법 제19조: 사업 관련 교육비 = 필요경비
- 법적 한도 없음. 권장 150만원 (디자인 기준)
- 주의: 조특법 §104의18은 "근로자" 대상이라 1인 사업자 본인에게 직접 적용 안 됨

```
절세 추천 서비스 확인. 기존 구조대로 추가.

- name: "교육훈련비 경비 활용"
- 로직: BookEntry에서 카테고리 25(도서)+27(연구/교육) 합산
  → ⚠1 이후 TaxCategory 기준으로. enum 값 확인 필수.
- 권장 한도: 1,500,000원
- 잔여 = 1,500,000 - 사용액
- estimatedSaving = 잔여 × 현재 세율
- description: "소득세법 §19. 인프런, 기술서적, 컨퍼런스 경비 처리 가능."
  주의: "조특법 §104의18은 근로자 대상이므로 1인 사업자 본인에게 적용 안 됨" 코드 주석에 표시.
```

---

### #26. 추천 DTO에 usedAmount/remainingAmount 추가 [세법 불필요]

```
절세 추천 서비스의 응답 DTO (TaxSavingRecommendation 또는 비슷한 이름) 확인.

현재: maxAmount, estimatedSaving만 있음.
추가 필드:
- usedAmount (long) — 현재까지 사용한 금액
- remainingAmount (long) — 잔여 = maxAmount - usedAmount
- usageRate (double) — 사용률 = (usedAmount / maxAmount) × 100, 소수점 1자리

각 추천 항목의 계산:
- 노란우산공제: usedAmount = 이미 납입한 금액 (BookEntry에서 관련 항목) 또는 0 (미가입)
- 연금저축: 동일
- 사업용카드: usedAmount = 현재 카드 결제 매입세액 합산
- 교육훈련비: usedAmount = 카테고리 25+27 합산
- 접대비: usedAmount = 카테고리 17 합산, maxAmount = 12,000,000 (한도)

기존 DTO에 필드 3개만 추가. 계산 로직은 각 추천 항목 생성 시 채움.
```

---

### #27. 총 절세 한도 사용률 [#26 선행]

```
절세 추천 응답의 최상위에 합산 필드 추가:

"totalSummary": {
  "totalMaxAmount": 모든 추천 항목의 maxAmount 합산 (null 제외),
  "totalUsedAmount": 모든 추천 항목의 usedAmount 합산,
  "totalRemainingAmount": totalMaxAmount - totalUsedAmount,
  "overallUsageRate": (totalUsedAmount / totalMaxAmount) × 100
}

#26에서 추가한 필드를 합산하면 끝.
```

---

## 실행 순서

```
=== Phase 0: 선행 (30분) ===
⚠1 TaxCategory 불일치 → ⚠2 User 필드 확인 → ⚠3 폰트 배치

=== Phase 1: 장부 필터 (2시간) ===
#1 entryType → #2 날짜 → #3,#4 확인 → #6 카테고리 → #7 거래처 → #8 증빙 → #9 미분류

=== Phase 2: 거래상세+내보내기 (2시간) ===
#10 메모 → #11 지방소득세CSV → #12 날짜범위 → #13 Excel → #14 PDF

=== Phase 3: 세무리포트 이번달 (1.5시간) ===
#15 비율 → #16 거래처집계 → #17 월별세금추정 → #18 합산

=== Phase 4: 세무리포트 연간 (2시간) ===
#19 전년비교 → #20 부가세반기 → #21 합산 → #22 다음구간 → #23 거래처연간

=== Phase 5: 절세상세 (1시간) ===
#24 사업용카드 → #25 교육훈련비 → #26 DTO확장 → #27 사용률

총 예상: 약 9시간 (2~3일)
```
