# 7iTAX API 명세서

> **Base URL:** `http://localhost:8080`
> **인증 방식:** JWT Bearer Token (`Authorization: Bearer {accessToken}`)
> **응답 형식:** 모든 응답은 아래 공통 포맷으로 래핑됩니다.

```json
// 성공
{ "status": "success", "message": "OK", "data": { ... } }

// 실패
{ "status": "fail", "errorCode": "ERROR_CODE", "message": "에러 메시지", "data": null }
```

---

## 목차

1. [인증 (Auth)](#1-인증-auth)
2. [카드 (Cards)](#2-카드-cards)
3. [결제 (Payments)](#3-결제-payments)
4. [간편장부 (Book Entries)](#4-간편장부-book-entries)
5. [세금 분류 (Classification)](#5-세금-분류-classification)
6. [세금 달력 (Tax Calendar)](#6-세금-달력-tax-calendar)
7. [세금 추정 (Tax Estimation)](#7-세금-추정-tax-estimation)
8. [내보내기 (Export)](#8-내보내기-export)
9. [에러 코드](#9-에러-코드)

---

## 1. 인증 (Auth)

### 인증 플로우

```
본인인증 → PIN 설정 → (약관 동의) → PIN 로그인 → 토큰 사용
```

| 단계 | 엔드포인트 | 설명 |
|------|-----------|------|
| 1 | `POST /api/auth/verify-identity` | 이름/생년월일/휴대폰으로 본인인증 |
| 2 | `POST /api/auth/setup-pin` | 6자리 PIN 설정 (최초 1회) |
| 3 | `POST /api/auth/consents` | 약관 동의 (선택) |
| 4 | `POST /api/auth/login` | 휴대폰번호 + PIN으로 로그인 |

---

### 1.1 본인인증

> `POST /api/auth/verify-identity` | **인증 불필요**

**Request Body:**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| name | String | O | 이름 | `"홍길동"` |
| birthDate | String | O | 생년월일 (YYYY-MM-DD) | `"1990-01-01"` |
| gender | String | O | 성별 (M/F) | `"M"` |
| phoneNumber | String | O | 휴대폰번호 (하이픈 없이) | `"01012345678"` |

**Response:**

```json
{
  "status": "success",
  "data": {
    "userId": 1,
    "isNewUser": true,
    "requiresPinSetup": true,
    "requiresConsent": true
  }
}
```

| 필드 | 설명 | 프론트 처리 |
|------|------|------------|
| userId | PIN 설정 시 필요 | 저장해두기 |
| isNewUser | 신규 유저 여부 | - |
| requiresPinSetup | true면 PIN 설정 화면으로 | `→ setup-pin` |
| requiresConsent | true면 약관 동의 화면으로 | `→ consents` |

---

### 1.2 PIN 설정

> `POST /api/auth/setup-pin?userId={userId}` | **인증 불필요**

**Query Parameter:**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 본인인증에서 받은 userId |

**Request Body:**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| pin | String | O | 6자리 PIN | `"123456"` |

**Response:**

```json
{
  "status": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

> PIN 설정과 동시에 로그인 처리됩니다. 토큰을 저장하세요.

---

### 1.3 PIN 로그인

> `POST /api/auth/login` | **인증 불필요**

**Request Body:**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| phoneNumber | String | O | 휴대폰번호 | `"01012345678"` |
| pin | String | O | 6자리 PIN | `"123456"` |

**Response:** `accessToken`, `refreshToken` (1.2와 동일)

---

### 1.4 약관 동의

> `POST /api/auth/consents` | **JWT 필요**

**Request Body:** (배열)

```json
[
  { "consentType": "SERVICE", "agreed": true },
  { "consentType": "PRIVACY", "agreed": true },
  { "consentType": "FINANCIAL", "agreed": true }
]
```

| consentType | 설명 |
|-------------|------|
| SERVICE | 서비스 이용약관 |
| PRIVACY | 개인정보 처리방침 |
| FINANCIAL | 금융정보 제공 동의 |

**Response:** `{ "status": "success", "data": null }`

---

### 1.5 토큰 갱신

> `POST /api/auth/reissue` | **인증 불필요**

**Request Body:**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | String | O | 기존 리프레시 토큰 |

**Response:** 새로운 `accessToken`, `refreshToken`

---

### 1.6 로그아웃

> `POST /api/auth/logout` | **JWT 필요 (Header)**

**Request Header:**

```
Authorization: Bearer {accessToken}
```

**Response:** `{ "status": "success", "data": null }`

---

## 2. 카드 (Cards)

### 2.1 카드 등록

> `POST /api/cards` | **JWT 필요**

**Request Body:**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| cardName | String | O | 카드 이름 | `"삼성카드"` |
| cardType | String | O | `BUSINESS` 또는 `PERSONAL` | `"BUSINESS"` |
| last4Digits | String | X | 카드 뒷 4자리 | `"1234"` |

**Response:**

```json
{
  "status": "success",
  "data": {
    "id": 1,
    "cardName": "삼성카드",
    "cardType": "BUSINESS",
    "last4Digits": "1234",
    "isDefault": false
  }
}
```

---

### 2.2 카드 목록 조회

> `GET /api/cards` | **JWT 필요**

**Response:**

```json
{
  "status": "success",
  "data": [
    { "id": 1, "cardName": "삼성카드", "cardType": "BUSINESS", "last4Digits": "1234", "isDefault": true },
    { "id": 2, "cardName": "신한카드", "cardType": "PERSONAL", "last4Digits": "5678", "isDefault": false }
  ]
}
```

---

### 2.3 카드 상세 조회

> `GET /api/cards/{cardId}` | **JWT 필요**

---

### 2.4 기본 카드 설정

> `PATCH /api/cards/{cardId}/default` | **JWT 필요**

---

### 2.5 카드 삭제

> `DELETE /api/cards/{cardId}` | **JWT 필요**

---

### 2.6 카드 입금 (테스트용 충전)

> `POST /api/cards/{cardId}/deposit` | **JWT 필요**

**Request Body:**

```json
{ "amount": 1000000 }
```

**Response:**

```json
{
  "status": "success",
  "data": {
    "cardId": 1,
    "cardName": "삼성카드",
    "depositAmount": 1000000,
    "balance": 1500000
  }
}
```

---

### 2.7 카드 잔액 조회

> `GET /api/cards/{cardId}/balance` | **JWT 필요**

**Response:**

```json
{
  "status": "success",
  "data": {
    "cardId": 1,
    "cardName": "삼성카드",
    "balance": 1500000
  }
}
```

---

## 3. 결제 (Payments)

### 결제 플로우

```
authorize (승인) → capture (확정) → cancel (취소, 선택)
```

---

### 3.1 결제 승인

> `POST /api/payments/authorize` | **인증 불필요**

**Request Body:**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| cardId | Long | O | 결제할 카드 ID | `1` |
| amount | Long | O | 결제 금액 (원) | `50000` |
| currency | String | X | 통화 (기본 KRW) | `"KRW"` |
| merchantName | String | O | 가맹점명 | `"스타벅스 강남점"` |
| merchantCategoryCode | String | X | MCC 코드 | `"5814"` |
| paymentMethod | String | O | `ONLINE` 또는 `OFFLINE` | `"OFFLINE"` |
| purpose | String | O | `BUSINESS` 또는 `PERSONAL` | `"BUSINESS"` |

**Response:**

```json
{
  "status": "success",
  "data": {
    "paymentId": 1,
    "authorizationCode": "AUTH-abc123",
    "cardId": 1,
    "amount": 50000,
    "status": "AUTHORIZED",
    "purpose": "BUSINESS",
    "authorizedAt": "2026-03-19T10:30:00"
  }
}
```

---

### 3.2 결제 확정 (캡처)

> `POST /api/payments/{paymentId}/capture` | **인증 불필요**

**Request:** 없음 (path parameter만)

**Response:**

```json
{
  "status": "success",
  "data": {
    "paymentId": 1,
    "status": "CAPTURED",
    "cardDebit": {
      "cardId": 1,
      "debitedAmount": 50000,
      "remainingBalance": 950000
    },
    "capturedAt": "2026-03-19T10:31:00"
  }
}
```

> 캡처 시 카드에서 실제 출금되고, 간편장부(BookEntry)가 자동 생성됩니다.

---

### 3.3 결제 취소

> `POST /api/payments/{paymentId}/cancel` | **JWT 필요**

**Request Body:**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| cancelAmount | Long | X | 취소 금액 (미입력 시 전액) | `20000` |
| reason | String | O | 취소 사유 (500자 이내) | `"단순 변심"` |

**Response:**

```json
{
  "status": "success",
  "data": {
    "paymentId": 1,
    "status": "CAPTURED",
    "cancelledAmount": 20000,
    "refundedToCardId": 1,
    "cancelledAt": "2026-03-19T11:00:00"
  }
}
```

> 부분 취소 가능. 전액 취소 시 status가 `CANCELLED`로 변경됩니다.

---

### 3.4 결제 상세 조회

> `GET /api/payments/{paymentId}` | **JWT 필요**

**Response:**

```json
{
  "status": "success",
  "data": {
    "paymentId": 1,
    "cardId": 1,
    "amount": 50000,
    "currency": "KRW",
    "merchantName": "스타벅스 강남점",
    "merchantCategoryCode": "5814",
    "paymentMethod": "OFFLINE",
    "purpose": "BUSINESS",
    "status": "CAPTURED",
    "authorizationCode": "AUTH-abc123",
    "cancelledAmount": 0,
    "cancelReason": null,
    "authorizedAt": "2026-03-19T10:30:00",
    "capturedAt": "2026-03-19T10:31:00",
    "cancelledAt": null,
    "createdAt": "2026-03-19T10:30:00"
  }
}
```

---

## 4. 간편장부 (Book Entries)

### 4.1 장부 생성

> `POST /api/book-entries` | **JWT 필요**

**Request Body:**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| paymentId | Long | X | 연결된 결제 ID | `1` |
| entryDate | String | O | 날짜 (YYYY-MM-DD) | `"2026-03-19"` |
| description | String | X | 설명 | `"커피 구매"` |
| merchantName | String | X | 가맹점명 | `"스타벅스"` |
| entryType | String | O | `INCOME`, `EXPENSE`, `ASSET` | `"EXPENSE"` |
| amount | Long | O | 금액 | `5000` |
| isVatExempt | Boolean | X | 부가세 면제 여부 | `false` |
| categoryCode | String | X | 카테고리 코드 | `"entertainment"` |
| categoryName | String | X | 카테고리명 | `"접대비"` |
| note | String | X | 메모 | `"거래처 미팅"` |

> 결제 캡처 시 자동 생성되므로, 수동 생성은 현금 거래 등에만 사용합니다.

---

### 4.2 장부 목록 조회

> `GET /api/book-entries?confirmed={true|false}&page={0}&size={20}` | **JWT 필요**

**Query Parameters:**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| confirmed | X | 확정 여부 필터 (미입력 시 전체) |
| page | X | 페이지 번호 (0부터) |
| size | X | 페이지 크기 (기본 20) |

**Response:**

```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "paymentId": 1,
        "entryDate": "2026-03-19",
        "description": "스타벅스 강남점",
        "merchantName": "스타벅스 강남점",
        "entryType": "EXPENSE",
        "incomeAmount": 0,
        "expenseAmount": 50000,
        "fixedAssetAmount": 0,
        "vatAmount": 4545,
        "supplyPrice": 45455,
        "categoryCode": "entertainment",
        "categoryName": "접대비",
        "isBusinessExpense": true,
        "isVatDeductible": true,
        "confirmed": false,
        "confirmedAt": null,
        "note": null,
        "createdAt": "2026-03-19T10:31:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

### 4.3 장부 상세 조회

> `GET /api/book-entries/{entryId}` | **JWT 필요**

---

### 4.4 미확정 건수 조회

> `GET /api/book-entries/unconfirmed-count` | **JWT 필요**

**Response:**

```json
{ "status": "success", "data": 3 }
```

---

### 4.5 장부 확정

> `PATCH /api/book-entries/{entryId}/confirm` | **JWT 필요**

---

### 4.6 카테고리 수정

> `PATCH /api/book-entries/{entryId}/category` | **JWT 필요**

**Request Body:**

```json
{ "categoryCode": "office_supplies", "categoryName": "사무용품비" }
```

---

### 4.7 개인 경비 처리

> `PATCH /api/book-entries/{entryId}/personal` | **JWT 필요**

---

### 4.8 사업 경비 처리

> `PATCH /api/book-entries/{entryId}/business` | **JWT 필요**

---

## 5. 세금 분류 (Classification)

### 5.1 거래 세금 분류

> `POST /api/classification` | **JWT 필요**

**Request Body:**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| merchantName | String | O | 가맹점명 | `"스타벅스 강남점"` |
| mcc | String | X | MCC 코드 | `"5814"` |
| amount | Long | O | 금액 | `50000` |
| isBusinessPurpose | Boolean | X | 사업 목적 여부 | `true` |
| isClientAccompanied | Boolean | X | 거래처 동행 여부 | `true` |
| userId | Long | X | 접대비 한도 조회용 | `1` |

**Response:**

```json
{
  "status": "success",
  "data": {
    "confidence": "CONFIRMED",
    "taxCategory": "접대비",
    "vatDeductible": "공제 가능",
    "legalBasis": "소득세법 제35조",
    "remark": "거래처 동행 접대비로 분류됨",
    "entertainmentLimit": {
      "annualLimit": 36000000,
      "usedAmount": 5000000,
      "remainingAmount": 31000000,
      "isOverLimit": false
    }
  }
}
```

| confidence | 설명 | 프론트 처리 |
|------------|------|------------|
| CONFIRMED | 자동 분류 확정 | 바로 반영 |
| RECOMMENDED | 추천 (확인 필요) | 유저에게 확인 UI |
| NEEDS_CONFIRMATION | 분류 불확실 | 유저가 직접 선택 |

---

## 6. 세금 달력 (Tax Calendar)

### 6.1 세금 납부 일정 조회

> `GET /api/tax-calendar/deadlines` | **인증 불필요**

**Response:**

```json
{
  "status": "success",
  "data": [
    {
      "taxName": "부가가치세 1기 예정",
      "description": "1~3월분 부가가치세 예정신고",
      "deadline": "2026-04-25",
      "dDay": -37
    },
    {
      "taxName": "종합소득세",
      "description": "전년도 종합소득세 확정신고",
      "deadline": "2026-05-31",
      "dDay": -73
    }
  ]
}
```

> dDay가 음수면 D-N일 남음, 양수면 N일 지남.

---

## 7. 세금 추정 (Tax Estimation)

### 7.1 예상 세금 조회

> `GET /api/tax-estimation?year={year}` | **JWT 필요**

**Query Parameters:**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| year | X | 연도 (기본: 올해) |

**Response:**

```json
{
  "status": "success",
  "data": {
    "year": 2026,
    "totalIncome": 50000000,
    "totalExpense": 30000000,
    "totalAssetPurchase": 2000000,
    "taxableIncome": 20000000,
    "salesVat": 5000000,
    "purchaseVat": 3000000,
    "estimatedVat": 2000000,
    "estimatedIncomeTax": 1680000,
    "incomeTaxBracket": "15%",
    "estimatedLocalTax": 168000,
    "deductibleExpenses": 30000000,
    "taxSavingFromExpenses": 4500000
  }
}
```

---

## 8. 내보내기 (Export)

모든 내보내기 API는 **CSV 파일**을 반환합니다.

### 8.1 장부 CSV 다운로드

> `GET /api/export/book-entries?year={year}` | **JWT 필요**

### 8.2 부가세 CSV 다운로드

> `GET /api/export/vat?year={year}&half={1|2}` | **JWT 필요**

| 파라미터 | 설명 |
|---------|------|
| half=1 | 상반기 (1~6월) |
| half=2 | 하반기 (7~12월) |

### 8.3 소득세 CSV 다운로드

> `GET /api/export/income-tax?year={year}` | **JWT 필요**

> 응답 Content-Type: `text/csv; charset=UTF-8` (BOM 포함)

---

## 9. 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| INVALID_ARGUMENT | 400 | 입력값 검증 실패 |
| TOKEN_INVALID | 401 | 유효하지 않은 토큰 |
| REFRESH_TOKEN_INVALID | 401 | 유효하지 않은 리프레시 토큰 |
| TOKEN_EXPIRED | 401 | 만료된 토큰 |
| FORBIDDEN | 403 | 접근 권한 없음 |
| NOT_FOUND | 404 | 리소스 없음 |
| USER_NOT_FOUND | 404 | 사용자 없음 |
| CARD_NOT_FOUND | 404 | 카드 없음 |
| PAYMENT_NOT_FOUND | 404 | 결제 없음 |
| PIN_INVALID | 400 | PIN 불일치 |
| PIN_NOT_SET | 400 | PIN 미설정 |
| USER_SUSPENDED | 403 | 정지된 계정 |
| USER_WITHDRAWN | 403 | 탈퇴한 계정 |
| IDENTITY_VERIFICATION_FAILED | 400 | 본인인증 실패 |
| BANK_SERVICE_UNAVAILABLE | 503 | 금융망 연결 실패 |
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류 |

---

## 프론트 개발 Quick Start

### 1. 로그인

```javascript
// 1) 본인인증
const verifyRes = await fetch('/api/auth/verify-identity', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ name: '홍길동', birthDate: '1990-01-01', gender: 'M', phoneNumber: '01012345678' })
});
const { data: { userId } } = await verifyRes.json();

// 2) PIN 설정 (최초 1회) → 토큰 발급
const pinRes = await fetch(`/api/auth/setup-pin?userId=${userId}`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ pin: '123456' })
});
const { data: { accessToken: token } } = await pinRes.json();

// 3) 이후 로그인은 PIN만
const loginRes = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ phoneNumber: '01012345678', pin: '123456' })
});
```

### 2. 인증이 필요한 API 호출

```javascript
const res = await fetch('/api/cards', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

### 3. 기본 사용 시나리오

```
1. 본인인증 → PIN 설정 → 토큰 획득
2. 카드 등록 → POST /api/cards
3. 카드 입금 → POST /api/cards/{id}/deposit
4. 결제 승인 → POST /api/payments/authorize
5. 결제 확정 → POST /api/payments/{id}/capture (→ 장부 자동 생성)
6. 장부 확인 → GET /api/book-entries
7. 세금 추정 → GET /api/tax-estimation
```
