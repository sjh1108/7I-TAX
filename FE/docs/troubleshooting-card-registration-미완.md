# 카드 등록 트러블슈팅 (미완)

## 1. 증상

카드 등록 후 앱을 재시작하면 등록된 카드가 사라짐.

## 2. 원인 분석

카드가 삭제되는 게 아니라, **처음부터 서버에 저장이 안 됐음.**

### 흐름
```
FE 카드번호 입력 → CardViewModel.completeRegistration()
  → CardCreateRequest 생성 (withdrawalAccountNo="", otpToken="")
    → BE: @NotBlank 검증 실패 → "입력값이 올바르지 않습니다"
  → FE: API 실패 → 인메모리 폴백 (ViewModel에만 저장)
  → 앱 재시작 → ViewModel 소멸 → 카드 사라짐
```

### 핵심 원인: FE ↔ BE API 스펙 불일치

**FE가 보낸 값:**
```kotlin
CardCreateRequest(
    cardName = "일반 카드",
    cardType = "PERSONAL",
    cardUniqueNo = "1234567890123456",  // 사용자 카드번호
    withdrawalAccountNo = "",           // ← 빈 값 (@NotBlank 위반)
    withdrawalDate = "1227",
    otpToken = ""                       // ← 빈 값 (@NotBlank 위반)
)
```

**BE가 기대하는 값:**
```java
CreateCardRequest(
    cardName,              // 카드 이름
    cardType,              // PERSONAL 또는 BUSINESS
    cardUniqueNo,          // SSAFY 카드 상품 고유번호 (GET /cards/products에서 조회)
    withdrawalAccountNo,   // 출금 계좌번호 (GET /cards/accounts에서 조회)
    withdrawalDate,        // 결제일
    otpToken               // SMS OTP 검증 토큰 (POST /api/sms/verify로 획득)
)
```

### FE와 BE의 "카드 등록" 의미 차이
- **FE**: 실제 카드 정보를 입력해서 등록
- **BE**: SSAFY 가상 금융 시스템에서 카드를 발급받는 프로세스

## 3. BE가 기대하는 카드 등록 플로우

```
1. GET  /api/cards/accounts  → 출금 계좌 목록 조회 → 계좌 선택
2. GET  /api/cards/products  → 카드 상품 목록 조회 → 상품 선택
3. POST /api/sms/send        → OTP 발송
4. POST /api/sms/verify      → OTP 검증 → otpToken 획득
5. POST /api/cards           → 계좌번호 + 상품번호 + otpToken으로 카드 생성
```

## 4. FE 수정 내역 (완료)

### 4-1. CardType 불일치 수정
- `DEBIT` → `PERSONAL`, `CREDIT` → `BUSINESS`
- BE CardType enum: [PERSONAL, BUSINESS]
- 커밋: `960e53f`

### 4-2. API + Repository + ViewModel 추가
- `GET /cards/accounts` — 계좌 목록 조회 API 추가
- `GET /cards/products` — 카드 상품 목록 조회 API 추가
- CardViewModel: `loadAccounts()`, `loadProducts()`, `selectAccount()`, `selectProduct()` 추가
- `completeRegistration()`에서 선택된 계좌번호 + 상품번호 사용
- 커밋: `95a516c`

### 4-3. UI 화면 추가
- `CardAccountSelectScreen` — 서버에서 계좌 목록 조회 + 선택
- `CardProductSelectScreen` — 서버에서 카드 상품 목록 조회 + 선택
- 카드 등록 플로우: 유형 선택 → 계좌 선택 → 상품 선택 → SMS 인증 → 등록
- 커밋: `95a516c`

## 5. 미해결 이슈

### 5-1. OTP 토큰 (미해결)
- SMS 인증 비용 문제로 OTP 목업 중
- BE에서 `smsOtpService.validateOtpToken()` 실제 검증하므로 더미값 불가
- 현재 `otpToken = "SKIP"` 임시값 사용 → BE에서 거절될 예정
- **BE 팀 대응 필요**: OTP 스킵 가능하게 하거나 테스트용 토큰 제공

### 5-2. 계좌/카드 상품 데이터 확인 (미확인)
- 서버 500 에러(verify-identity)로 토큰 발급 불가 → API 테스트 못 함
- 서버 복구 후 확인 필요:
  - `GET /cards/accounts` — 계좌 데이터 있는지
  - `GET /cards/products` — 카드 상품 데이터 있는지
- 계좌 없으면 `POST /api/banking/accounts`로 먼저 생성 필요할 수 있음

### 5-3. verify-identity 500 에러 (간헐적)
- 아까 정상 동작 → 이후 500 재발
- Redis 연결 또는 SSAFY Finance API 장애 추정
- BE 서버 로그 확인 필요

## 6. 테스트 방법

서버 정상화 후:
```bash
# 1. 로그인
TOKEN=$(curl -s -X POST https://j14c203.p.ssafy.io/api/auth/login \
  -H 'Content-Type: application/json; charset=UTF-8' \
  -d '{"phoneNumber":"01012345678","pin":"123456"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# 2. 계좌 조회
curl -s https://j14c203.p.ssafy.io/api/cards/accounts \
  -H "Authorization: Bearer $TOKEN"

# 3. 카드 상품 조회
curl -s https://j14c203.p.ssafy.io/api/cards/products \
  -H "Authorization: Bearer $TOKEN"

# 4. 카드 생성 (OTP 해결 후)
curl -s -X POST https://j14c203.p.ssafy.io/api/cards \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{
    "cardName": "일반 카드",
    "cardType": "PERSONAL",
    "cardUniqueNo": "{상품번호}",
    "withdrawalAccountNo": "{계좌번호}",
    "withdrawalDate": "15",
    "otpToken": "{OTP토큰}"
  }'
```

## 7. 관련 파일

### FE
- `CardViewModel.kt` — 카드 등록 로직 + 디버그 로그
- `CardApi.kt` — Retrofit 인터페이스
- `CardRepository.kt` / `CardRepositoryImpl.kt` — API 호출
- `CardAccountSelectScreen.kt` — 계좌 선택 화면 (신규)
- `CardProductSelectScreen.kt` — 카드 상품 선택 화면 (신규)
- `CardCreateRequest.kt` — 요청 DTO

### BE
- `CardController.java` — 카드 API 컨트롤러
- `CreateCardRequest.java` — 요청 DTO (@NotBlank 검증)
- `CardService.java:78~107` — 카드 생성 로직 (OTP 검증 + SSAFY API 호출)

---

*작성일: 2026-03-26*
*상태: 미완 — OTP 해결 + 서버 정상화 후 재테스트 필요*
