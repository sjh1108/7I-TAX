# 7iTAX 참조 데이터 (Claude CLI 태스크 보조 자료)

> 이 파일을 프로젝트 루트에 `REFERENCE.md`로 두면, Claude CLI가 코드 생성 시 참조할 수 있다.
> 각 태스크 프롬프트에서 "REFERENCE.md를 먼저 읽고 작업해줘"를 추가하면 된다.

---

## 1. 간편장부 세목코드 전체 매핑표

간편장부에서 사용하는 비용 항목 분류. BookEntry의 카테고리와 매핑되어야 한다.

| 코드 | 세목명 | 설명 | 대표 MCC | 비고 |
|------|--------|------|----------|------|
| 11 | 매입비용 | 상품/원재료 매입 | 5045, 5065 | 재판매 목적 구매 |
| 14 | 임차료 | 사무실 월세, 공유오피스 | — | 사업장 임대료 |
| 15 | 인건비 | 직원 급여, 일용직 | — | 1인 사업자는 거의 없음 |
| 16 | 여비교통비 | 택시, KTX, 항공, 숙박 | 4111, 4121, 4112, 7011 | 업무 목적 이동/출장 |
| 17 | 접대비 | 거래처 식사, 선물 | 5812(고액), 5811 | 연 1,200만원 한도 (수입 1억 이하) |
| 18 | 보험료 | 사업용 보험 | 6300 | 건강보험, 산재보험 등 |
| 19 | 수선비 | 장비 수리, 유지보수 | 7629 | 사업용 자산 수리 |
| 20 | 소모품비 | 문구, 사무용품, 100만원 미만 전자기기 | 5732, 5943 | 100만원 미만 물품 |
| 21 | 지급수수료 | SW 구독, 외주비, 세무기장료, 앱스토어 등록비 | 5817, 5734, 7399, 8999 | ❌ 소모품비(20)가 아님! |
| 22 | 차량유지비 | 주유, 수리, 보험, 렌터카 | 5541, 7512, 7538 | 업무용 차량 |
| 23 | 복리후생비 | 1인 식대, 음료, 간식 | 5812(소액), 5411, 5815 | 연 1,500만원 한도 |
| 24 | 통신비 | 휴대폰, 인터넷 | 4814 | 업무용 통신 |
| 25 | 도서인쇄비 | 전문서적, 인쇄물 | 5942, 2741 | 업무 관련 서적 |
| 26 | 광고선전비 | 온라인 광고, 명함 | 7311 | 사업 홍보 |
| 27 | 연구비 | R&D, 교육, 세미나 | 8299 | IT 개발자 교육비 포함 |
| 30 | 감가상각비 | 100만원 이상 고정자산의 연간 감가비 | — | 취득가 ÷ 내용연수(4년) |
| 40 | 고정자산 | 100만원 이상 물품 취득 (즉시 비용 아님) | 5045(고액) | 장부의 고정자산 증가 컬럼 |

### 분류 판단 규칙 (코드에 반영)

```
같은 MCC 5812(식당)인데:
- 금액 3만원 미만 + 1인 → 복리후생비(23)
- 금액 3만원 이상 또는 동행 있음 → 접대비(17)

같은 MCC 5045(전자기기)인데:
- 금액 100만원 미만 → 소모품비(20)
- 금액 100만원 이상 → 고정자산(40) + 감가상각 4년

SW 구독(5817): 항상 지급수수료(21)
- ❌ 소모품비(20)로 분류하면 안 됨 (무형 서비스이므로)
```

---

## 2. 종합소득세 확정신고서 필드 구조

> 소득세법 시행규칙 별지 제40(1)호 — "종합소득세·농어촌특별세 과세표준확정신고 및 납부계산서"

### 2.1 인적사항 섹션

```
필드명                  타입        설명
taxpayerName           String     성명
residentNumber         String     주민등록번호 (앞6-뒤1******)
address                String     주소
businessNumber         String     사업자등록번호 (123-45-67890)
taxOfficeCode          String     관할세무서 코드
taxOfficeName          String     관할세무서명 (예: "광주세무서")
filingDate             LocalDate  신고일자
```

### 2.2 사업소득명세 섹션

```
필드명                  타입        설명                              자동화
businessCode           String     업종코드 (722000)                  AUTO (User에서)
businessType           String     업태 ("서비스")                     AUTO
businessItem           String     종목 ("소프트웨어 개발")             AUTO
bookkeepingType        String     기장의무 ("간편장부")                AUTO (고정값)
totalRevenue           long       총수입금액                          AUTO (BookEntry 합산)
incomeAmount           long       소득금액                            AUTO (총수입-필요경비)
```

### 2.3 세액계산 섹션 (핵심 — 빈칸이 코드의 변수가 됨)

```
번호  필드명                  타입     계산식                                     자동화
①   comprehensiveIncome    long    = incomeAmount (사업소득만)                  AUTO
②   totalDeductions        long    = 소득공제 합계                              AUTO
③   taxableIncome          long    = ① - ②  (0 미만이면 0)                    AUTO
④   taxRate                double  = 세율표에서 조회                            AUTO
⑤   calculatedTax          long    = ③ × ④ - 누진공제                         AUTO
⑥   taxCredit              long    = 세액공제 합계 (기장세액공제 등)              AUTO (현재 0)
⑦   determinedTax          long    = max(0, ⑤ - ⑥)                           AUTO
⑧   prepaidTax             long    = 기납부세액 (3.3% 원천징수분)                USER 입력
⑨   finalTax               long    = ⑦ - ⑧  (음수면 환급)                     AUTO
⑩   localTax               long    = floor(⑦ × 0.10)                         AUTO (별도 납부)
```

### 2.4 소득공제 상세 (deductionsJson)

```json
{
    "기본공제_본인": 1500000,
    "국민연금보험료": 2400000,
    "건강보험료": 800000,
    "노란우산공제": 0,
    "연금저축": 0,
    "개인연금저축": 0,
    "소기업소상공인공제": 0
}
```

각 공제 항목의 한도:

| 공제 항목 | 한도 | 법적 근거 |
|-----------|------|-----------|
| 기본공제 (본인) | 150만원 (고정) | 소득세법 제50조 |
| 국민연금보험료 | 납입액 전액 | 소득세법 제51조의3 |
| 건강보험료 | 납입액 전액 | 소득세법 제52조 |
| 노란우산공제 | 연 500만원 (수입 4천만 이하) / 300만원 (4천~1억) | 조세특례제한법 제86조의3 |
| 연금저축 | 연 600만원 (종합소득 1억 이하) | 소득세법 제51조의3 |
| 개인연금저축 | 연 72만원 | 조세특례제한법 제86조의2 |

### 2.5 환급계좌 섹션

```
refundBankName         String     환급 수령 은행명 ("싸피뱅크")
refundAccountNumber    String     환급 수령 계좌번호
```

---

## 3. 총수입금액 및 필요경비명세서 필드 구조

> 소득세법 시행규칙 별지 제74호 부표

### 3.1 총수입금액

```
코드   항목명                금액         설명
40    장부상 수입금액        {합산}       BookEntry(INCOME) 합계
41    기타수입금액           0           사업 외 수입 (현재 미지원)
      총수입금액 합계        {합산}
```

### 3.2 필요경비

```
코드   항목명          금액              설명
11    매입비용         {세목11 합계}     BookEntry(EXPENSE) 중 코드 11
14    임차료           {세목14 합계}
15    인건비           {세목15 합계}
16    여비교통비       {세목16 합계}
17    접대비           {세목17 합계}     한도 초과분은 세무조정에서 차감
18    보험료           {세목18 합계}
19    수선비           {세목19 합계}
20    소모품비         {세목20 합계}
21    지급수수료       {세목21 합계}
22    차량유지비       {세목22 합계}
23    복리후생비       {세목23 합계}
24    통신비           {세목24 합계}
25    도서인쇄비       {세목25 합계}
26    광고선전비       {세목26 합계}
27    연구비           {세목27 합계}
30    감가상각비       {세목30 합계}
      필요경비 합계    {전체 합산}
```

→ 이 데이터가 바로 `ExpenseDetail` entity에 저장되는 것.
→ `BookEntry`를 `expenseCode`별로 GROUP BY SUM 하면 자동 생성.

---

## 4. 간편장부소득금액계산서 필드 구조

> 소득세법 시행규칙 별지 제74호

```
번호   항목명                계산식                            자동화
①    총수입금액              = 3번 문서의 총수입금액 합계        AUTO
②    필요경비 계             = 3번 문서의 필요경비 합계          AUTO
③    차감소득금액            = ① - ②                          AUTO
④    손금불산입              = 접대비 한도초과분 등               AUTO (세무조정)
⑤    익금불산입              = 0 (일반적으로)                    AUTO
⑥    소득금액                = ③ + ④ - ⑤                      AUTO
```

### 세무조정 자동화 규칙

현재 구현 범위에서 자동 반영해야 할 세무조정:

```
1. 접대비 한도 초과
   - 한도: 1,200만원 (수입 1억 이하), 1,200만 + 수입초과분×0.2% (1억 초과)
   - 접대비(17) 합계가 한도 초과 시 → 초과분을 손금불산입(④)에 가산
   - 기존 EntertainmentLimitService가 이미 있으므로 활용

2. 감가상각비 한도
   - 100만원 이상 고정자산: 내용연수 4년, 정액법
   - 연간 상각액 = 취득가 ÷ 4
   - 상각 한도를 초과하여 비용 처리한 경우 → 한도초과분 손금불산입
```

---

## 5. 납부확인서 필드 구조

> 국세청민원사무처리규정 별지서식 20호 준용 (7iTAX 시뮬레이션 버전)

```
[문서 헤더]
documentTitle          = "납부확인서"
documentSubtitle       = "Certificate of Tax Payment (시뮬레이션)"
issueNumber            = "7T-{year}-{7자리시퀀스}"    (예: 7T-2026-0000001)
issueDate              = 발급일자

[납세자 인적사항]
taxpayerName           = 성명
residentNumber         = 주민등록번호 (마스킹)
businessNumber         = 사업자등록번호
businessCode           = 업종코드 + 업태/종목
address                = 주소
taxOfficeName          = 관할세무서

[국세 납부내역]
nationalTax.taxType    = "종합소득세"
nationalTax.taxYear    = 귀속연도
nationalTax.paymentType = "확정신고 자진납부"
nationalTax.paidDate   = 납부일시
nationalTax.amount     = 납부금액
nationalTax.fromAccount = 출금계좌 (마스킹)
nationalTax.toAccount  = 국세 가상계좌 (880-XXXX)
nationalTax.transferId = SSAFY 거래ID

[지방세 납부내역]
localTax.taxType       = "개인지방소득세"
localTax.taxYear       = 귀속연도
localTax.basis         = "종합소득세 결정세액의 10%"
localTax.paidDate      = 납부일시
localTax.amount        = 납부금액
localTax.fromAccount   = 출금계좌 (마스킹)
localTax.toAccount     = 지방세 가상계좌 (770-XXXX)
localTax.transferId    = SSAFY 거래ID

[합계]
totalPaid              = 국세 + 지방세

[세액 산출 내역 (참고)]
totalRevenue           = 총수입금액
totalExpense           = 필요경비
incomeAmount           = 소득금액
taxableIncome          = 과세표준
taxRate                = 적용 세율
calculatedTax          = 산출세액
determinedTax          = 결정세액
prepaidTax             = 기납부세액

[시뮬레이션 고지문] — ⚠ 반드시 포함
disclaimer = "본 문서는 7iTAX 시뮬레이션 환경에서 생성되었으며,
              실제 납세증빙으로 사용할 수 없습니다.
              공식 납부내역증명은 국세청 홈택스(hometax.go.kr)에서
              발급받으시기 바랍니다."

[문서 푸터]
storageNotice = "장부 및 증빙서류는 소득세 확정신고기한이 지난 날부터
                 5년간 보존하여야 합니다. (국세기본법 제85조의3)"
issuer = "7iTAX (시뮬레이션)"
```

---

## 6. 부가가치세 확정신고서 필드 구조 (COULD)

> 부가가치세법 시행규칙 별지 제21호

```
[인적사항]
같은 구조 (성명, 사업자번호, 과세기간)

[과세기간]
startDate / endDate    예: 2025.07.01 ~ 2025.12.31 (2기 확정)

[매출세액]
salesByInvoice         = 세금계산서 발급분 공급가액
salesByCard            = 신용카드/현금영수증 발행분
totalSalesAmount       = 매출 공급가액 합계
totalSalesTax          = 매출세액 합계 (= totalSalesAmount × 10%)

[매입세액]
purchaseByInvoice      = 세금계산서 수취분 공급가액
purchaseByCard         = 신용카드 매입분 (사업용 카드 결제 중 매입세액 공제 대상)
totalPurchaseAmount    = 매입 공급가액 합계
totalPurchaseTax       = 매입세액 합계 (= totalPurchaseAmount × 10%)

[납부세액 계산]
payableTax             = totalSalesTax - totalPurchaseTax
preliminaryPaid        = 예정고지 납부분 (4월 또는 10월에 납부한 금액)
finalTax               = payableTax - preliminaryPaid
```

### 매입세액 공제 대상 vs 불공제 판단

```
공제 대상 (purchaseTax에 포함):
- 사업용 카드로 결제한 사업 관련 매입
- 세금계산서 수취분

불공제 (제외):
- 접대비 관련 매입세액 (부가가치세법 제39조 제1항 제1호)
- 비영업용 소형승용차 관련 (부가가치세법 제39조 제1항 제5호)
- 개인적 소비 (personal로 분류된 BookEntry)

→ BookEntry에서 business/personal 분류가 이미 있으므로:
  - business + EXPENSE → 매입세액 공제 대상 (접대비 제외)
  - personal → 불공제
  - 접대비(코드17) → 불공제
```

---

## 7. 절세 추천 항목 상세 (T7용)

| # | 추천 항목 | 유형 | 최대 공제/감면액 | 조건 | 법적 근거 |
|---|-----------|------|-----------------|------|-----------|
| 1 | 노란우산공제 (소기업·소상공인 공제부금) | 소득공제 | 500만원 (수입 4천만↓) / 300만원 (4천만~1억) | 소기업·소상공인 해당 | 조세특례제한법 제86조의3 |
| 2 | 국민연금 추가납입 | 소득공제 | 납입액 전액 | 국민연금 가입자 | 소득세법 제51조의3 |
| 3 | 연금저축 | 세액공제 | 600만원 한도 × 13.2% (=79.2만원) | 총급여 5,500만원 이하 15%, 초과 13.2% | 소득세법 제59조의3 |
| 4 | 기장세액공제 | 세액공제 | 산출세액의 20% (한도 100만원) | 간편장부 대상자가 복식부기로 신고 시 | 소득세법 제56조의2 |
| 5 | 중소기업 특별세액감면 | 세액감면 | 산출세액의 10~30% | 수도권 밖 + 소규모 | 조세특례제한법 제7조 |
| 6 | 성실신고확인비용 세액공제 | 세액공제 | 120만원 한도 | 성실신고확인서 제출 시 | 소득세법 제126조의6 |

### 추천 로직 의사코드

```
function getRecommendations(userId, taxYear):
    summary = getBookEntrySummary(userId, taxYear)
    currentDeductions = getCurrentDeductions(userId, taxYear)  // 이미 적용 중인 공제
    currentTax = calculateTax(summary, currentDeductions)

    recommendations = []

    // 1. 노란우산 체크
    if "노란우산공제" not in currentDeductions:
        limit = summary.totalRevenue <= 40_000_000 ? 5_000_000 : 3_000_000
        withDeduction = calculateTax(summary, currentDeductions + {"노란우산": limit})
        saving = currentTax.determinedTax - withDeduction.determinedTax
        recommendations.add({
            name: "노란우산공제",
            maxAmount: limit,
            estimatedSaving: saving + floor(saving * 0.10)  // 지방세 절감분 포함
        })

    // 2. 연금저축 체크
    if "연금저축" not in currentDeductions:
        creditRate = summary.totalRevenue <= 55_000_000 ? 0.15 : 0.132
        maxCredit = floor(6_000_000 * creditRate)
        recommendations.add({
            name: "연금저축",
            maxAmount: 6_000_000,
            estimatedSaving: maxCredit
        })

    // 3. 접대비 한도 여유 체크
    entertainmentUsed = summary.byCategory["17"] or 0
    entertainmentLimit = calculateEntertainmentLimit(summary.totalRevenue)
    if entertainmentUsed < entertainmentLimit:
        remaining = entertainmentLimit - entertainmentUsed
        recommendations.add({
            name: "접대비 한도 여유",
            description: "올해 접대비 {remaining}원 추가 사용 가능",
            maxAmount: remaining,
            estimatedSaving: floor(remaining * currentTax.taxRate)
        })

    return recommendations
```

---

## 8. 시연용 더미 데이터 시나리오

시연 시 사용할 구체적인 데이터. 시드 스크립트에 이 데이터를 넣어야 한다.

### 사용자 프로필

```
성명: 홍길동
주민번호: 900101-1234567
사업자번호: 123-45-67890
업종코드: 722000 (소프트웨어 개발업)
주소: 광주광역시 ○○구 ○○로 123
관할세무서: 광주세무서 (코드: 0305)
```

### 2025년 수입 내역 (BookEntry INCOME)

| 날짜 | 내용 | 금액 | 원천징수(3.3%) | 비고 |
|------|------|------|---------------|------|
| 2025-01-15 | A사 프리랜서 용역 | 3,300,000 | 108,900 | 월정액 |
| 2025-02-15 | A사 프리랜서 용역 | 3,300,000 | 108,900 | |
| ... (매월 동일) | | | | |
| 2025-12-15 | A사 프리랜서 용역 | 3,300,000 | 108,900 | |
| **합계** | | **39,600,000** | **1,306,800** | 연 12건 |

### 2025년 지출 내역 (BookEntry EXPENSE) — 시연 3건 + 연간 총합

**시연 시 실시간 결제할 3건:**

| 날짜 | 가맹점 | MCC | 금액 | 세목 | 시연 포인트 |
|------|--------|-----|------|------|------------|
| (시연) | 스타벅스 선릉역 | 5815 | 5,500 | 복리후생비(23) | 소액 음료 → 자동 분류 |
| (시연) | AWS Korea | 5817 | 85,000 | 지급수수료(21) | SW구독 → 소모품비 아님! |
| (시연) | 쿠팡 (맥북프로) | 5045 | 1,800,000 | 고정자산(40) | 100만원 이상 → 감가상각 |

**연간 지출 총합 (시드 데이터로 미리 넣어둘 것):**

| 세목코드 | 세목명 | 연간 합계 | 건수 |
|----------|--------|----------|------|
| 21 | 지급수수료 | 2,040,000 | 24 |
| 23 | 복리후생비 | 1,200,000 | 150 |
| 16 | 여비교통비 | 520,000 | 30 |
| 17 | 접대비 | 480,000 | 12 |
| 24 | 통신비 | 660,000 | 12 |
| 20 | 소모품비 | 350,000 | 8 |
| 25 | 도서인쇄비 | 440,000 | 6 |
| 18 | 보험료 | 360,000 | 4 |
| 11 | 매입비용 | 2,400,000 | 5 |
| **합계** | | **8,450,000** | **251** |

### 세액 계산 결과 (시연 시 이 숫자가 나와야 함)

```
총수입금액:          39,600,000원
필요경비:            8,450,000원
소득금액:            31,150,000원

소득공제:
  기본공제(본인):     1,500,000원
  국민연금보험료:     2,400,000원
  ─────────────
  소득공제 합계:      3,900,000원

과세표준:            27,250,000원
세율:                15% (1,400만~5,000만 구간)
산출세액:            27,250,000 × 0.15 - 1,260,000 = 2,827,500원
결정세액:            2,827,500원

기납부세액(3.3%):    1,306,800원
납부할 세액:         1,520,700원

지방소득세(10%):     282,750원

총 납부액:           1,803,450원
```

**절세 추천 적용 시 (시연 킬링 포인트):**

```
노란우산공제 300만원 적용 시:
  소득공제 합계:      6,900,000원 (+3,000,000)
  과세표준:           24,250,000원
  산출세액:           24,250,000 × 0.15 - 1,260,000 = 2,377,500원
  결정세액:           2,377,500원
  납부할 세액:        1,070,700원 (450,000원 절세!)
  지방소득세:         237,750원 (45,000원 절세!)
  총 절세액:          495,000원
```

---

## 9. 감가상각 계산 규칙

100만원 이상 고정자산 취득 시 즉시 비용 처리하지 않고 내용연수에 걸쳐 비용 처리.

```
적용 대상: 취득가액 100만원 이상 (소득세법 시행령 제62조)
상각 방법: 정액법
내용연수: 업무용 컴퓨터/전자기기 = 4년 (법인세법 시행규칙 별표5)

계산:
  연간 감가상각비 = 취득가액 ÷ 내용연수(4)
  월할 계산: 취득월이 7월이면 → 해당 연도 6개월분만 인식

예시 (맥북프로 1,800,000원, 2025년 3월 취득):
  연간 상각비 = 1,800,000 ÷ 4 = 450,000원
  2025년분 = 450,000 × (10개월/12) = 375,000원 (3월~12월)
  2026년분 = 450,000원 (전체)
  2027년분 = 450,000원
  2028년분 = 450,000원
  2029년분 = 450,000 × (2개월/12) = 75,000원 (1월~2월)
```

### 코드 반영 포인트

```java
// 고정자산 감가상각비 계산
public long calculateDepreciation(long acquisitionCost, LocalDate acquisitionDate, int targetYear) {
    int usefulLife = 4; // 컴퓨터/전자기기
    long annualDepreciation = acquisitionCost / usefulLife;

    int acquiredYear = acquisitionDate.getYear();
    int acquiredMonth = acquisitionDate.getMonthValue();

    if (targetYear == acquiredYear) {
        // 취득 연도: 월할 계산 (취득월~12월)
        int months = 13 - acquiredMonth; // 취득월 포함
        return Math.round(annualDepreciation * months / 12.0);
    } else if (targetYear == acquiredYear + usefulLife) {
        // 마지막 연도: 잔여 월수
        int months = acquiredMonth - 1;
        return Math.round(annualDepreciation * months / 12.0);
    } else if (targetYear > acquiredYear && targetYear < acquiredYear + usefulLife) {
        return annualDepreciation;
    }
    return 0;
}
```

---

## 10. 접대비 한도 계산 규칙

```
기본 한도: 1,200만원 (소득세법 시행령 제55조)

수입금액별 추가 한도:
  수입 1억원 이하:  추가 없음                    → 한도 1,200만원
  1억~5억원:      1억 초과분 × 0.2%             → 1,200만 + 초과분×0.002
  5억~100억원:    800만 + 5억 초과분 × 0.04%
  100억원 초과:   별도 계산

1인 IT 개발자(수입 7,500만원 이하) → 한도 1,200만원

한도 초과 시:
  초과분은 세무조정에서 손금불산입(비용 불인정)
  → 소득금액 증가 → 세액 증가
```

### 코드 반영 포인트

```java
// 기존 EntertainmentLimitService가 있으면 활용
public long calculateEntertainmentLimit(long totalRevenue) {
    long baseLimitKRW = 12_000_000L;

    if (totalRevenue <= 100_000_000L) {
        return baseLimitKRW;
    } else if (totalRevenue <= 500_000_000L) {
        return baseLimitKRW + (long)((totalRevenue - 100_000_000L) * 0.002);
    }
    // 5억 초과는 생략 (타겟 사용자 범위 밖)
    return baseLimitKRW;
}
```
