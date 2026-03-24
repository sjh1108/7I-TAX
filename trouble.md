# Trouble Shooting Log — 2026-03-23

## 개요
`feature/credit-card-api` 브랜치에서 MR 리뷰 피드백 반영 및 기존 버그 수정 작업 수행.
총 12개 파일 변경, 1개 파일 신규 생성.

---

## 1. [Critical] QR 토큰 동시성 — 이중 결제 가능성 제거

**파일:** `PaymentService.java`
**증상:** `getPaymentIdFromToken()`이 Redis `.get()`만 사용하여 토큰이 삭제되지 않음. 동일 토큰으로 `confirmQrPayment()` 중복 호출 시 SSAFY 금융망에 이중 결제 요청이 가능했음.

**원인:** Redis 토큰 조회가 읽기 전용이라 TTL(300초) 만료 전까지 토큰 재사용 가능. DB status 체크(`AUTHORIZED`)도 동시 요청 시 race condition으로 우회됨.

**수정 내용:**
- 기존 `getPaymentIdFromToken()` → 두 메서드로 분리:
  - `peekPaymentIdFromToken()` — `.get()` (읽기 전용, 정보 조회/상태 확인/SSE 구독용)
  - `consumePaymentToken()` — `.getAndDelete()` (원자적 삭제, 결제 확정 전용)
- `confirmQrPayment()`만 `consumePaymentToken()` 사용
- `getQrPaymentInfo()`, `getQrPaymentStatus()`, `subscribeQrPayment()`는 `peekPaymentIdFromToken()` 사용

**추가 방어:** `PaymentRepository`에 `findByIdWithFetchForUpdate()` 비관적 락 쿼리 추가. `confirmQrPayment()`에서 `SELECT ... FOR UPDATE`로 DB 레벨 동시성 제어.

**영향 범위:** PaymentService, PaymentRepository

---

## 2. [Medium] SSE 초기 더미 이벤트 미발송

**파일:** `PaymentService.java` — `subscribeQrPayment()`
**증상:** SseEmitter 생성 후 아무 데이터 없이 반환. Nginx 등 리버스 프록시 환경에서 첫 데이터 없이 대기 시간이 길어지면 연결을 끊을 수 있음.

**수정 내용:**
- emitter 생성 직후 `connect` 이벤트 발송 추가
- 전송 실패 시 debug 로그만 남기고 무시 (연결 끊김 시)

```java
emitter.send(SseEmitter.event().name("connect").data("connected"));
```

**영향 범위:** PaymentService (SSE 구독 클라이언트)

---

## 3. [Low] 장부 자동생성 실패 이벤트 발행

**파일:** `PaymentService.java` — `autoCreateBookEntry()`, 신규 `BookEntryCreationFailedEvent.java`
**증상:** 장부 자동 생성 실패 시 로그만 남기고 후속 조치 불가. 실패 건 추적/재처리 메커니즘 없음.

**수정 내용:**
- `payment.event` 패키지에 `BookEntryCreationFailedEvent` record 클래스 생성
- `autoCreateBookEntry()` 외부 catch 블록에서 `ApplicationEventPublisher`로 이벤트 발행
- 향후 `@EventListener`로 실패 건 수집 및 배치 재처리 기반 마련

**영향 범위:** PaymentService, 신규 이벤트 클래스

---

## 4. [Medium] Payment 엔티티 메서드 리네이밍

**파일:** `Payment.java`, `PaymentService.java`
**증상:** `setSsafyTransactionUniqueNo()` — setter 네이밍이 JPA 엔티티 도메인 메서드 컨벤션과 불일치.

**수정 내용:**
- `setSsafyTransactionUniqueNo()` → `assignSsafyTransaction()` 리네이밍
- PaymentService 내 3곳 호출부(`capture`, `processQrPayment`, `confirmQrPayment`) 모두 일괄 변경

**영향 범위:** Payment 엔티티, PaymentService

---

## 5. [Medium] BookEntryRepository JPQL 호환성 수정

**파일:** `BookEntryRepository.java`
**증상:** `YEAR()`, `MONTH()` 함수는 MySQL 전용. H2/PostgreSQL 등 다른 DB에서는 동작하지 않음.

**수정 내용:**
- `YEAR(b.entryDate)` → `EXTRACT(YEAR FROM b.entryDate)` (3곳)
- `MONTH(b.entryDate)` → `EXTRACT(MONTH FROM b.entryDate)` (1곳)
- JPQL 표준 함수로 통일하여 DB 벤더 독립성 확보

**영향 범위:** BookEntryRepository, TaxSavingService, VatReturnService (간접)

---

## 6. [High] TaxSavingService / VatReturnService 집계 인덱스 오류

**파일:** `TaxSavingService.java`, `VatReturnService.java`
**증상:** `aggregateByUserIdAndDateRange()` 반환값 `Object[]`의 인덱스 참조 오류.
- 쿼리 결과: `[0]=income, [1]=expense, [2]=asset, [3]=businessExpense`
- 기존 코드: `agg[3]`으로 사업경비(businessExpense)만 읽어와 총경비로 사용 → **세금 계산 시 일반 경비가 누락됨**

**수정 내용:**
- `TaxSavingService`: `agg[3]` → `agg[1]` (totalExpense)
- `VatReturnService`: `agg[3]` → `agg[1]` (totalExpense), 변수명 `deductibleExpenses` → `totalExpense`

**영향 범위:** 세금 절감 추천, 부가세 신고서 초안 생성

---

## 7. [High] TaxCalculationEngine 방어 로직 추가

**파일:** `TaxCalculationEngine.java`
**증상:** 세율 구간 데이터가 없거나 과세표준이 어떤 구간에도 해당하지 않을 때 잘못된 세액(0원)이 반환될 수 있음.

**수정 내용:**
- 빈 brackets 리스트 체크 → `BusinessException` 발생
- `matched` 플래그 추가 → 구간 미매칭 시 `BusinessException` 발생

**영향 범위:** TaxCalculationEngine

---

## 8. [High] Card 엔티티 AES 암호화 적용

**파일:** `Card.java`
**증상:** 카드번호(`cardNo`)와 CVC가 평문으로 DB에 저장됨.

**수정 내용:**
- `@Convert(converter = AesEncryptor.class)` 어노테이션 추가
- 컬럼 길이 `16→512` (cardNo), `3→512` (cvc) — 암호화 후 길이 증가 대응

**영향 범위:** Card 엔티티, 모든 카드 조회/저장 로직 (JPA 자동 적용)

---

## 9. [Medium] AuthService SSAFY 멤버 등록 실패 시 공용키 폴백 제거

**파일:** `AuthService.java`
**증상:** SSAFY 멤버 등록 실패 시 공용 userKey를 할당하는 폴백 로직이 있었음. 공용키 사용 시 다른 사용자 거래와 혼선 가능.

**수정 내용:**
- 폴백 로직 전체 삭제, `log.error()`로 대체
- 등록 실패 시 userKey가 null로 남아 결제 시점에 명시적 에러 발생

**영향 범위:** AuthService, PaymentService (간접)

---

## 10. [Low] SecurityConfig CORS 와일드카드 제거

**파일:** `SecurityConfig.java`
**증상:** `allowedOriginPatterns`에 강제로 `"*"` 추가하는 코드가 있어 CORS 설정이 무의미했음.

**수정 내용:** 와일드카드 강제 추가 로직 삭제. `application.yml`에 정의된 origin만 허용.

**영향 범위:** SecurityConfig, 전체 API CORS 정책

---

## 11. [Low] api-tester.html 세목분류 테스트 UI 제거

**파일:** `api-tester.html`
**증상:** 세목 분류가 `autoCreateBookEntry` 내부로 통합되어 별도 테스트 UI 불필요.

**수정 내용:** 세목분류 네비게이션 링크, HTML 섹션, JavaScript 함수 삭제.

**영향 범위:** 프론트엔드 테스터 (개발용)

---

## 12. [Low] ev.md 포맷 오류

**파일:** `ev.md`
**증상:** 코드 블록 구문 오류 (닫는 태그 깨짐)

**수정 내용:** 마크다운 포맷 수정

---

# 변경 간 충돌(Conflict) 분석

아래는 오늘 수정한 내역들 사이에서 서로 영향을 주는 관계를 분석한 결과입니다.

## ⚠️ 충돌 1: AuthService 폴백 제거 × PaymentService getUserKey()

**관련 수정:** #9 (AuthService 공용키 폴백 제거) ↔ #1~#4 (PaymentService 결제 플로우)

**문제:** SSAFY 멤버 등록이 실패하면 `user.ssafyUserKey`가 `null`로 남음. 이후 해당 유저가 결제를 시도하면 `PaymentService.getUserKey()`에서 `BusinessException`이 즉시 발생.

**심각도:** ⚠️ 주의 필요
**판단:** 의도된 동작일 가능성이 높음(공용키 혼선 방지). 단, SSAFY 멤버 등록 실패 시 사용자에게 적절한 안내 없이 결제가 모두 실패하므로, **회원가입/로그인 시 등록 재시도 로직** 또는 **마이페이지에서 수동 등록 기능**이 필요할 수 있음.

## ✅ 비충돌: Card AES 암호화 × PaymentService 카드번호 사용

**관련 수정:** #8 (Card AES 암호화) ↔ #1 (PaymentService `card.getCardNo()`, `card.getCvc()`)

**상태:** 충돌 없음
**이유:** `@Convert(converter = AesEncryptor.class)`는 JPA 레벨에서 투명하게 암/복호화 처리. `card.getCardNo()` 호출 시 자동 복호화되어 평문 반환. PaymentService 코드 변경 불필요.

## ✅ 비충돌: Payment 리네이밍 × PaymentService 호출부

**관련 수정:** #4 (assignSsafyTransaction 리네이밍) ↔ #1 (PaymentService 3곳 호출)

**상태:** 충돌 없음
**이유:** 엔티티 메서드명과 Service 호출부 3곳 모두 일괄 변경 완료. 빌드 성공으로 미수정 호출부 없음 확인.

## ✅ 비충돌: BookEntryRepository JPQL 수정 × TaxSavingService/VatReturnService 인덱스 수정

**관련 수정:** #5 (EXTRACT 함수 변경) ↔ #6 (agg 인덱스 수정)

**상태:** 충돌 없음
**이유:** JPQL 함수 변경은 쿼리 실행 호환성 수정이고, 인덱스 수정은 결과 매핑 수정. `aggregateByUserIdAndDateRange()` 쿼리의 SELECT 절 순서(`[0]=income, [1]=expense, [2]=asset, [3]=businessExpense`)는 변경되지 않았으므로 `agg[1]` 참조가 정확함.

## ✅ 비충돌: QR 토큰 소비 × SSE 구독

**관련 수정:** #1 (consumePaymentToken) ↔ #2 (SSE 초기 이벤트)

**상태:** 충돌 없음
**이유:** SSE 구독(`subscribeQrPayment`)은 `peekPaymentIdFromToken()`을 사용하므로 토큰 소비 안 됨. 결제 확정(`confirmQrPayment`)만 `consumePaymentToken()`으로 토큰 소비. 순서: 구독 → 결제 확정 → SSE 알림의 흐름이 정상 유지.

## ✅ 비충돌: 장부 이벤트 발행 × TaxCalculationEngine 방어 로직

**관련 수정:** #3 (BookEntryCreationFailedEvent) ↔ #7 (TaxCalculationEngine 예외)

**상태:** 충돌 없음
**이유:** `autoCreateBookEntry()`는 `TaxClassificationService`를 사용하며, `TaxCalculationEngine`은 직접 호출하지 않음. 두 변경은 독립적.
