# FE API 연동 TODO 리스트

> BE 31개 API 기준 | 완료: 5개 | 미완료: 26개
> 최종 업데이트: 2026-03-23

---

## 완료

### Auth API (5/6)

- [x] `POST /api/auth/verify-identity` — 본인인증
- [x] `POST /api/auth/setup-pin` — PIN 설정 + JWT 발급
- [x] `POST /api/auth/login` — PIN 로그인
- [x] `POST /api/auth/reissue` — 토큰 재발급
- [x] `POST /api/auth/logout` — 로그아웃
- [ ] `POST /api/auth/test-login` — 테스트용 로그인

---

## 미완료

### 1. Card API (0/8) — Repository/ViewModel 전부 TODO

> **우선순위: 높음** — 카드 등록 UI 이미 있음, TODO만 구현하면 됨

- [ ] `POST /api/cards` — 카드/계좌 생성 (SSAFY 금융 API 연동)
- [ ] `GET /api/cards` — 카드 목록 조회
- [ ] `GET /api/cards/{cardId}` — 카드 상세 조회
- [ ] `PATCH /api/cards/{cardId}/default` — 기본 카드 설정
- [ ] `DELETE /api/cards/{cardId}` — 카드 삭제
- [ ] `POST /api/cards/{cardId}/deposit` — 카드 입금 (충전)
- [ ] `GET /api/cards/{cardId}/balance` — 잔액 조회
- [ ] `GET /api/cards/{cardId}/transactions` — 거래 내역 조회

**작업 범위**: CardRepositoryImpl 구현 + CardViewModel 구현 + UI 화면 연결

---

### 2. Payment API (0/6) — FE에 API 인터페이스 없음

> **우선순위: 높음** — QR 결제 화면 있음, API 인터페이스부터 생성 필요

- [ ] `POST /api/payments/authorize` — 결제 승인 (2단계: 승인)
- [ ] `POST /api/payments/{paymentId}/capture` — 결제 확정 (2단계: 확정)
- [ ] `POST /api/payments/{paymentId}/cancel` — 결제 취소
- [ ] `GET /api/payments/{paymentId}` — 결제 상세 조회
- [ ] `GET /api/payments` — 결제 내역 조회 (페이징, 날짜/상태 필터)
- [ ] `POST /api/payments/qr` — QR 결제 (원스텝 승인+확정)

**작업 범위**: PaymentApi 생성 + Repository + ViewModel + UI 연결

---

### 3. Transfer API (0/4) — FE에 API 인터페이스 없음

> **우선순위: 중간** — UI 화면도 없음, API + 화면 모두 생성 필요

- [ ] `POST /api/transfers/p2p` — P2P 송금
- [ ] `POST /api/transfers/withdraw` — 출금 (외부 계좌)
- [ ] `GET /api/transfers` — 송금 내역 조회 (페이징)
- [ ] `GET /api/transfers/{transferId}` — 송금 상세 조회

**작업 범위**: TransferApi 생성 + Repository + ViewModel + UI 화면 신규 생성

---

### 4. BookEntry API (0/8) — Repository 구현됨, ViewModel 없음

> **우선순위: 중간** — Repository는 완성, ViewModel만 만들면 됨

- [ ] `POST /api/book-entries` — 장부 생성
- [ ] `GET /api/book-entries` — 장부 목록 조회 (페이징, confirmed 필터)
- [ ] `GET /api/book-entries/{entryId}` — 장부 상세 조회
- [ ] `GET /api/book-entries/unconfirmed-count` — 미확인 건수 조회
- [ ] `PATCH /api/book-entries/{entryId}/confirm` — 장부 확인 처리
- [ ] `PATCH /api/book-entries/{entryId}/category` — 카테고리 변경
- [ ] `PATCH /api/book-entries/{entryId}/personal` — 개인용 마크
- [ ] `PATCH /api/book-entries/{entryId}/business` — 사업용 마크

**작업 범위**: BookEntryViewModel 생성 + UI 화면 연결

---

### 5. Classification API (0/1) — Repository 구현됨, ViewModel 없음

> **우선순위: 중간**

- [ ] `POST /api/classification` — 세목 자동분류 (MCC/키워드/금액 규칙 기반)

**작업 범위**: ClassificationViewModel 생성 + 분류 화면 연결

---

### 6. Export API (0/3) — Repository 구현됨, ViewModel 없음

> **우선순위: 낮음** — 다운로드 기능, 화면 구현도 필요

- [ ] `GET /api/export/book-entries` — 장부 CSV 내보내기 (연도 필터)
- [ ] `GET /api/export/vat` — 부가세 CSV 내보내기 (연도/반기 필터)
- [ ] `GET /api/export/income-tax` — 소득세 CSV 내보내기 (연도 필터)

**작업 범위**: ExportViewModel 생성 + 다운로드 UI 생성

---

### 7. Tax Calendar API (0/1) — Repository 구현됨, ViewModel 없음

> **우선순위: 높음** — 서버 정상 동작 확인됨, ViewModel만 만들면 끝

- [ ] `GET /api/tax-calendar/deadlines` — 세금 신고 기한 + D-day 조회

**작업 범위**: TaxCalendarViewModel 생성 + 홈/대시보드 화면 연결

---

### 8. Tax Estimation API (0/1) — Repository 구현됨, ViewModel 없음

> **우선순위: 낮음** — 대시보드에 표시할 데이터

- [ ] `GET /api/tax-estimation` — 세금 예상 계산 (2024 과세표준)

**작업 범위**: TaxEstimationViewModel 생성 + 대시보드 화면 연결

---

## 작업 우선순위 요약

| 순위 | 기능 | 작업량 | 이유 |
|------|------|--------|------|
| 1 | **Card** | Repository + ViewModel | UI 이미 있음, TODO 구현만 |
| 2 | **Payment + QR** | API + Repository + ViewModel | QR 화면 있음, 핵심 기능 |
| 3 | **Tax Calendar** | ViewModel만 | 서버 정상, 가장 적은 작업량 |
| 4 | **BookEntry** | ViewModel | Repository 완성됨 |
| 5 | **Classification** | ViewModel | Repository 완성됨 |
| 6 | **Transfer** | 전부 신규 | API + UI 모두 필요 |
| 7 | **Export** | 전부 신규 | 파일 다운로드 UI 필요 |
| 8 | **Tax Estimation** | ViewModel | 대시보드 연동 |

---

## FE 레이어별 작업 현황

| API | Retrofit Interface | Repository | ViewModel | UI 연결 |
|-----|-------------------|------------|-----------|---------|
| Auth | 완료 | 완료 | 완료 | 완료 |
| Card | 완료 (부분) | TODO | TODO | mock 데이터 |
| Payment | 없음 | 없음 | 없음 | mock 데이터 |
| Transfer | 없음 | 없음 | 없음 | 화면 없음 |
| BookEntry | 완료 | 완료 | 없음 | 화면 있음 |
| Classification | 완료 | 완료 | 없음 | 화면 있음 |
| Export | 완료 | 완료 | 없음 | 화면 없음 |
| Tax Calendar | 완료 | 완료 | 없음 | 화면 없음 |
| Tax Estimation | 완료 | 완료 | 없음 | 화면 없음 |
