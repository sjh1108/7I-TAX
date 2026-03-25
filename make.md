# Tax7i 백엔드 기능 목록

## 1. 인증 (Auth)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/verify-identity` | 본인인증 (NICE 연동) |
| POST | `/api/auth/setup-pin` | PIN 설정 (X-Verify-Token 헤더 필요) |
| POST | `/api/auth/login` | PIN 로그인 (전화번호 + PIN) |
| POST | `/api/auth/reissue` | 토큰 재발급 (Refresh Token) |
| POST | `/api/auth/logout` | 로그아웃 (Access Token 무효화) |
| POST | `/api/auth/test-login` | 테스트 로그인 (설정으로 활성화/비활성화) |

**관련 서비스:** AuthService, PinService, ConsentService, NiceIdentityMockService

---

## 2. 카드 (Card)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/cards/products` | 카드 상품 목록 조회 |
| POST | `/api/cards` | 신용카드 발급 |
| GET | `/api/cards` | 내 카드 목록 조회 |
| GET | `/api/cards/{cardId}` | 카드 상세 조회 |
| PATCH | `/api/cards/{cardId}/default` | 기본 카드 설정 |
| DELETE | `/api/cards/{cardId}` | 카드 삭제 |
| POST | `/api/cards/{cardId}/payment` | 카드 결제 |
| POST | `/api/cards/{cardId}/payment/cancel` | 카드 결제 취소 |
| GET | `/api/cards/{cardId}/transactions` | 카드 결제 내역 조회 (기간별) |
| GET | `/api/cards/{cardId}/billing` | 청구서 조회 (월별) |

**관련 서비스:** CardService, SsafyCreditCardClient

---

## 3. 결제 (Payment)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/payments/authorize` | 결제 승인 요청 |
| POST | `/api/payments/{paymentId}/capture` | 결제 확정 (캡처) |
| POST | `/api/payments/{paymentId}/cancel` | 결제 취소 |
| GET | `/api/payments/{paymentId}` | 결제 상세 조회 |
| GET | `/api/payments` | 결제 내역 목록 조회 (기간/상태 필터, 페이징) |
| POST | `/api/payments/qr` | QR 결제 처리 |
| POST | `/api/payments/qr/token` | QR 토큰 생성 (MPM) |
| GET | `/api/payments/qr/token/{token}` | QR 결제 정보 조회 |
| POST | `/api/payments/qr/token/{token}/confirm` | QR 결제 확인 |
| GET | `/api/payments/qr/token/{token}/status` | QR 결제 상태 조회 |
| GET | `/api/payments/qr/token/{token}/events` | QR 결제 실시간 이벤트 (SSE) |

**관련 서비스:** PaymentService

---

## 4. 송금 (Transfer)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/transfers/p2p` | P2P 송금 |
| POST | `/api/transfers/withdraw` | 출금 |
| GET | `/api/transfers` | 송금 내역 목록 조회 (페이징) |
| GET | `/api/transfers/{transferId}` | 송금 상세 조회 |

**관련 서비스:** TransferService, SsafyFinanceClient

---

## 5. 간편장부 (Book Entry)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/book-entries` | 장부 항목 생성 |
| GET | `/api/book-entries` | 장부 목록 조회 (확인 여부 필터, 페이징) |
| GET | `/api/book-entries/{entryId}` | 장부 상세 조회 |
| GET | `/api/book-entries/unconfirmed-count` | 미확인 항목 수 조회 |
| PATCH | `/api/book-entries/{entryId}/confirm` | 장부 항목 확인 처리 |
| PATCH | `/api/book-entries/{entryId}/category` | 카테고리 변경 |
| PATCH | `/api/book-entries/{entryId}/personal` | 개인용 지출로 분류 |
| PATCH | `/api/book-entries/{entryId}/business` | 사업용 지출로 분류 |

**관련 서비스:** BookEntryService

---

## 6. 세금 분류 (Tax Classification)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/classification` | 거래 세금 분류 (가맹점명, MCC, 금액 기반) |

**관련 서비스:** TaxClassificationService, ClassificationCacheService, EntertainmentLimitService

---

## 7. 세금 캘린더 (Tax Calendar)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/tax-calendar/deadlines` | 다가오는 세금 납부 기한 조회 |

**관련 서비스:** TaxCalendarService

---

## 8. 예상 세금 계산 (Tax Estimation)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/tax-estimation` | 연도별 예상 세금 계산 |

**관련 서비스:** TaxEstimationService

---

## 9. 데이터 내보내기 (Export)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/export/book-entries` | 간편장부 CSV 내보내기 (연도별) |
| GET | `/api/export/vat` | 부가세 요약 CSV 내보내기 (연도/반기별) |
| GET | `/api/export/income-tax` | 종합소득세 요약 CSV 내보내기 (연도별) |

**관련 서비스:** ExportService

---

## 주요 엔티티

| 도메인 | 엔티티 |
|--------|--------|
| Auth | User, BusinessProfile, UserConsent, UserStatus, ConsentType |
| Card | Card, CardTransaction, CardType, CardTransactionType |
| Payment | Payment, PaymentStatus, PaymentMethod, PaymentPurpose |
| Transfer | Transfer, TransferStatus, TransferType |
| BookEntry | BookEntry, EntryType |
| Classification | MccTaxRule, Merchant, MerchantKeywordMapping, TaxCategory |

## 외부 연동

- **SSAFY 금융망 API** — SsafyFinanceClient (계좌 관련)
- **SSAFY 신용카드 API** — SsafyCreditCardClient (카드 발급/결제/조회)
- **NICE 본인인증** — NiceIdentityMockService (Mock)
