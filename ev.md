# Tax7i 프론트엔드 API 개발 가이드

## 공통 사항

### Base URL
```
{서버주소}/api
```

### 인증
`/api/auth/*`, `/api/tax-calendar/deadlines`를 제외한 모든 엔드포인트는 아래 헤더 필요:
```
Authorization: Bearer <access_token>
```

### 응답 형식

**성공 (2xx)**
```json
{ "status": "success", "message": "OK", "data": <응답 데이터> }
```

**실패 (4xx/5xx)**
```json
{ "status": "fail", "errorCode": "<에러코드>", "message": "<메시지>", "data": null }
```

### 에러 코드

| HTTP | 코드 | 설명 |
|------|------|------|
| 400 | INVALID_ARGUMENT | 잘못된 요청 |
| 400 | MISSING_FIELD | 필수 항목 누락 |
| 400 | INVALID_FORMAT | 데이터 형식 오류 |
| 400 | DUPLICATE_VALUE | 중복된 값 |
| 400 | SELF_TRANSFER_NOT_ALLOWED | 자기 자신에게 송금 불가 |
| 400 | CONSENT_REQUIRED | 필수 약관 동의 필요 |
| 400 | IDENTITY_VERIFICATION_FAILED | 본인인증 실패 |
| 400 | PIN_ATTEMPTS_EXCEEDED | PIN 입력 횟수 초과 |
| 400 | TAX_NATIONAL_FIRST | 국세 납부를 먼저 완료 필요 |
| 401 | UNAUTHORIZED | 인증 필요 |
| 401 | TOKEN_EXPIRED | 토큰 만료 |
| 401 | TOKEN_INVALID | 유효하지 않은 토큰 |
| 401 | REFRESH_TOKEN_EXPIRED | Refresh Token 만료 |
| 401 | REFRESH_TOKEN_INVALID | 유효하지 않은 Refresh Token |
| 401 | PIN_INVALID | PIN 불일치 |
| 401 | PIN_NOT_SET | PIN 미설정 |
| 403 | FORBIDDEN | 접근 권한 없음 |
| 403 | USER_SUSPENDED | 정지된 계정 |
| 403 | USER_WITHDRAWN | 탈퇴한 계정 |
| 403 | CARD_INACTIVE | 비활성 카드 |
| 404 | USER_NOT_FOUND | 사용자 없음 |
| 404 | CARD_NOT_FOUND | 카드 없음 |
| 404 | TRANSFER_NOT_FOUND | 송금 내역 없음 |
| 404 | BOOK_ENTRY_NOT_FOUND | 장부 항목 없음 |
| 404 | PAYMENT_NOT_FOUND | 결제 없음 |
| 404 | TAX_RETURN_NOT_FOUND | 신고서 없음 |
| 409 | DUPLICATE_BOOK_ENTRY | 이미 장부 생성된 결제 |
| 409 | ALREADY_CONFIRMED | 이미 확인된 전표 |
| 409 | TAX_ALREADY_SUBMITTED | 이미 제출된 신고서 |
| 409 | TAX_ALREADY_PAID | 이미 납부 완료 |
| 402 | PAYMENT_DECLINED | 결제 거절 |
| 410 | QR_TOKEN_EXPIRED | QR 결제 토큰 만료 |
| 422 | INSUFFICIENT_BALANCE | 잔액 부족 |
| 503 | BANK_SERVICE_UNAVAILABLE | 은행 서비스 일시 불가 |

---

## 1. 인증 (Auth)

### POST /api/auth/verify-identity — 본인인증
```json
// Request
{
  "name": "홍길동",
  "birthDate": "1990-01-01",
  "gender": "M",
  "phoneNumber": "01012345678"
}

// Response
{
  "userId": 1,
  "isNewUser": true,
  "requiresPinSetup": true,
  "verifyToken": "abc123..."
}
```

### POST /api/auth/setup-pin — PIN 설정
```
Header: X-Verify-Token: <verifyToken>
```
```json'/
// Request
{ "pin": "123456" }
7
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

### POST /api/auth/login — 로그인
```json
// Request
{ "phoneNumber": "01012345678", "pin": "123456" }

// Response
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

### POST /api/auth/reissue — 토큰 갱신
```json
// Request
{ "refreshToken": "eyJ..." }

// Response
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

### POST /api/auth/logout — 로그아웃
```
Header: Authorization: Bearer <accessToken>
Response: { "data": null }
```

### POST /api/auth/test-login — 테스트 로그인 (개발 환경)
```
Query: ?email=test@test.com (선택)
Response: { "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

---

## 2. 카드 (Card)

### GET /api/cards/merchants — 가맹점 목록
```json
// Response
[
  { "merchantId": 1, "merchantName": "스타벅스", "categoryId": "C01", "categoryName": "카페" }
]
```

### GET /api/cards/accounts — 내 계좌 목록
```json
// Response
[
  { "accountNo": "001234567890", "accountName": "수시입출금", "accountBalance": "1000000" }
]
```

### GET /api/cards/products — 카드 상품 목록
```json
// Response
[
  {
    "cardUniqueNo": "1001-...",
    "cardIssuerName": "싸피은행",
    "cardName": "싸피 체크카드",
    "baselinePerformance": "300000",
    "maxBenefitLimit": "50000",
    "cardDescription": "설명..."
  }
]
```

### POST /api/cards — 카드 발급
```json
// Request
{
  "cardName": "내 카드",
  "cardType": "DEBIT",       // DEBIT | CREDIT
  "cardUniqueNo": "1001-...",
  "withdrawalAccountNo": "001234567890",
  "withdrawalDate": "15"
}

// Response
{
  "id": 1,
  "cardName": "내 카드",
  "cardType": "DEBIT",
  "last4Digits": "7890",
  "isDefault": false,
  "cardExpiryDate": "2031-03-23",
  "withdrawalDate": "15"
}
```

### GET /api/cards — 내 카드 목록
```json
// Response
[
  {
    "id": 1, "cardName": "개인카드", "cardType": "DEBIT",
    "last4Digits": "7890", "isDefault": true,
    "cardExpiryDate": "2031-03-23", "withdrawalDate": "15"
  }
]
```

### GET /api/cards/{cardId} — 카드 상세

### PATCH /api/cards/{cardId}/default — 기본 카드 설정

### DELETE /api/cards/{cardId} — 카드 삭제

### POST /api/cards/{cardId}/payment — 카드 결제
```json
// Request
{ "merchantId": 1, "paymentBalance": 15000 }

// Response
{
  "transactionUniqueNo": 12345,
  "cardId": 1,
  "merchantName": "스타벅스",
  "categoryName": "카페",
  "paymentBalance": 15000,
  "transactionDate": "20260323",
  "transactionTime": "143022"
}
```

### POST /api/cards/{cardId}/payment/cancel — 결제 취소
```json
// Request
{ "transactionUniqueNo": 12345 }
```

### GET /api/cards/{cardId}/transactions — 거래 내역
```
Query: ?startDate=20260301&endDate=20260323
```
```json
// Response
[
  {
    "transactionUniqueNo": 12345,
    "transactionDate": "20260323",
    "transactionTime": "143022",
    "merchantName": "스타벅스",
    "categoryName": "카페",
    "paymentBalance": 15000,
    "status": "COMPLETED"
  }
]
```

### GET /api/cards/{cardId}/billing — 청구서 조회
```
Query: ?startMonth=2026-01&endMonth=2026-03
```
```json
// Response
[
  {
    "billingMonth": "2026-03",
    "billingAmount": 500000,
    "paidAmount": 300000,
    "unpaidAmount": 200000,
    "billingDate": "2026-03-01",
    "paymentDueDate": "2026-03-15",
    "status": "PARTIAL"
  }
]
```

---

## 3. 송금 (Transfer)

### POST /api/transfers/p2p — 사용자 간 송금
```json
// Request
{
  "senderCardId": 1,
  "receiverUserId": 2,
  "amount": 50000,
  "description": "점심값"
}

// Response
{
  "id": 1,
  "transferType": "P2P",
  "senderUserId": 1,
  "receiverUserId": 2,
  "amount": 50000,
  "status": "COMPLETED",
  "description": "점심값",
  "targetAccountNo": null,
  "createdAt": "2026-03-23T14:30:00"
}
```

### POST /api/transfers/withdraw — 계좌 출금
```json
// Request
{
  "cardId": 1,
  "targetAccountNo": "110123456789",
  "amount": 100000,
  "description": "출금"
}
```

### GET /api/transfers — 송금 내역 (페이징)
```
Query: ?page=0&size=20&sort=id,desc
```

### GET /api/transfers/{transferId} — 송금 상세

---

## 4. 결제 (Payment)

### POST /api/payments/authorize — 결제 인증 (2단계)
```json
// Request
{
  "cardId": 1,
  "amount": 30000,
  "currency": "KRW",
  "merchantId": 1,
  "merchantName": "스타벅스",
  "merchantCategoryCode": "C01",
  "paymentMethod": "CARD",       // CARD | ACCOUNT
  "purpose": "BUSINESS"          // BUSINESS | PERSONAL | TAX_PAYMENT
}

// Response
{
  "paymentId": 1,
  "authorizationCode": "AUTH-...",
  "cardId": 1,
  "amount": 30000,
  "status": "AUTHORIZED",
  "purpose": "BUSINESS",
  "authorizedAt": "2026-03-23T14:30:00"
}
```

### POST /api/payments/{paymentId}/capture — 결제 확정

### POST /api/payments/{paymentId}/cancel — 결제 취소
```json
// Request
{ "cancelAmount": 10000, "reason": "단순 변심" }
```

### GET /api/payments/{paymentId} — 결제 상세

### GET /api/payments — 결제 내역 (필터링 & 페이징)
```
Query: ?startDate=2026-03-01&endDate=2026-03-23&status=CAPTURED&page=0&size=20
```

### POST /api/payments/qr — QR 즉시 결제
```json
// Request
{
  "cardId": 1,
  "amount": 15000,
  "merchantId": 1,
  "merchantName": "스타벅스",
  "merchantCategoryCode": "C01",
  "purpose": "BUSINESS"
}
```

### POST /api/payments/qr/token — QR 토큰 생성 (201)
```json
// Response
{
  "token": "qr-token-...",
  "paymentId": 1,
  "amount": 15000,
  "payerName": "홍길동",
  "expiresAt": "2026-03-23T14:35:00"
}
```

### GET /api/payments/qr/token/{token} — QR 결제 정보 조회

### POST /api/payments/qr/token/{token}/confirm — QR 결제 확정

### GET /api/payments/qr/token/{token}/status — QR 결제 상태

### GET /api/payments/qr/token/{token}/events — QR 상태 SSE 스트림
```
Content-Type: text/event-stream
```

---

## 5. 장부 (Book Entry)

### POST /api/book-entries — 지출 항목 생성 (201)
```json
// Request
{
  "paymentId": 1,
  "entryDate": "2026-03-23",
  "description": "업무용 커피",
  "merchantName": "스타벅스",
  "entryType": "EXPENSE",        // INCOME | EXPENSE | FIXED_ASSET
  "amount": 15000,
  "isVatExempt": false,
  "categoryCode": "C01",
  "categoryName": "접대비",
  "note": "거래처 미팅"
}

// Response
{
  "id": 1,
  "paymentId": 1,
  "entryDate": "2026-03-23",
  "description": "업무용 커피",
  "merchantName": "스타벅스",
  "entryType": "EXPENSE",
  "incomeAmount": 0,
  "expenseAmount": 15000,
  "fixedAssetAmount": 0,
  "vatAmount": 1364,
  "supplyPrice": 13636,
  "categoryCode": "C01",
  "categoryName": "접대비",
  "isBusinessExpense": true,
  "isVatDeductible": true,
  "confirmed": false,
  "confirmedAt": null,
  "note": "거래처 미팅",
  "createdAt": "2026-03-23T14:30:00"
}
```

### POST /api/book-entries/income — 수입 항목 생성 (201)
```json
// Request
{
  "transactionDate": "2026-03-23",
  "description": "프리랜서 외주비",
  "amount": 3000000,
  "withholdingRate": 0.033,
  "note": "3월 외주"
}
```

### GET /api/book-entries/summary — 장부 요약 (연도별)
```
Query: ?year=2026
```
```json
// Response
{
  "year": 2026,
  "totalIncome": 30000000,
  "totalExpense": 15000000,
  "byMonth": [
    { "month": 1, "income": 3000000, "expense": 1200000 },
    { "month": 2, "income": 3000000, "expense": 1500000 }
  ],
  "byCategory": [
    { "code": "C01", "name": "접대비", "amount": 500000, "count": 12 }
  ]
}
```

### GET /api/book-entries — 장부 목록 (페이징)
```
Query: ?confirmed=false&page=0&size=20&sort=entryDate,desc
```

### GET /api/book-entries/{entryId} — 장부 상세

### GET /api/book-entries/unconfirmed-count — 미확인 항목 수
```json
// Response
{ "data": 5 }
```

### PATCH /api/book-entries/{entryId}/confirm — 확인 완료

### PATCH /api/book-entries/{entryId}/category — 세목 변경
```json
// Request
{ "categoryCode": "C02", "categoryName": "복리후생비" }
```

### PATCH /api/book-entries/{entryId}/personal — 개인용 표시

### PATCH /api/book-entries/{entryId}/business — 사업용 표시

---

## 6. 자동 분류 (Classification)

### POST /api/classification — 거래 세목 분류
```json
// Request
{
  "merchantName": "스타벅스",
  "mcc": "5812",
  "amount": 15000,
  "isBusinessPurpose": true,
  "isClientAccompanied": true,
  "userId": 1
}

// Response
{
  "confidence": "RECOMMENDED",    // CONFIRMED | RECOMMENDED | NEEDS_CONFIRMATION
  "taxCategory": "접대비",
  "vatDeductible": "공제 가능",
  "legalBasis": "법인세법 제25조",
  "remark": "거래처 동반 식사",
  "entertainmentLimit": {
    "annualLimit": 36000000,
    "usedAmount": 5000000,
    "remainingAmount": 31000000,
    "isOverLimit": false
  }
}
```

---

## 7. 세무 캘린더 (Tax Calendar)

### GET /api/tax-calendar/deadlines — 신고 기한 (인증 불필요)
```json
// Response
[
  {
    "taxName": "부가가치세 확정신고",
    "description": "1기 확정신고 및 납부",
    "deadline": "2026-07-25",
    "dDay": -124
  }
]
```

---

## 8. 세금 예측 (Tax Estimation)

### GET /api/tax-estimation — 세금 예측
```
Query: ?year=2026
```
```json
// Response
{
  "year": 2026,
  "totalIncome": 50000000,
  "totalExpense": 20000000,
  "totalAssetPurchase": 5000000,
  "taxableIncome": 30000000,
  "salesVat": 5000000,
  "purchaseVat": 2000000,
  "estimatedVat": 3000000,
  "estimatedIncomeTax": 4680000,
  "incomeTaxBracket": "15%",
  "estimatedLocalTax": 468000,
  "deductibleExpenses": 20000000,
  "taxSavingFromExpenses": 3000000
}
```

---

## 9. 세금 (Tax)

### POST /api/tax/calculate — 세금 계산
```json
// Request
{
  "taxYear": 2025,
  "prepaidTax": 500000,
  "deductions": { "국민연금": 2000000, "건강보험": 1500000 }
}

// Response
{
  "totalRevenue": 50000000,
  "totalExpense": 20000000,
  "incomeAmount": 30000000,
  "totalDeductions": 3500000,
  "taxableIncome": 26500000,
  "taxRate": 0.15,
  "calculatedTax": 2895000,
  "determinedTax": 2895000,
  "prepaidTax": 500000,
  "finalTax": 2395000,
  "localTax": 239500,
  "isRefund": false
}
```

### GET /api/tax/savings — 절세 추천
```
Query: ?taxYear=2025
```
```json
// Response
{
  "currentFinalTax": 2395000,
  "recommendations": [
    {
      "type": "DEDUCTION",
      "name": "노란우산공제",
      "description": "소기업·소상공인 공제부금",
      "maxAmount": 5000000,
      "estimatedSaving": 750000,
      "isApplied": false
    }
  ],
  "potentialTotalSaving": 750000
}
```

### POST /api/tax/returns — 종합소득세 신고서 생성
```json
// Request
{ "taxYear": 2025, "prepaidTax": 500000, "deductions": {} }
```

### GET /api/tax/returns — 신고서 목록

### GET /api/tax/returns/{id} — 신고서 상세

### PUT /api/tax/returns/{id} — 신고서 수정
```json
// Request
{ "prepaidTax": 600000, "deductions": { "국민연금": 2000000 } }
```

### POST /api/tax/returns/{id}/submit — 신고서 제출
```json
// Response
{
  "receiptNumber": "TAX-2025-...",
  "submittedAt": "2026-03-23T14:30:00",
  "status": "SUBMITTED",
  "finalTax": 2395000,
  "localTax": 239500,
  "totalPayable": 2634500,
  "nationalAccount": { "bank": "한국은행", "account": "110-xxx-xxx" },
  "localAccount": { "bank": "한국은행", "account": "120-xxx-xxx" },
  "paymentDeadline": "2026-05-31"
}
```

### GET /api/tax/returns/{id}/payment-status — 납부 상태
```json
// Response
{
  "national": { "status": "UNPAID", "amount": 2395000, "paidAt": null },
  "local": { "status": "UNPAID", "amount": 239500, "paidAt": null },
  "totalPaid": 0
}
```

### POST /api/tax/returns/{id}/pay — 국세 납부
```json
// Request
{ "cardId": 1 }

// Response
{ "paymentType": "NATIONAL", "amount": 2395000, "transferId": "TRF-...", "status": "PAID", "paidAt": "..." }
```

### POST /api/tax/returns/{id}/pay-local — 지방세 납부
```json
// Request
{ "cardId": 1 }
```
> 국세 납부 완료 후에만 가능 (TAX_NATIONAL_FIRST 에러)

### GET /api/tax/returns/{id}/pdf — 신고서 PDF 다운로드
```
Response: application/pdf (binary)
Content-Disposition: attachment; filename="tax-return-{id}.pdf"
```

### GET /api/tax/returns/{id}/receipt — 납부 영수증 PDF
```
Response: application/pdf (binary)
Content-Disposition: attachment; filename="tax-receipt-{id}.pdf"
```

---

## 10. 부가세 (VAT)

### POST /api/tax/vat-returns — 부가세 신고서 생성
```json
// Request
{ "taxYear": 2025, "taxPeriod": 1, "preliminaryPaid": 100000 }

// Response
{
  "id": 1,
  "taxYear": 2025,
  "taxPeriod": 1,
  "status": "DRAFT",
  "receiptNumber": null,
  "salesAmount": 25000000,
  "salesTax": 2500000,
  "purchaseAmount": 10000000,
  "purchaseTax": 1000000,
  "preliminaryPaid": 100000,
  "finalTax": 1400000,
  "submittedAt": null,
  "createdAt": "2026-03-23T14:30:00"
}
```

### GET /api/tax/vat-returns — 부가세 신고 목록

### GET /api/tax/vat-returns/{id} — 부가세 신고 상세

### POST /api/tax/vat-returns/{id}/submit — 부가세 신고 제출

---

## 11. 내보내기 (Export)

### GET /api/export/book-entries — 장부 CSV
```
Query: ?year=2026
Response: text/csv → 간편장부_2026.csv
```

### GET /api/export/vat — 부가세 요약 CSV
```
Query: ?year=2026&half=1
Response: text/csv → 부가세_2026_1기.csv
```

### GET /api/export/income-tax — 종합소득세 CSV
```
Query: ?year=2026
Response: text/csv → 종합소득세_2026.csv
```

---

## 주요 Enum 값 정리

| Enum | 값 |
|------|-----|
| CardType | `DEBIT`, `CREDIT` |
| TransferType | `P2P`, `WITHDRAW` |
| TransferStatus | `PENDING`, `COMPLETED`, `FAILED` |
| PaymentMethod | `CARD`, `ACCOUNT` |
| PaymentPurpose | `BUSINESS`, `PERSONAL`, `TAX_PAYMENT` |
| PaymentStatus | `AUTHORIZED`, `CAPTURED`, `CANCELLED` |
| BookEntryType | `INCOME`, `EXPENSE`, `FIXED_ASSET` |
| Confidence | `CONFIRMED`, `RECOMMENDED`, `NEEDS_CONFIRMATION` |
| TaxReturnStatus | `DRAFT`, `SUBMITTED`, `ACCEPTED` |
| TaxPayStatus | `UNPAID`, `PAID`, `PARTIAL` |

---

## 페이징 공통

페이징이 적용된 엔드포인트(`GET /api/payments`, `GET /api/transfers`, `GET /api/book-entries`)는 아래 쿼리 파라미터 사용:

```
?page=0&size=20&sort=createdAt,desc
```

응답에 포함되는 페이징 메타:
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```
