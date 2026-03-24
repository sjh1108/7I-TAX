# Payment API 명세서

## 개요

Tax7i 결제 시스템은 **2단계 결제(Authorize → Capture)** 패턴과 **QR 결제(CPM/MPM)** 를 지원합니다.
SSAFY 금융망 신용카드 API와 연동하여 실제 카드 거래를 처리하며, 결제된 거래는 자동으로 세금 분류 및 장부 기록에 활용됩니다.

### 결제 상태 흐름

```
AUTHORIZED → CAPTURED → CANCELLED
                ↘ DECLINED
```

| 상태 | 설명 |
|------|------|
| `AUTHORIZED` | 결제 승인됨 (아직 금융망에 전송 전) |
| `CAPTURED` | 결제 확정됨 (금융망 거래 완료) |
| `CANCELLED` | 결제 취소됨 |
| `DECLINED` | 결제 거절됨 (금융망 오류 등) |

### Enum 값 참조

| Enum | 값 | 설명 |
|------|----|------|
| PaymentMethod | `ONLINE`, `OFFLINE` | 온라인 결제 / 오프라인(QR) 결제 |
| PaymentPurpose | `PERSONAL`, `BUSINESS` | 개인 지출 / 사업 지출 |

---

## 1. 결제 승인 요청 (Authorize)

> **왜 필요한가:** 결제를 바로 확정하지 않고 먼저 승인 상태로 두어, 사용자가 결제 내용을 확인한 뒤 확정(Capture)할 수 있게 합니다. 온라인 결제 시 "결제 미리보기 → 최종 결제" 같은 2단계 흐름을 구현합니다.

```
POST /api/payments/authorize
Authorization: Bearer {accessToken}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| cardId | Long | O | 결제할 카드 ID |
| amount | Long | O | 결제 금액 (0보다 커야 함) |
| currency | String | X | 통화 (기본값: `KRW`) |
| merchantId | Long | O | 가맹점 ID |
| merchantName | String | O | 가맹점명 |
| merchantCategoryCode | String | X | MCC 코드 (세금 분류에 사용) |
| paymentMethod | PaymentMethod | O | `ONLINE` / `OFFLINE` |
| purpose | PaymentPurpose | O | `PERSONAL` / `BUSINESS` |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "authorizationCode": "A1B2C3D4",
    "cardId": 1,
    "amount": 50000,
    "status": "AUTHORIZED",
    "purpose": "BUSINESS",
    "authorizedAt": "2026-03-23T14:30:00"
  }
}
```

---

## 2. 결제 확정 (Capture)

> **왜 필요한가:** 승인된 결제를 최종 확정합니다. 이 시점에 SSAFY 금융망에 실제 카드 거래가 전송됩니다. 금융망 호출 실패 시 결제는 `DECLINED` 상태로 전환됩니다.

```
POST /api/payments/{paymentId}/capture
Authorization: Bearer {accessToken}
```

### Path Parameter

| 필드 | 타입 | 설명 |
|------|------|------|
| paymentId | Long | 승인된 결제 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "status": "CAPTURED",
    "cardId": 1,
    "amount": 50000,
    "capturedAt": "2026-03-23T14:31:00"
  }
}
```

### 에러 케이스

| 상황 | 에러 |
|------|------|
| AUTHORIZED가 아닌 결제 확정 시도 | `승인 상태의 결제만 확정할 수 있습니다.` |
| 금융망 거래 실패 | 결제 DECLINED 처리 후 예외 전파 |

---

## 3. 결제 취소 (Cancel)

> **왜 필요한가:** 확정된 결제를 취소합니다. 부분 취소를 지원하여, 전체 금액 중 일부만 취소할 수도 있습니다. 전액 취소 시 상태가 `CANCELLED`로 변경됩니다. SSAFY 금융망에도 거래 삭제 요청이 전송됩니다.

```
POST /api/payments/{paymentId}/cancel
Authorization: Bearer {accessToken}
```

### Path Parameter

| 필드 | 타입 | 설명 |
|------|------|------|
| paymentId | Long | 취소할 결제 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| cancelAmount | Long | X | 취소 금액 (미입력 시 전액 취소) |
| reason | String | O | 취소 사유 (500자 이내) |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "status": "CANCELLED",
    "cancelledAmount": 50000,
    "cardId": 1,
    "cancelledAt": "2026-03-23T15:00:00"
  }
}
```

### 에러 케이스

| 상황 | 에러 |
|------|------|
| CAPTURED가 아닌 결제 취소 시도 | `확정된 결제만 취소할 수 있습니다.` |
| 취소 금액 > 남은 결제 금액 | `취소 금액이 남은 결제 금액을 초과할 수 없습니다.` |

---

## 4. 결제 상세 조회

> **왜 필요한가:** 특정 결제의 전체 상세 정보를 조회합니다. 결제 상태, 금액, 가맹점 정보, 취소 내역 등 모든 정보를 한번에 확인할 수 있습니다.

```
GET /api/payments/{paymentId}
Authorization: Bearer {accessToken}
```

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "cardId": 1,
    "amount": 50000,
    "currency": "KRW",
    "merchantName": "스타벅스 강남점",
    "merchantCategoryCode": "5812",
    "paymentMethod": "OFFLINE",
    "purpose": "BUSINESS",
    "status": "CAPTURED",
    "authorizationCode": "A1B2C3D4",
    "cancelledAmount": null,
    "cancelReason": null,
    "authorizedAt": "2026-03-23T14:30:00",
    "capturedAt": "2026-03-23T14:31:00",
    "cancelledAt": null,
    "createdAt": "2026-03-23T14:30:00"
  }
}
```

---

## 5. 결제 내역 목록 조회

> **왜 필요한가:** 사용자의 결제 내역을 기간별/상태별로 필터링하여 조회합니다. 페이징을 지원하여 대량 데이터도 효율적으로 처리합니다. 장부 작성, 세금 신고 자료 조회 등에 활용됩니다.

```
GET /api/payments
Authorization: Bearer {accessToken}
```

### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| startDate | LocalDate | X | 조회 시작일 (ISO: `2026-01-01`) |
| endDate | LocalDate | X | 조회 종료일 (ISO: `2026-03-31`) |
| status | PaymentStatus | X | 필터할 상태 (`AUTHORIZED` / `CAPTURED` / `CANCELLED` / `DECLINED`) |
| page | int | X | 페이지 번호 (기본: 0) |
| size | int | X | 페이지 크기 (기본: 20) |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [ /* PaymentDetailResponse 배열 */ ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0,
    "size": 20
  }
}
```

---

## 6. QR 결제 (CPM - 소비자 제시형)

> **왜 필요한가:** 오프라인 매장에서 가맹점 QR을 스캔하여 즉시 결제합니다. 승인과 확정을 한 번에 처리하므로 사용자 입장에서 원터치 결제가 됩니다. 결제 동시에 SSAFY 금융망에 카드 거래가 생성됩니다.

```
POST /api/payments/qr
Authorization: Bearer {accessToken}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| cardId | Long | O | 결제할 카드 ID |
| amount | Long | O | 결제 금액 |
| merchantId | Long | O | 가맹점 ID |
| merchantName | String | O | 가맹점명 |
| merchantCategoryCode | String | X | MCC 코드 |
| purpose | PaymentPurpose | O | `PERSONAL` / `BUSINESS` |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "paymentId": 2,
    "authorizationCode": "E5F6G7H8",
    "merchantName": "이디야커피 역삼점",
    "amount": 4500,
    "status": "CAPTURED",
    "purpose": "BUSINESS",
    "capturedAt": "2026-03-23T14:35:00"
  }
}
```

---

## 7. QR 토큰 생성 (MPM - 가맹점 제시형)

> **왜 필요한가:** 결제자가 QR 토큰을 생성하면, 상대방(가맹점/수금자)이 해당 QR을 스캔하여 결제를 확인합니다. P2P 거래, 대면 결제 등에 사용됩니다. 토큰은 Redis에 저장되며 **5분 후 만료**됩니다.

```
POST /api/payments/qr/token
Authorization: Bearer {accessToken}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| cardId | Long | O | 결제할 카드 ID |
| amount | Long | O | 결제 금액 |
| merchantId | Long | O | 가맹점 ID |
| merchantName | String | O | 가맹점명 |
| merchantCategoryCode | String | X | MCC 코드 |
| purpose | PaymentPurpose | O | `PERSONAL` / `BUSINESS` |

### Response (201 Created)

```json
{
  "success": true,
  "data": {
    "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "paymentId": 3,
    "amount": 30000,
    "payerName": "박기택",
    "expiresAt": "2026-03-23T14:40:00"
  }
}
```

---

## 8. QR 결제 정보 조회

> **왜 필요한가:** QR을 스캔한 상대방이 결제 정보(결제자, 금액, 가맹점 등)를 확인합니다. 결제 확인(confirm) 전에 내용을 검증하는 단계입니다.

```
GET /api/payments/qr/token/{token}
```

### Path Parameter

| 필드 | 타입 | 설명 |
|------|------|------|
| token | String | QR 토큰 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "payerName": "박기택",
    "amount": 30000,
    "merchantName": "점심식사",
    "purpose": "BUSINESS",
    "status": "AUTHORIZED",
    "createdAt": "2026-03-23T14:35:00"
  }
}
```

---

## 9. QR 결제 확인 (Confirm)

> **왜 필요한가:** QR을 스캔한 상대방이 결제를 최종 확인합니다. 이 시점에 SSAFY 금융망에 실제 카드 거래가 전송됩니다. 결제 완료 시 SSE로 결제자에게 실시간 알림이 전송됩니다.

```
POST /api/payments/qr/token/{token}/confirm
Authorization: Bearer {accessToken}
```

### Path Parameter

| 필드 | 타입 | 설명 |
|------|------|------|
| token | String | QR 토큰 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "paymentId": 3,
    "authorizationCode": "I9J0K1L2",
    "merchantName": "점심식사",
    "amount": 30000,
    "status": "CAPTURED",
    "purpose": "BUSINESS",
    "capturedAt": "2026-03-23T14:36:00"
  }
}
```

---

## 10. QR 결제 상태 조회

> **왜 필요한가:** 결제자가 자신이 생성한 QR 토큰의 결제 처리 상태를 폴링 방식으로 확인합니다. SSE를 사용하지 않는 클라이언트를 위한 대체 수단입니다.

```
GET /api/payments/qr/token/{token}/status
Authorization: Bearer {accessToken}
```

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "status": "CAPTURED",
    "paymentId": 3,
    "capturedAt": "2026-03-23T14:36:00"
  }
}
```

---

## 11. QR 결제 실시간 이벤트 (SSE)

> **왜 필요한가:** 결제자가 QR 토큰을 생성한 후, 상대방이 결제를 확인할 때까지 실시간으로 대기합니다. SSE(Server-Sent Events)를 통해 결제 완료/실패 알림을 즉시 받아 UI를 업데이트할 수 있습니다.

```
GET /api/payments/qr/token/{token}/events
Authorization: Bearer {accessToken}
Accept: text/event-stream
```

### SSE 이벤트

| 이벤트명 | 전송 시점 | 데이터 |
|----------|----------|--------|
| `qr-payment-result` | 결제 확정 또는 거절 시 | `QrPaymentStatusResponse` |

### SSE 데이터 예시

```
event: qr-payment-result
data: {"token":"a1b2c3d4-...","status":"CAPTURED","paymentId":3,"capturedAt":"2026-03-23T14:36:00"}
```

- 연결 유지 시간: QR 토큰 TTL과 동일 (5분)
- 결제 완료/실패 후 자동 연결 종료

---

## 전체 흐름 요약

### 온라인 결제 (2단계)

```
클라이언트                    서버                      금융망
   │── authorize ──────────→ 결제 생성(AUTHORIZED) ──→     │
   │←── paymentId, code ───│                              │
   │── capture ────────────→ 결제 확정(CAPTURED) ────→ 카드거래 생성
   │←── capturedAt ────────│                              │
```

### QR 결제 - CPM (소비자가 가맹점 QR 스캔)

```
소비자                        서버                      금융망
   │── POST /qr ───────────→ 생성+확정 한번에 ──────→ 카드거래 생성
   │←── CAPTURED ──────────│                              │
```

### QR 결제 - MPM (결제자가 QR 생성, 상대방이 스캔)

```
결제자                        서버                   상대방
   │── POST /qr/token ────→ 토큰 생성(Redis 5분) ──→    │
   │── GET /events (SSE) ──→ 대기 중...                  │
   │                                    ←── GET /qr/token/{t} (정보 확인)
   │                                    ←── POST /confirm (결제 확인)
   │←── SSE: CAPTURED ─────│ 금융망 거래 생성            │
```
