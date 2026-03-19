# Tax7i DB 성능 최적화 보고서

## 1. 개요

### 1.1 프로젝트 배경
Tax7i는 개인사업자를 위한 세금 자동분류 및 간편장부 관리 서비스로, 결제(Payment), 장부(BookEntry), 세목분류(Classification), 세금 추정(TaxEstimation), CSV 내보내기(Export) 등 다양한 기능을 제공한다.

서비스가 성장하여 **사용자 10만명, 장부 데이터 수백만 건** 규모로 확대될 경우를 가정하고, 현재 코드에서 성능 병목이 될 수 있는 지점을 사전에 분석하여 최적화를 적용하였다.

### 1.2 최적화 전 시스템 현황

| 항목 | 최적화 전 상태 |
|------|---------------|
| DB 인덱스 | 분류 관련 테이블(Merchant, MccTaxRule)에만 존재. Payment, Card, User, UserConsent 등 핵심 엔티티에 FK 인덱스 없음 |
| Hibernate 배치 설정 | 없음 — INSERT/UPDATE가 건건이 개별 SQL로 실행 |
| HikariCP 커넥션 풀 | Spring Boot 기본값(max=10) 사용 |
| PostgreSQL 튜닝 | 기본 설정 — shared_buffers=128MB, random_page_cost=4.0 |
| 쿼리 패턴 | 전체 데이터를 메모리에 로드 후 Java에서 집계하는 방식 다수 |
| 캐싱 | 없음 — 변경이 드문 참조 데이터도 매번 DB 조회 |
| 트랜잭션 | 일부 읽기 전용 메서드에 readOnly 미적용 |

---

## 2. 최적화 항목별 상세 분석

---

### 2.1 Phase 1: 인덱스 추가

#### 왜 이 방식을 선택했는가

인덱스는 대용량 데이터에서 **가장 즉각적이고 확실한 성능 개선 수단**이다. 인덱스 없이 WHERE 조건으로 조회하면 DB는 테이블 전체를 순차 탐색(Full Table Scan)해야 하지만, B-Tree 인덱스가 있으면 O(log N)으로 탐색할 수 있다.

현재 코드에서 `PaymentRepository`, `CardRepository`, `UserRepository` 등이 FK 컬럼(user_id, card_id)이나 조건 컬럼(status, phoneLast4)으로 빈번하게 조회하지만, 해당 컬럼에 인덱스가 없었다.

#### 변경 내역

| 엔티티 | 추가한 인덱스 | 대상 컬럼 | 용도 |
|--------|-------------|----------|------|
| Payment | `idx_payment_user_id` | user_id | 유저별 결제 내역 조회 |
| Payment | `idx_payment_card_id` | card_id | 카드별 결제 내역 조회 |
| Payment | `idx_payment_status` | status | 상태별 필터링 |
| Payment | `idx_payment_user_status` | user_id, status | 복합 인덱스 — "해당 유저의 승인된 결제" 등 복합 조건 |
| Card | `idx_card_user_id` | user_id | 유저별 카드 목록 조회 |
| User | `idx_user_phone_last4` | phone_last4 | PIN 로그인 시 전화번호 뒷자리로 사용자 검색 |
| UserConsent | `idx_consent_user_id` | user_id | 유저별 동의 내역 조회 |

> BookEntry, Merchant, MccTaxRule, MerchantKeywordMapping은 이미 적절한 인덱스가 존재하여 변경하지 않음

#### 예상 성능 개선 수치

**테스트 시나리오: Payment 테이블 100만 건 기준, 특정 유저의 결제 내역 조회**

| 지표 | 인덱스 적용 전 | 인덱스 적용 후 | 개선율 |
|------|--------------|--------------|--------|
| 조회 방식 | Sequential Scan (Full Table Scan) | Index Scan (B-Tree) | — |
| 탐색 복잡도 | O(N) = 1,000,000건 스캔 | O(log N) ≈ 20회 탐색 | **~50,000배** |
| 예상 응답 시간 | 200~500ms | 1~5ms | **40~100배** |
| 디스크 I/O | 테이블 전체 페이지 읽기 | 인덱스 페이지 + 데이터 페이지 소수 | **~99% 감소** |

**복합 인덱스 효과: `idx_payment_user_status` (user_id, status)**

```
-- 이 쿼리가 인덱스 하나로 해결됨 (Index Only Scan 가능)
SELECT * FROM payments WHERE user_id = ? AND status = 'CAPTURED';
```

| 지표 | 단일 인덱스 2개 사용 | 복합 인덱스 1개 사용 | 개선율 |
|------|-------------------|-------------------|--------|
| 인덱스 스캔 횟수 | 2회 (Bitmap AND) | 1회 | **50% 감소** |
| 예상 응답 시간 | 3~8ms | 1~3ms | **~2배** |

#### 어려웠던 점

- **JPA @Index의 columnList는 DB 컬럼명을 사용해야 한다.** Java 필드명(`userId`)이 아닌 실제 DB 컬럼명(`user_id`)을 사용해야 하므로, Spring Boot의 네이밍 전략(`CamelCaseToUnderscoresNamingStrategy`)이 적용된 결과를 정확히 파악해야 했다.
- **인덱스 추가는 INSERT/UPDATE 성능에 영향을 준다.** 인덱스가 많을수록 쓰기 성능이 떨어지므로, 실제 쿼리 패턴을 분석하여 필요한 인덱스만 선별적으로 추가했다. Payment 테이블에 4개의 인덱스를 추가했지만, 결제 데이터는 조회가 쓰기보다 압도적으로 많으므로 적절한 트레이드오프라 판단했다.

---

### 2.2 Phase 2: Hibernate 배치 설정 + HikariCP + PostgreSQL 튜닝

#### 왜 이 방식을 선택했는가

**Hibernate 배치 설정이 전혀 없었다.** 이는 10건의 BookEntry를 생성할 때 INSERT 문이 10번 개별 실행된다는 의미이다. 네트워크 왕복(Round-trip)이 건당 발생하므로, 대량 데이터 처리 시 심각한 병목이 된다.

또한 HikariCP의 기본 커넥션 풀 크기(10)는 동시 접속이 늘어나면 커넥션 대기가 발생할 수 있고, PostgreSQL도 기본 설정은 범용적이지만 SSD 기반 서버에 최적화되어 있지 않다.

#### 변경 내역

**application.yaml — Hibernate 설정**

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20       # INSERT/UPDATE를 20건씩 묶어 실행
          fetch_size: 50       # ResultSet을 50건씩 가져옴
        order_inserts: true    # 같은 테이블 INSERT를 모아서 배치
        order_updates: true    # 같은 테이블 UPDATE를 모아서 배치
        default_batch_fetch_size: 100  # LAZY 컬렉션 로딩 시 IN절로 100건씩
    open-in-view: false        # Controller에서 DB 커넥션 점유 방지
```

**application-local.yaml — HikariCP 설정**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 최대 커넥션 수
      minimum-idle: 5            # 최소 유지 커넥션
      connection-timeout: 30000  # 커넥션 획득 대기 시간 (30초)
      idle-timeout: 600000       # 유휴 커넥션 제거 시간 (10분)
      max-lifetime: 1800000      # 커넥션 최대 수명 (30분)
```

**docker-compose.yml — PostgreSQL 튜닝**

```yaml
command: >
  postgres
  -c shared_buffers=256MB         # 공유 메모리 버퍼 (기본 128MB)
  -c work_mem=16MB                # 정렬/해시 작업 메모리
  -c effective_cache_size=512MB   # OS 캐시 포함 예상 메모리
  -c random_page_cost=1.1         # SSD 최적화 (기본 4.0은 HDD 기준)
```

#### 예상 성능 개선 수치

**시나리오 1: BookEntry 100건 일괄 INSERT**

| 지표 | 배치 미적용 | batch_size=20 적용 | 개선율 |
|------|-----------|-------------------|--------|
| SQL 실행 횟수 | 100회 (건건이) | 5회 (20건씩 묶음) | **95% 감소** |
| 네트워크 왕복 | 100회 | 5회 | **95% 감소** |
| 예상 소요 시간 | ~500ms | ~50ms | **~10배** |

**시나리오 2: LAZY 컬렉션 로딩 (N+1 문제 방지)**

```
-- 적용 전: 유저 10명의 결제 내역 조회 시
SELECT * FROM users LIMIT 10;
SELECT * FROM payments WHERE user_id = 1;  -- 쿼리 1
SELECT * FROM payments WHERE user_id = 2;  -- 쿼리 2
...
SELECT * FROM payments WHERE user_id = 10; -- 쿼리 10
-- 총 11개 쿼리

-- 적용 후: default_batch_fetch_size=100
SELECT * FROM users LIMIT 10;
SELECT * FROM payments WHERE user_id IN (1, 2, 3, ..., 10); -- IN절 1개
-- 총 2개 쿼리
```

| 지표 | batch_fetch 미적용 | batch_fetch_size=100 | 개선율 |
|------|-------------------|---------------------|--------|
| 쿼리 수 (유저 10명) | 11개 (1+N) | 2개 | **82% 감소** |
| 쿼리 수 (유저 100명) | 101개 (1+N) | 2개 | **98% 감소** |
| 응답 시간 (유저 100명) | ~1000ms | ~20ms | **~50배** |

**시나리오 3: open-in-view=false**

| 지표 | open-in-view=true (기본값) | open-in-view=false | 효과 |
|------|--------------------------|-------------------|------|
| DB 커넥션 점유 | Controller → View 렌더링 완료까지 | Service 메서드 종료 시 반환 | **커넥션 점유 시간 ~60% 감소** |
| 동시 처리 가능 요청 수 | 커넥션 풀 크기에 제한됨 | 커넥션 빠른 반환으로 여유 확보 | **동시성 ~2배 향상** |

**시나리오 4: PostgreSQL random_page_cost 튜닝**

| 지표 | random_page_cost=4.0 (기본) | random_page_cost=1.1 (SSD) | 효과 |
|------|---------------------------|---------------------------|------|
| 쿼리 플래너 동작 | Sequential Scan 선호 | Index Scan 적극 활용 | — |
| 인덱스 사용률 | 낮음 (비용 과다 산정) | 높음 (실제 SSD 성능 반영) | **인덱스 활용률 향상** |

#### 어려웠던 점

- **order_inserts/order_updates를 반드시 함께 설정해야 한다.** batch_size만 설정하면, 서로 다른 테이블의 INSERT가 섞여서 배치가 깨질 수 있다. 예를 들어 `INSERT Payment → INSERT BookEntry → INSERT Payment`처럼 섞이면 배치가 분할된다. `order_inserts=true`로 같은 테이블끼리 모아야 배치 효과가 극대화된다.
- **open-in-view=false로 변경 시 LazyInitializationException 위험이 있다.** Controller에서 엔티티의 LAZY 필드에 접근하면 이미 트랜잭션이 종료된 상태이므로 예외가 발생한다. 기존 코드에서 Controller가 엔티티를 직접 반환하는 곳이 없는지 확인한 후 적용했다. 모든 Controller는 DTO를 반환하고 있었으므로 안전하게 적용할 수 있었다.

---

### 2.3 Phase 3: 쿼리 최적화

#### 2.3.1 TaxEstimationService — Java 집계를 DB 집계로 전환

##### 문제

```java
// 최적화 전: 전체 BookEntry를 메모리에 로드하여 Java for-loop로 합산
List<BookEntry> entries = bookEntryRepository
    .findByUserIdAndEntryDateBetween(userId, start, end, Pageable.unpaged())
    .getContent();

long totalIncome = 0, totalExpense = 0, deductibleExpenses = 0;
for (BookEntry e : entries) {
    if (!e.getConfirmed()) continue;
    switch (e.getEntryType()) {
        case INCOME -> totalIncome += e.getIncomeAmount();
        case EXPENSE -> { totalExpense += e.getExpenseAmount(); ... }
    }
}
```

이 방식은 1년치 장부 데이터 **전체**를 JVM 힙에 올린 뒤 순회한다. 데이터가 1만 건이면 각 BookEntry 객체(약 500바이트) × 10,000 = **~5MB**, 10만 건이면 **~50MB**의 메모리를 소비하며, GC 압력도 증가한다.

##### 해결

```java
// 최적화 후: DB에서 집계하여 결과값 4개만 받아옴
@Query("SELECT " +
    "COALESCE(SUM(CASE WHEN b.entryType = 'INCOME' THEN b.incomeAmount ELSE 0 END), 0), " +
    "COALESCE(SUM(CASE WHEN b.entryType = 'EXPENSE' THEN b.expenseAmount ELSE 0 END), 0), " +
    "COALESCE(SUM(CASE WHEN b.entryType = 'ASSET' THEN b.fixedAssetAmount ELSE 0 END), 0), " +
    "COALESCE(SUM(CASE WHEN b.entryType = 'EXPENSE' AND b.isBusinessExpense = true " +
    "THEN b.expenseAmount ELSE 0 END), 0) " +
    "FROM BookEntry b WHERE b.userId = :userId AND b.confirmed = true " +
    "AND b.entryDate BETWEEN :start AND :end")
Object[] aggregateByUserIdAndDateRange(Long userId, LocalDate start, LocalDate end);
```

##### 예상 성능 개선 수치 (장부 10만 건 기준)

| 지표 | Java 집계 (Before) | DB 집계 (After) | 개선율 |
|------|-------------------|----------------|--------|
| DB → App 전송 데이터 | ~50MB (10만 행 × ~500B) | ~32B (Long 4개) | **99.99% 감소** |
| JVM 힙 사용량 | ~50MB (객체 10만 개) | ~0 (원시 타입 4개) | **99.99% 감소** |
| DB 처리 | 전체 행 반환 | SUM만 계산하여 반환 | — |
| 네트워크 왕복 | 대용량 ResultSet 전송 | 단일 행 반환 | **전송량 99% 감소** |
| 예상 응답 시간 | 500ms~2s | 10~30ms | **15~60배** |
| GC 영향 | Young GC 빈번 발생 | 거의 없음 | **GC 부하 제거** |

#### 2.3.2 ExportService — Pageable.unpaged()를 페이지 단위 처리로 변경

##### 문제

```java
// 최적화 전: 전체 데이터를 한번에 메모리에 로드
List<BookEntry> entries = bookEntryRepository
    .findByUserIdAndEntryDateBetween(userId, start, end, Pageable.unpaged())
    .getContent();
```

`Pageable.unpaged()`는 LIMIT 없이 전체 결과를 한번에 가져온다. CSV 내보내기처럼 전체 데이터가 필요한 경우에도, 메모리에 전체를 올리면 대용량 데이터에서 **OutOfMemoryError** 위험이 있다.

##### 해결

```java
// 최적화 후: 500건씩 페이지 단위로 처리
int page = 0;
Page<BookEntry> entryPage;
do {
    entryPage = bookEntryRepository.findByUserIdAndEntryDateBetween(
            userId, start, end, PageRequest.of(page, 500, Sort.by("entryDate")));
    for (BookEntry e : entryPage.getContent()) {
        // CSV 행 생성...
    }
    page++;
} while (entryPage.hasNext());
```

##### 예상 성능 개선 수치 (장부 10만 건 기준)

| 지표 | unpaged (Before) | 500건 페이징 (After) | 개선율 |
|------|-----------------|---------------------|--------|
| 동시 메모리 사용 | ~50MB (전체 로드) | ~250KB (500건만 유지) | **99.5% 감소** |
| OOM 위험 | 높음 (데이터 증가 시 선형 증가) | 없음 (고정 메모리) | **OOM 리스크 제거** |
| 첫 응답까지 시간 | 전체 로드 후 시작 | 첫 페이지 로드 후 시작 | **첫 페이지 시작 속도 ~200배** |

#### 2.3.3 ConsentService — 루프 내 개별 DB 조회를 배치 조회로 변경

##### 문제

```java
// 최적화 전: 동의 항목 수만큼 개별 DB 조회 (N+1 패턴)
for (ConsentRequest req : requests) {
    UserConsent existing = userConsentRepository
        .findByUserIdAndConsentType(userId, req.consentType())  // 매 루프마다 DB 호출
        .orElse(null);
    ...
}
```

동의 항목이 5개면 SELECT가 5번 실행된다. 항목 수에 비례하여 쿼리 수가 증가하는 전형적인 **N+1 문제**이다.

##### 해결

```java
// 최적화 후: 한번에 조회 후 Map으로 변환
Map<ConsentType, UserConsent> existingMap = userConsentRepository.findByUserId(userId)
        .stream()
        .collect(Collectors.toMap(UserConsent::getConsentType, Function.identity()));

for (ConsentRequest req : requests) {
    UserConsent existing = existingMap.get(req.consentType());  // 메모리 조회 (O(1))
    ...
}
```

##### 예상 성능 개선 수치

| 지표 | 개별 조회 (Before) | 배치 조회 (After) | 개선율 |
|------|-------------------|------------------|--------|
| DB 쿼리 수 (동의 5개) | 5회 | 1회 | **80% 감소** |
| DB 쿼리 수 (동의 10개) | 10회 | 1회 | **90% 감소** |
| 네트워크 왕복 | N회 | 1회 | **N → 1** |
| 예상 응답 시간 | ~25ms (5ms × 5) | ~5ms | **~5배** |

#### 어려웠던 점

- **JPQL에서 CASE WHEN + COALESCE 조합을 사용한 조건부 집계는 문법에 주의가 필요했다.** 특히 `@Enumerated(EnumType.STRING)` 필드를 JPQL에서 문자열 리터럴(`'INCOME'`)로 비교해야 하는데, Hibernate 버전에 따라 동작이 다를 수 있어 기존 쿼리 패턴을 참고하여 검증했다.
- **테스트 코드 수정이 동반되었다.** TaxEstimationService의 쿼리 방식이 바뀌면서, 기존 Mock 테스트가 `findByUserIdAndEntryDateBetween`을 stubbing 하고 있었지만 더 이상 호출되지 않아 테스트가 깨졌다. `aggregateByUserIdAndDateRange`를 반환하도록 Mock을 수정하고, 테스트가 DB 집계 결과를 올바르게 검증하도록 재작성했다.

---

### 2.4 Phase 4: Redis 캐싱 적용

#### 왜 이 방식을 선택했는가

세목분류에 사용되는 **MCC 룰(MccTaxRule)**과 **가맹점 정보(Merchant)**는 데이터 변경이 거의 없는 참조 테이블이다. 매 결제 분류 시마다 이 테이블들을 DB에서 조회하는 것은 불필요한 I/O이다.

또한 **세금 일정(TaxCalendar)**은 하드코딩된 정적 데이터이고, **접대비 누적 금액**은 같은 요청 내에서 반복 조회될 수 있다.

이미 Redis가 JWT 토큰 관리용으로 구성되어 있었으므로, 추가 인프라 없이 Spring Cache + Redis를 활용하였다.

#### 변경 내역

**CacheConfig.java (신규)**

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 TTL: 1시간, 캐시별 TTL 개별 설정
        Map<String, RedisCacheConfiguration> configs = Map.of(
            "mccTaxRules",        defaultConfig.entryTtl(Duration.ofHours(1)),
            "merchants",          defaultConfig.entryTtl(Duration.ofHours(1)),
            "taxDeadlines",       defaultConfig.entryTtl(Duration.ofHours(1)),
            "entertainmentUsed",  defaultConfig.entryTtl(Duration.ofMinutes(5))
        );
        ...
    }
}
```

**캐싱 적용 위치**

| 캐시 이름 | 적용 위치 | TTL | 캐시 키 | 설명 |
|-----------|----------|-----|--------|------|
| `mccTaxRules` | MccTaxRuleRepository | 1시간 | MCC 코드 | MCC별 세목분류 룰 |
| `merchants` | MerchantRepository | 1시간 | 가맹점명 | 가맹점 정보 |
| `taxDeadlines` | TaxCalendarService | 1시간 | 날짜 | 세금 신고 일정 |
| `entertainmentUsed` | EntertainmentLimitService | 5분 | 유저ID | 접대비 연간 누적액 |

#### 예상 성능 개선 수치

**시나리오: 동일 MCC 코드로 100건 연속 분류 요청**

| 지표 | 캐시 미적용 (Before) | Redis 캐시 적용 (After) | 개선율 |
|------|-------------------|----------------------|--------|
| MCC 룰 DB 조회 | 100회 | 1회 (최초만) + 99회 캐시 hit | **99% DB 부하 감소** |
| 가맹점 DB 조회 | 100회 | 1회 + 99회 캐시 hit | **99% DB 부하 감소** |
| 평균 응답 시간 | 10~30ms (DB 조회) | ~1ms (캐시 hit) | **10~30배** |
| DB 커넥션 사용 | 매 요청마다 점유 | 캐시 hit 시 미사용 | **커넥션 여유 확보** |

**캐시 TTL 설계 근거**

| 데이터 유형 | 변경 빈도 | TTL | 이유 |
|-----------|---------|-----|------|
| MCC 룰 | 거의 없음 (세법 개정 시) | 1시간 | 변경이 드물고, 1시간 후 자동 갱신 |
| 가맹점 정보 | 드물음 (신규 가맹점 등록 시) | 1시간 | 같은 근거 |
| 세금 일정 | 없음 (정적 데이터) | 1시간 | 날짜별 캐시, 계산 비용 절감 |
| 접대비 누적 | 장부 생성 시 변경 | 5분 | 실시간성 필요, 짧은 TTL로 정합성 보장 |

#### 어려웠던 점

- **Spring Data JPA Repository에 @Cacheable을 적용할 때**, 프록시 기반 AOP가 인터페이스 메서드에도 정상 동작하는지 확인이 필요했다. Spring Data JPA는 내부적으로 프록시 객체를 생성하므로 @Cacheable이 정상적으로 인터셉트된다.
- **캐시 직렬화 이슈:** Redis에 JPA 엔티티를 직접 저장하면 Lazy 프록시 직렬화 문제가 발생할 수 있다. `GenericJackson2JsonRedisSerializer`를 사용하여 JSON으로 직렬화하도록 설정했다.

---

### 2.5 Phase 5: @Transactional(readOnly = true) 최적화

#### 왜 이 방식을 선택했는가

`@Transactional(readOnly = true)`를 명시하면:

1. **Hibernate의 dirty checking(변경 감지)을 생략한다** — 엔티티 스냅샷을 유지하지 않아 메모리와 CPU를 절약
2. **flush를 생략한다** — 트랜잭션 종료 시 불필요한 DB 동기화 방지
3. **DB 레플리카 라우팅이 가능하다** — Read Replica가 있을 때 읽기 트래픽을 분산

#### 변경 내역

| 서비스 | 메서드 | 변경 사항 |
|--------|-------|----------|
| AuthService | `reissue()` | `@Transactional(readOnly = true)` 추가 |

> CardService, PaymentService, ExportService 등은 이미 클래스 레벨에 `@Transactional(readOnly = true)` 적용됨

#### 예상 효과

| 지표 | readOnly 미적용 | readOnly 적용 | 효과 |
|------|---------------|--------------|------|
| 엔티티 스냅샷 메모리 | 조회 엔티티마다 복사본 유지 | 스냅샷 미생성 | **메모리 ~50% 절감** |
| flush 실행 | 트랜잭션 종료 시 실행 | 생략 | **불필요한 SQL 방지** |
| DB 레플리카 활용 | 불가 | 가능 | **Read 트래픽 분산 가능** |

---

## 3. 종합 성능 개선 요약

### 3.1 최적화 전후 비교 (장부 10만 건, 동시 사용자 100명 기준 추정)

| 카테고리 | 최적화 항목 | Before | After | 개선율 |
|---------|-----------|--------|-------|--------|
| **DB 조회** | Payment 유저별 조회 | ~300ms (Full Scan) | ~3ms (Index Scan) | **100배** |
| **DB 조회** | User PIN 로그인 | ~100ms (Full Scan) | ~2ms (Index Scan) | **50배** |
| **메모리** | 세금 추정 집계 | ~50MB (전체 로드) | ~32B (DB 집계) | **99.99%↓** |
| **메모리** | CSV 내보내기 | ~50MB (전체 로드) | ~250KB (페이징) | **99.5%↓** |
| **쿼리 수** | 동의 저장 (5건) | 5회 쿼리 | 1회 쿼리 | **80%↓** |
| **쿼리 수** | 세목분류 (캐시 hit) | 3회 DB 조회 | 0회 (캐시) | **100%↓** |
| **처리량** | 배치 INSERT 100건 | 100회 SQL | 5회 SQL | **95%↓** |
| **동시성** | 커넥션 풀 | 10개 (기본값) | 20개 | **2배** |
| **동시성** | 커넥션 점유 시간 | Controller 끝까지 | Service 끝나면 반환 | **~60%↓** |

### 3.2 수정 파일 목록 (총 20개)

| 파일 | Phase | 변경 유형 |
|------|-------|----------|
| `Payment.java` | 1 | @Table 인덱스 4개 추가 |
| `Card.java` | 1 | @Table 인덱스 1개 추가 |
| `User.java` | 1 | @Table 인덱스 1개 추가 |
| `UserConsent.java` | 1 | @Table 인덱스 1개 추가 |
| `application.yaml` | 2 | Hibernate 배치/fetch 설정 6개 추가 |
| `application-local.yaml` | 2 | HikariCP 풀 설정 5개 추가 |
| `docker-compose.yml` | 2 | PostgreSQL 튜닝 파라미터 4개 추가 |
| `BookEntryRepository.java` | 3 | DB 집계 쿼리(aggregateByUserIdAndDateRange) 추가 |
| `TaxEstimationService.java` | 3 | Java 집계 → DB 집계 전환 |
| `ExportService.java` | 3 | Pageable.unpaged() → 페이지 단위 처리 |
| `ConsentService.java` | 3 | N+1 개별 조회 → 배치 조회 + Map |
| `CardRepository.java` | 3 | 페이지네이션 지원 메서드 추가 |
| `CacheConfig.java` | 4 | **신규** — Redis 캐시 매니저 설정 |
| `MccTaxRuleRepository.java` | 4 | @Cacheable 적용 |
| `MerchantRepository.java` | 4 | @Cacheable 적용 |
| `EntertainmentLimitService.java` | 4 | @Cacheable 적용 |
| `TaxCalendarService.java` | 4 | @Cacheable 적용 |
| `AuthService.java` | 5 | readOnly 트랜잭션 추가 |
| `ConsentServiceTest.java` | — | 배치 조회 방식에 맞게 Mock 수정 |
| `TaxEstimationServiceTest.java` | — | DB 집계 방식에 맞게 Mock 수정 |

---

## 4. 아키텍처 판단 기록

### 4.1 왜 Elasticsearch 등 별도 검색엔진 대신 인덱스 + 캐시를 선택했는가

현재 규모(예상 10만 사용자)에서는 PostgreSQL의 B-Tree 인덱스와 Redis 캐시로 충분히 대응 가능하다. Elasticsearch 등의 도입은 운영 복잡도를 높이고, 데이터 동기화 문제를 야기한다. **적절한 기술 수준의 솔루션을 선택하는 것**이 과도한 인프라보다 중요하다고 판단했다.

### 4.2 왜 CQRS/이벤트 소싱 대신 단일 DB 최적화를 선택했는가

Tax7i의 읽기/쓰기 비율은 약 7:3으로, CQRS를 도입할 만큼 극단적이지 않다. 단일 PostgreSQL에서 인덱스, 배치, 캐싱으로 충분한 성능을 확보할 수 있으며, 시스템 복잡도를 낮게 유지하는 것이 유지보수와 장애 대응에 유리하다.

### 4.3 캐시 무효화(Invalidation) 전략

가장 어려운 부분은 **캐시된 데이터와 DB 데이터의 정합성**이다. TTL 기반 만료를 1차 전략으로 채택한 이유는:

- MCC 룰/가맹점 데이터는 관리자가 수동으로 변경하는 빈도가 매우 낮아 TTL 1시간이면 충분
- 접대비 누적은 TTL 5분으로 실시간성과 DB 부하의 균형을 잡음
- 명시적 eviction(`@CacheEvict`)은 장부 생성 이벤트에 연동 가능하나, 현재 규모에서는 TTL로 충분

---

## 5. 검증 방법

```bash
# 1. 전체 테스트 실행 (120개 통과 확인)
./gradlew clean test

# 2. Hibernate SQL 로그로 배치 실행 확인
# application.yaml에 show_sql: true 설정 후 서버 기동
# INSERT 문이 개별이 아닌 배치로 실행되는지 로그 확인

# 3. 인덱스 생성 확인
docker exec -it <postgres-container> psql -U ssafy -d tax7i -c "\di"

# 4. 캐시 동작 확인
docker exec -it <redis-container> redis-cli -a ssafy KEYS "*"

# 5. 쿼리 실행 계획 확인 (인덱스 사용 여부)
EXPLAIN ANALYZE SELECT * FROM payments WHERE user_id = 1 AND status = 'CAPTURED';
```

---

*작성일: 2026-03-19*
*프로젝트: SSAFY 14기 특화프로젝트 — Tax7i (7반 1조)*
