# 7iTAX 백엔드 — Claude CLI 태스크 목록

> **목적**: 이미 완성된 기능을 제외하고, 미완성 기능만 Claude CLI에 태스크로 넘겨 구현을 완성한다.
>
> **사용법**: 각 태스크를 복사하여 `claude` 명령에 전달하거나, `.claude/commands/` 에 저장하여 슬래시 커맨드로 실행한다.
>
> **필수**: `REFERENCE.md`를 프로젝트 루트에 배치한 뒤, 각 태스크 실행 시 "REFERENCE.md를 먼저 읽고 작업해줘"를 프롬프트 앞에 추가한다. 이 파일에 세법 서식 필드 구조, 세목코드 전체 매핑, 세액 계산 규칙, 시연 더미 데이터 등 코드 생성에 필요한 참조 데이터가 들어 있다.

---

## 현재 상태 요약

### ✅ 완성된 기능 (건드리지 않음)

| 도메인 | 상태 | API 수 | 비고 |
|--------|------|--------|------|
| Auth | ✅ 완료 | 6개 | PIN 인증, JWT, NICE Mock |
| Card | ✅ 완료 | 10개 | 발급, 결제, 취소, 청구서 |
| Payment | ✅ 완료 | 11개 | QR 결제, SSE 실시간 이벤트 |
| Transfer | ✅ 완료 | 4개 | P2P 송금, 출금 |
| BookEntry | ✅ 기본 완료 | 8개 | CRUD, 확인, 카테고리 변경 |
| Classification | ✅ 기본 완료 | 1개 | MCC 기반 분류 |
| Tax Calendar | ✅ 기본 완료 | 1개 | 납부 기한 조회 |
| Tax Estimation | ✅ 기본 완료 | 1개 | 예상 세금 계산 |
| Export | ✅ 완료 | 3개 | CSV 내보내기 (장부/부가세/종소세) |

### ❌ 미완성 기능 (Claude CLI로 구현해야 함)

| # | 태스크 | 우선순위 | 예상 난이도 | 의존성 |
|---|--------|----------|------------|--------|
| T1 | 세액 계산 엔진 강화 (tax_bracket config) | **MUST** | ★★★ | TaxEstimationService 확장 |
| T2 | 종합소득세 신고서 도메인 (Entity + 자동 생성) | **MUST** | ★★★ | T1 |
| T3 | 시뮬레이션 제출 + 접수번호 채번 | **MUST** | ★★☆ | T2 |
| T4 | 국세/지방세 순차 납부 | **MUST** | ★★★ | T3, Transfer 도메인 참고 |
| T5 | PDF 생성 (신고서 3종 + 납부확인서) | **MUST** | ★★☆ | T2, T4 |
| T6 | 장부 수입 등록 + 월별 집계 강화 | **SHOULD** | ★★☆ | BookEntry 도메인 확장 |
| T7 | 절세 추천 API | **SHOULD** | ★★☆ | T1 |
| T8 | 부가세 신고서 도메인 | **COULD** | ★★☆ | T1 패턴 재사용 |

---

## T1. 세액 계산 엔진 강화

### 컨텍스트
`TaxEstimationService`가 이미 존재하지만, 소득세법 제55조 8단계 누진세율을 config 기반으로 적용하는 정밀 계산 엔진이 필요하다. 세율을 하드코딩하지 않고 DB 테이블(`tax_bracket`)에서 로드해야 한다.

### Claude CLI 프롬프트

```
먼저 REFERENCE.md를 읽고, 프로젝트 구조를 파악한 뒤, 다음 작업을 수행해줘.

## 작업: 세액 계산 엔진 강화

### 1. Entity 생성: TaxBracket
위치: tax 도메인 패키지 내 entity

```java
필드:
- id (Long, PK)
- year (int) — 귀속연도
- bracketMin (long) — 구간 하한 (원)
- bracketMax (long) — 구간 상한 (원)
- rate (double) — 세율 (예: 0.15)
- progressiveDeduction (long) — 누진공제액 (원)
```

### 2. Repository 생성: TaxBracketRepository
- findByYearOrderByBracketMinAsc(int year)
- 해당 연도의 세율 구간을 정렬된 상태로 반환

### 3. Service 생성 또는 확장: TaxCalculationEngine
기존 TaxEstimationService를 확장하거나 별도 서비스로 생성.

핵심 메서드: calculate(int taxYear, long totalRevenue, long totalExpense, long prepaidTax, Map<String, Long> deductions)

계산 플로우:
1. 소득금액 = totalRevenue - totalExpense
2. 소득공제 합계 = deductions의 value 합산
3. 과세표준 = Math.max(0, 소득금액 - 소득공제합계)
4. TaxBracketRepository에서 해당 연도 구간 조회
5. 산출세액 = Math.floor(과세표준 × 세율 - 누진공제)
6. 결정세액 = Math.max(0, 산출세액 - 세액감면)
7. 최종납부세액 = 결정세액 - prepaidTax
8. 지방소득세 = Math.floor(결정세액 × 0.10)

모든 금액은 long(원 단위). float/double은 세율 계산에서만 사용하고 결과는 Math.floor()로 절사.

### 4. API 엔드포인트 확장
기존 GET /api/tax-estimation 을 확장하거나 별도로:
POST /api/tax/calculate

Request:
{
    "taxYear": 2025,
    "prepaidTax": 1306800,
    "deductions": {
        "기본공제": 1500000,
        "국민연금": 2400000
    }
}

Response:
{
    "totalRevenue": 39600000,
    "totalExpense": 8450000,
    "incomeAmount": 31150000,
    "taxableIncome": 24250000,
    "taxRate": 0.15,
    "calculatedTax": 2377500,
    "determinedTax": 2377500,
    "prepaidTax": 1306800,
    "finalTax": 1070700,
    "localTax": 237750,
    "isRefund": false
}

장부(BookEntry)에서 해당 연도의 수입/비용을 자동 집계하여 totalRevenue, totalExpense를 채운다.

### 5. DDL + 시드 데이터
tax_bracket 테이블 생성 + 2025 귀속 세율 8단계 INSERT:
(0, 14000000, 0.06, 0)
(14000001, 50000000, 0.15, 1260000)
(50000001, 88000000, 0.24, 5760000)
(88000001, 150000000, 0.35, 15440000)
(150000001, 300000000, 0.38, 19940000)
(300000001, 500000000, 0.40, 25940000)
(500000001, 1000000000, 0.42, 35940000)
(1000000001, 9999999999, 0.45, 65940000)

### 주의사항
- 기존 TaxEstimationService 코드를 먼저 확인하고, 중복되지 않게 확장 또는 교체
- 기존 프로젝트의 패키지 구조, 네이밍 컨벤션, 응답 형식을 따를 것
- @Transactional 적절히 사용
```

---

## T2. 종합소득세 신고서 도메인

### 컨텍스트
신고서를 자동 생성하고, 제출(시뮬레이션)하고, 납부까지 이어지는 핵심 플로우의 데이터 구조. 현재 존재하지 않는 완전히 새로운 도메인이다.

### Claude CLI 프롬프트

```
먼저 REFERENCE.md를 읽고, 프로젝트 구조를 파악한 뒤, 다음 작업을 수행해줘.

## 작업: 종합소득세 신고서 도메인 생성

### 1. Entity 생성

#### TaxReturn (종합소득세 신고서)
위치: tax 도메인 패키지 내 entity

```java
필드:
- id (Long, PK)
- userId (Long, NOT NULL) — User FK
- taxYear (int) — 귀속연도 (예: 2025)
- returnType (String) — "INCOME_TAX"
- status (TaxReturnStatus enum) — DRAFT / SUBMITTED / ACCEPTED / PAID / COMPLETED
- receiptNumber (String, UNIQUE) — 접수번호 (예: G2026-0305-0000001)
- submittedAt, acceptedAt (LocalDateTime)

인적사항 (User에서 복사):
- taxpayerName, residentNumber, businessNumber, businessCode, address, taxOfficeCode

소득/세액 계산 결과:
- totalRevenue (long) — 총수입금액
- totalExpense (long) — 필요경비 합계
- incomeAmount (long) — 소득금액
- totalDeductions (long) — 소득공제 합계
- taxableIncome (long) — 과세표준
- taxRate (double) — 적용 세율
- calculatedTax (long) — 산출세액
- taxDeduction (long) — 세액공제/감면
- determinedTax (long) — 결정세액
- prepaidTax (long) — 기납부세액 (3.3%)
- finalTax (long) — 납부(환급)할 세액
- localTax (long) — 지방소득세 (결정세액 × 10%)
- deductionsJson (String, JSONB) — 소득공제 상세

- version (Long) — @Version Optimistic Lock
- createdAt, updatedAt
```

#### ExpenseDetail (필요경비 명세)
```java
필드:
- id (Long, PK)
- taxReturnId (Long, FK → TaxReturn)
- expenseCode (String) — 세목코드 (23, 21, 16 등)
- expenseName (String) — 세목명
- amount (long) — 해당 세목 합계
```

#### TaxReturnStatus (enum)
```java
DRAFT, SUBMITTED, ACCEPTED, PAID, COMPLETED

상태 전이 검증 메서드:
DRAFT → SUBMITTED (only)
SUBMITTED → ACCEPTED (only)
ACCEPTED → PAID (only)
PAID → COMPLETED (only)
```

### 2. Repository 생성
- TaxReturnRepository
  - findByIdAndUserId(Long id, Long userId)
  - findByUserIdAndTaxYear(Long userId, int taxYear)
  - existsByUserIdAndTaxYearAndStatusNot(Long userId, int taxYear, TaxReturnStatus status)
- ExpenseDetailRepository
  - findByTaxReturnId(Long taxReturnId)

### 3. Service: TaxReturnService

#### 신고서 자동 생성 메서드: createDraft(Long userId, int taxYear)
1. 중복 체크: 해당 연도에 이미 DRAFT 이상 신고서가 있으면 예외
2. BookEntry에서 해당 연도 수입/비용 집계 (기존 BookEntryService 활용)
3. T1의 TaxCalculationEngine으로 세액 계산
4. User 정보 조회하여 인적사항 복사
5. TaxReturn INSERT (status: DRAFT)
6. BookEntry의 세목별 합계 → ExpenseDetail INSERT
7. 결과 반환

#### 신고서 조회: getReturn(Long userId, Long returnId)
- findByIdAndUserId로 조회 (다른 사용자 접근 차단)
- ExpenseDetail도 함께 반환

#### 신고서 수정: updateReturn(Long userId, Long returnId, UpdateRequest)
- DRAFT 상태에서만 수정 가능
- prepaidTax, deductions 수정 → 세액 재계산

### 4. API 엔드포인트

POST /api/tax/returns — 신고서 생성 (DRAFT)
  Request: { "taxYear": 2025, "prepaidTax": 1306800, "deductions": {...} }
  Response: TaxReturn 전체 데이터

GET /api/tax/returns?taxYear=2025 — 신고서 목록 조회

GET /api/tax/returns/{id} — 신고서 상세 조회
  Response: TaxReturn + ExpenseDetail 리스트

PUT /api/tax/returns/{id} — 신고서 수정 (DRAFT만)
  Request: { "prepaidTax": ..., "deductions": {...} }

### 5. DDL
TaxReturn, ExpenseDetail 테이블 생성.
인덱스: idx_taxreturn_user_year ON tax_return(user_id, tax_year, status)
유니크: uk_taxreturn_receipt ON tax_return(receipt_number)

### 주의사항
- 기존 BookEntryService를 호출하여 수입/비용을 집계한다. BookEntry 테이블을 직접 쿼리하지 말고 기존 서비스 레이어를 활용.
- 기존 User entity에서 인적사항을 가져온다.
- 기존 프로젝트의 응답 형식(ApiResponse 등)을 따른다.
- 모든 금액은 long(원 단위).
- @Transactional 필수.
```

---

## T3. 시뮬레이션 제출 + 접수번호 채번

### 컨텍스트
T2에서 만든 TaxReturn의 status를 DRAFT → ACCEPTED로 변경하고 접수번호를 채번하는 로직. 접수번호는 Redis INCR로 원자적으로 채번해야 동시 요청 시 중복이 발생하지 않는다.

### Claude CLI 프롬프트

```
먼저 REFERENCE.md를 읽고, 프로젝트 구조를 파악한 뒤, 다음 작업을 수행해줘.

## 작업: 시뮬레이션 제출 + 접수번호 채번

### 전제: T2에서 만든 TaxReturn, TaxReturnService가 이미 존재

### 1. TaxReturnService에 메서드 추가: submit(Long userId, Long returnId)

로직:
1. TaxReturn 조회 (findByIdAndUserId)
2. status 검증: DRAFT가 아니면 → ErrorCode.TAX_001 ("이미 제출된 신고서입니다")
3. 세액 최종 계산 (TaxCalculationEngine.calculate)
4. 접수번호 채번:
   - Redis key: "receipt:seq:{taxYear+1}:{taxOfficeCode}"
   - Redis INCR로 시퀀스 증가 (원자적)
   - 형식: G{year}-{taxOfficeCode}-{7자리 시퀀스}
   - 예: G2026-0305-0000001
5. status 변경: DRAFT → ACCEPTED (SUBMITTED를 거쳐 바로 ACCEPTED — 시뮬레이션이므로)
6. submittedAt, acceptedAt = now()
7. TaxPayment 2건 생성:
   - NATIONAL: amount = finalTax, virtualAccount = "880-{taxOfficeCode}-{랜덤8자리}"
   - LOCAL: amount = localTax, virtualAccount = "770-6200-{랜덤8자리}"
   - 둘 다 status = PENDING
8. 결과 반환: 접수번호, 세액 요약, 가상계좌 정보, 납부기한

### 2. Entity 생성: TaxPayment (납부 내역)
```java
필드:
- id (Long, PK)
- taxReturnId (Long, FK)
- paymentType (PaymentType enum) — NATIONAL / LOCAL
- amount (long)
- virtualAccount (String) — 가상계좌번호
- virtualBank (String) — "싸피뱅크"
- fromAccount (String) — 출금 계좌 (납부 시 채워짐)
- transferId (String) — SSAFY 금융망 거래ID
- status (PaymentStatus enum) — PENDING / PROCESSING / COMPLETED / FAILED
- errorMessage (String)
- paidAt (LocalDateTime)
- createdAt
```

유니크 제약: (taxReturnId, paymentType) — 이중 납부 방지

### 3. API 엔드포인트
POST /api/tax/returns/{id}/submit — 신고서 제출

Response:
{
    "receiptNumber": "G2026-0305-0000001",
    "submittedAt": "2026-05-15T14:30:00+09:00",
    "status": "ACCEPTED",
    "finalTax": 1070700,
    "localTax": 237750,
    "totalPayable": 1308450,
    "nationalAccount": { "bank": "싸피뱅크", "account": "880-0305-12345678" },
    "localAccount": { "bank": "싸피뱅크", "account": "770-6200-87654321" },
    "paymentDeadline": "2026-05-31"
}

### 4. Redis 사용
기존 프로젝트의 RedisTemplate 또는 Redis 설정을 확인하고 활용.
increment() 메서드로 원자적 채번.

### 주의사항
- DB의 MAX(seq)+1 방식 절대 사용 금지 (동시 요청 시 중복 발생)
- @Transactional 필수
- TaxPayment의 (taxReturnId, paymentType) UNIQUE 제약 반드시 추가
- 기존 프로젝트의 Redis 설정/사용 패턴을 먼저 확인하고 따를 것
```

---

## T4. 국세/지방세 순차 납부

### 컨텍스트
T3에서 만든 TaxPayment의 status를 변경하면서 SSAFY 금융망 API로 실제 이체를 수행한다. 기존 TransferService/SsafyFinanceClient를 참고하되, 세금 납부 전용 로직(이중 납부 방지, 국세→지방세 순차)을 추가한다.

### Claude CLI 프롬프트

```
먼저 REFERENCE.md를 읽고, 프로젝트 구조를 파악한 뒤, 다음 작업을 수행해줘.

## 작업: 국세/지방세 순차 납부

### 전제
- T3에서 만든 TaxPayment entity가 존재
- 기존 TransferService, SsafyFinanceClient가 존재 — 이체 로직 참고

### 1. Service 생성: TaxPaymentService

#### payNationalTax(Long userId, Long returnId, Long fromAccountId)
1. TaxReturn 조회 + userId 검증
2. TaxPayment 조회 (returnId + NATIONAL) + 비관적 락 (SELECT FOR UPDATE 또는 @Lock(PESSIMISTIC_WRITE))
3. status 검증: PENDING이 아니면 → TAX_002 ("이미 납부 완료")
4. status → PROCESSING (이중 호출 방지)
5. 기존 SsafyFinanceClient 또는 TransferService의 이체 로직 활용:
   - from: 사용자의 계좌번호 (fromAccountId로 조회)
   - to: TaxPayment.virtualAccount (880-XXXX)
   - amount: TaxPayment.amount
   - memo: "{taxYear}귀속_종합소득세"
6. 성공 시:
   - status → COMPLETED, paidAt = now(), transferId = 응답 거래ID
   - TaxReturn.status 업데이트 확인 (아직 PAID 아님 — 지방세 남음)
7. 실패 시:
   - status → FAILED, errorMessage = 에러메시지
   - 예외 throw

#### payLocalTax(Long userId, Long returnId, Long fromAccountId)
1. 국세 납부 완료 확인:
   - TaxPayment(returnId + NATIONAL).status == COMPLETED 인지 검증
   - 아니면 → TAX_004 ("국세 납부를 먼저 완료해주세요")
2. 이후 로직은 국세와 동일 (to: 770-XXXX, memo: "개인지방소득세")
3. 성공 시 TaxReturn.status → COMPLETED (국세+지방세 모두 완료)

### 2. Repository
- TaxPaymentRepository
  - findByTaxReturnIdAndPaymentType(Long returnId, PaymentType type)
  - @Lock(LockModeType.PESSIMISTIC_WRITE) 적용한 조회 메서드

### 3. API 엔드포인트

POST /api/tax/returns/{id}/pay — 국세 납부
  Request: { "fromAccountId": 1 }
  Response: { paymentType, amount, transferId, paidAt, status }

POST /api/tax/returns/{id}/pay-local — 지방세 납부
  Request: { "fromAccountId": 1 }
  Response: 동일 구조

GET /api/tax/returns/{id}/payment-status — 납부 상태 조회
  Response: { national: { status, amount, paidAt }, local: { status, amount, paidAt }, totalPaid }

### 주의사항
- 기존 Transfer 도메인의 SsafyFinanceClient를 확인하고 이체 메서드를 재사용
- 기존 프로젝트에서 @Lock 사용 패턴이 있으면 따르고, 없으면 @Lock(LockModeType.PESSIMISTIC_WRITE)를 Repository 메서드에 적용
- @Transactional 필수
- 국세와 지방세는 절대 합산하여 한 번에 이체하지 않음 (별도 가상계좌)
```

---

## T5. PDF 생성 (신고서 3종 + 납부확인서)

### 컨텍스트
기존 ExportService가 CSV 내보내기를 하고 있으므로, PDF는 별도 서비스로 만든다. Thymeleaf HTML 템플릿 → OpenPDF 변환 방식.

### Claude CLI 프롬프트

```
먼저 REFERENCE.md를 읽고, 프로젝트 구조를 파악한 뒤, 다음 작업을 수행해줘.

## 작업: PDF 생성 서비스

### 1. 의존성 추가 (build.gradle)
- com.openhtmltopdf:openhtmltopdf-pdfbox (HTML→PDF 변환)
- 또는 com.github.librepdf:openpdf + Thymeleaf
- 기존 프로젝트에 Thymeleaf가 있으면 활용, 없으면 openhtmltopdf 단독 사용

### 2. Service 생성: PdfService

#### generateTaxReturnPdf(Long taxReturnId) → byte[]
TaxReturn + ExpenseDetail 데이터를 HTML 템플릿에 바인딩하여 PDF 생성.

PDF에 포함할 내용 (신고서 3종 통합):
- 페이지 1: 종합소득세 확정신고서 (별지 제40(1)호 구조)
  - 인적사항 (성명, 주민번호, 사업자번호, 주소)
  - 사업소득명세 (업종코드 722000, 총수입, 소득금액)
  - 세액계산 (과세표준, 세율, 산출세액, 결정세액, 기납부세액, 최종세액)
- 페이지 2: 총수입금액 및 필요경비명세서 (별지 제74호 부표)
  - 총수입금액 항목
  - 필요경비 세목별 (코드, 세목명, 금액) — ExpenseDetail에서
- 페이지 3: 간편장부소득금액계산서 (별지 제74호)
  - 총수입 - 필요경비 = 차감소득금액 → 세무조정 → 소득금액

#### generateReceiptPdf(Long taxReturnId) → byte[]
납부확인서 PDF 생성. TaxReturn + TaxPayment 데이터 사용.

반드시 포함:
- 납세자 인적사항
- 국세 납부 내역 (세목, 귀속연도, 납부일, 금액, 거래ID)
- 지방세 납부 내역
- 총 납부 세액
- ⚠ "본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며, 실제 납세증빙으로 사용할 수 없습니다."
- 발급일자, 발급번호

### 3. HTML 템플릿
resources/templates/pdf/ 폴더에:
- tax-return.html — 신고서 3종 통합
- tax-receipt.html — 납부확인서

한글 폰트: NotoSansKR-Regular.ttf를 resources/fonts/에 배치.
CSS에서 @font-face로 지정.

### 4. API 엔드포인트

GET /api/tax/returns/{id}/pdf — 신고서 PDF 다운로드
  Response: application/pdf

GET /api/tax/returns/{id}/receipt — 납부확인서 PDF 다운로드
  Response: application/pdf

Controller에서:
```java
@GetMapping("/{id}/pdf")
public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
    byte[] pdf = pdfService.generateTaxReturnPdf(id);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tax-return.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
}
```

### 주의사항
- 기존 ExportService의 패턴(CSV 내보내기)을 참고하되, PDF는 별도 서비스
- 한글 폰트 임베딩이 안 되면 PDF에서 한글이 깨짐 — 반드시 폰트 파일 포함
- userId 검증 필수 (다른 사용자의 신고서 PDF 다운로드 불가)
```

---

## T6. 장부 수입 등록 + 월별 집계 강화

### 컨텍스트
현재 BookEntry에 수입(매출) 전용 등록 API가 없다. 프리랜서 용역대금 등 수입을 등록하고, 월별 세목별 집계 테이블을 추가하여 신고서 생성 시 쿼리 성능을 높인다.

### Claude CLI 프롬프트

```
먼저 REFERENCE.md를 읽고, 프로젝트 구조를 파악한 뒤, 다음 작업을 수행해줘.

## 작업: 장부 수입 등록 + 월별 집계

### 전제: 기존 BookEntry, BookEntryService가 존재

### 1. 수입 등록 API 추가

POST /api/book-entries/income

Request:
{
    "transactionDate": "2026-01-15",
    "description": "A사 프리랜서 용역대금",
    "amount": 3300000,
    "withholdingRate": 3.3,
    "note": "프리랜서 개발 외주"
}

로직:
1. BookEntry INSERT (entryType: INCOME)
2. 원천징수 금액 계산: amount × withholdingRate / 100 → note에 추가 기록
3. 월별 집계 갱신

기존 BookEntryService에 메서드 추가하거나, Controller에 엔드포인트 추가.

### 2. Entity 생성: MonthlyExpenseSummary (월별 집계)
```java
필드:
- id (Long, PK)
- userId (Long)
- year (int)
- month (int)
- expenseCode (String)
- totalAmount (long)
- count (int)
UNIQUE: (userId, year, month, expenseCode)
```

### 3. 집계 갱신 로직
BookEntry가 INSERT/UPDATE/DELETE 될 때마다 MonthlyExpenseSummary를 UPSERT.
기존 BookEntryService의 생성/수정 메서드에 갱신 로직 추가.

### 4. 집계 조회 API
GET /api/book-entries/summary?year=2025

Response:
{
    "year": 2025,
    "totalIncome": 39600000,
    "totalExpense": 8450000,
    "byMonth": [
        { "month": 1, "income": 3300000, "expense": 850000 },
        ...
    ],
    "byCategory": [
        { "code": "21", "name": "지급수수료", "amount": 2040000, "count": 24 },
        { "code": "23", "name": "복리후생비", "amount": 1200000, "count": 150 },
        ...
    ]
}

### 주의사항
- 기존 BookEntry entity 구조를 먼저 확인하고, 수입(INCOME) 타입이 이미 존재하는지 체크
- 기존 EntryType enum에 INCOME이 있으면 활용, 없으면 추가
- 기존 서비스의 패턴(응답 형식, 예외 처리)을 따를 것
```

---

## T7. 절세 추천 API

### 컨텍스트
장부 데이터를 분석하여 적용 가능한 소득공제/세액감면 항목을 추천한다. T1의 TaxCalculationEngine을 활용하여 적용 전/후 세액 차이를 보여준다.

### Claude CLI 프롬프트

```
먼저 REFERENCE.md를 읽고, 프로젝트 구조를 파악한 뒤, 다음 작업을 수행해줘.

## 작업: 절세 추천 API

### 1. Service 생성: TaxSavingService

#### getRecommendations(Long userId, int taxYear) → List<TaxSavingRecommendation>

로직:
1. BookEntry에서 해당 연도 수입/비용 집계
2. 현재 적용 중인 소득공제 확인
3. 추천 항목 생성:
   - 노란우산공제: 미적용 시 → "가입하면 연 최대 500만원 소득공제. 예상 절세액: XX원"
   - 국민연금 추가납입: 여유 한도 있으면 → "추가 납입 시 XX원 절세"
   - 연금저축: 미적용 시 → "연 400만원 한도 세액공제 13.2%"
   - 기장세액공제: 간편장부→복식부기 전환 시 20% (한도 100만원)
   - 접대비 한도 여유: 현재 사용액 vs 한도(1200만원) 비교
4. 각 추천별로 TaxCalculationEngine으로 적용 전/후 세액 차이 계산

### 2. DTO
```java
TaxSavingRecommendation:
- type (String) — "DEDUCTION" / "CREDIT"
- name (String) — "노란우산공제"
- description (String)
- maxAmount (long) — 최대 적용 금액
- estimatedSaving (long) — 예상 절세액
- isApplied (boolean) — 이미 적용 중인지
```

### 3. API
GET /api/tax/savings?taxYear=2025

Response:
{
    "currentTax": 1070700,
    "recommendations": [
        {
            "type": "DEDUCTION",
            "name": "노란우산공제",
            "description": "소기업·소상공인 공제부금. 가입 시 연 최대 500만원 소득공제.",
            "maxAmount": 5000000,
            "estimatedSaving": 450000,
            "isApplied": false
        }
    ],
    "potentialTotalSaving": 750000
}

### 주의사항
- 기존 EntertainmentLimitService가 접대비 한도 관련 서비스로 보임 — 확인 후 활용
- 추천 항목은 하드코딩이 아니라 config 또는 DB에서 관리하면 좋지만, 시간상 enum/상수로 해도 OK
- 기존 TaxCalculationEngine(T1)을 호출하여 세액 차이를 계산
```

---

## T8. 부가세 신고서 도메인 (COULD)

### 컨텍스트
기존 Export에 부가세 CSV가 있으므로 데이터 집계 로직은 일부 존재할 수 있다. 부가세 신고서 Entity와 제출 플로우를 T2~T3 패턴으로 추가한다.

### Claude CLI 프롬프트

```
먼저 REFERENCE.md를 읽고, 프로젝트 구조를 파악한 뒤, 다음 작업을 수행해줘.

## 작업: 부가세 신고서 도메인

### 전제: T2~T3 패턴을 따름. 기존 ExportService의 VAT 로직 참고.

### 1. Entity: VatReturn
```java
필드:
- id (Long, PK)
- userId (Long)
- taxYear (int)
- taxPeriod (int) — 1(1기) / 2(2기)
- periodType (String) — CONFIRMED(확정) / PRELIMINARY(예정고지)
- status (String) — DRAFT / SUBMITTED / ACCEPTED
- receiptNumber (String)
- salesAmount (long) — 매출 공급가액
- salesTax (long) — 매출세액 (10%)
- purchaseAmount (long) — 매입 공급가액
- purchaseTax (long) — 매입세액
- preliminaryPaid (long) — 예정고지 납부분
- finalTax (long) — 차감 납부세액
- submittedAt, createdAt
```

### 2. Service: VatReturnService
- 매출: BookEntry에서 INCOME 타입 합산 → salesAmount
- 매입: BookEntry에서 EXPENSE 타입 중 매입세액 공제 대상 합산 → purchaseAmount
- 세액 계산: salesTax - purchaseTax - preliminaryPaid = finalTax

### 3. API
POST /api/tax/vat-returns — 부가세 신고서 생성
GET /api/tax/vat-returns?taxYear=2025&period=1 — 조회
POST /api/tax/vat-returns/{id}/submit — 제출

### 주의사항
- 기존 ExportService의 VAT 내보내기 로직을 확인하고 집계 로직 재사용
- T2~T3의 패턴(접수번호 채번, 상태 머신)을 동일하게 적용
```

---

## 실행 순서

```
T1 (세액 계산 엔진) ← 독립, 먼저 해야 함
    ↓
T2 (신고서 도메인) ← T1 의존
    ↓
T3 (시뮬레이션 제출) ← T2 의존
    ↓
T4 (납부) ← T3 의존
    ↓
T5 (PDF 생성) ← T2, T4 의존

T6 (장부 강화) ← 독립, 아무 때나 가능
T7 (절세 추천) ← T1 의존
T8 (부가세) ← T1 패턴 참고, 독립
```

### 추천 분담 (기택 + 주헌)

```
기택: T1 → T2 → T3 (세액 계산 → 신고서 → 제출)
주헌: T6 → T4 → T5 (장부 강화 → 납부 → PDF)
둘 다 끝나면: T7, T8 (절세, 부가세)
```
