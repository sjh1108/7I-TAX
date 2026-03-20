-- ============================================================
-- 7iTAX Schema: merchant / mcc_tax_rule / merchant_keyword_mapping
-- PostgreSQL
-- ============================================================

-- 1. 가맹점 마스터
CREATE TABLE merchant (
    id              BIGSERIAL       PRIMARY KEY,
    merchant_name   VARCHAR(200)    NOT NULL,               -- 가맹점명 (예: 스타벅스, AWS)
    mcc             VARCHAR(10)     NOT NULL,               -- MCC 코드 (예: 5814, 5817)
    category        VARCHAR(100),                           -- 가맹점 유형 (예: 카페, SW구독/디지털상품)
    example_names   TEXT,                                   -- 동일 유형 예시 (콤마 구분)
    is_domestic     BOOLEAN         NOT NULL DEFAULT TRUE,  -- 국내(true) / 해외(false)
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_merchant_mcc ON merchant (mcc);
CREATE INDEX idx_merchant_name ON merchant (merchant_name);

COMMENT ON TABLE  merchant IS '가맹점 마스터 – seed-reference.md 가맹점→MCC 매핑 기준';
COMMENT ON COLUMN merchant.mcc IS 'Merchant Category Code (ISO 18245)';

-- 2. MCC → 세목 매핑 룰
CREATE TABLE mcc_tax_rule (
    id                  BIGSERIAL       PRIMARY KEY,
    mcc                 VARCHAR(10)     NOT NULL,               -- MCC 코드
    tier                VARCHAR(1)      NOT NULL                -- A: 자동확정, B: 조건분기
                            CHECK (tier IN ('A', 'B')),
    condition_expr      TEXT,                                   -- Tier B 조건 설명 (예: '거래처 동행', '100만원 이상')
    tax_category        VARCHAR(50)     NOT NULL,               -- 세목 (여비교통비, 차량유지비, 지급수수료 …)
    vat_deductible      VARCHAR(30)     NOT NULL DEFAULT '확인필요',
                                                                -- 공제 / 불공제 / 면세 / 확인필요
    amount_threshold    BIGINT,                                 -- 금액 기준 (원). NULL이면 금액 무관
    remark              TEXT,                                   -- 비고 / 추가 설명
    legal_basis         VARCHAR(200),                           -- 세법 근거 (예: 소득세법§33①5)
    annual_limit        BIGINT,                                 -- 연간 한도 (원). NULL이면 무제한
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_mcc_tax_rule_mcc ON mcc_tax_rule (mcc);
CREATE INDEX idx_mcc_tax_rule_tier ON mcc_tax_rule (tier);
CREATE UNIQUE INDEX uq_mcc_tax_rule_mcc_condition ON mcc_tax_rule (mcc, COALESCE(condition_expr, ''));

COMMENT ON TABLE  mcc_tax_rule IS 'MCC→세목 분류 룰 (Tier A: 자동확정 / Tier B: 조건분기)';
COMMENT ON COLUMN mcc_tax_rule.tier IS 'A = MCC만으로 자동확정, B = 조건 분기 필요';
COMMENT ON COLUMN mcc_tax_rule.vat_deductible IS '부가세 매입세액공제 여부: 공제 / 불공제 / 면세 / 확인필요';

-- 3. 가맹점 키워드 매칭 (MCC 5817 전용)
CREATE TABLE merchant_keyword_mapping (
    id              BIGSERIAL       PRIMARY KEY,
    keyword         VARCHAR(100)    NOT NULL,               -- 가맹점명 키워드 (예: AWS, GitHub, 인프런)
    mcc             VARCHAR(10)     NOT NULL DEFAULT '5817',-- 대상 MCC (기본 5817)
    tax_category    VARCHAR(50)     NOT NULL,               -- 매핑 세목 (지급수수료 / 교육훈련비)
    is_domestic     BOOLEAN         NOT NULL DEFAULT TRUE,  -- 국내 여부
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_keyword_mcc UNIQUE (keyword, mcc)
);

CREATE INDEX idx_mkm_keyword ON merchant_keyword_mapping (keyword);
CREATE INDEX idx_mkm_tax_category ON merchant_keyword_mapping (tax_category);

COMMENT ON TABLE  merchant_keyword_mapping IS '가맹점 키워드 매칭 – MCC 5817(SW구독/디지털상품) 세목 분기용';
COMMENT ON COLUMN merchant_keyword_mapping.keyword IS '가맹점명에 포함될 키워드 (대소문자 구분 없이 매칭 권장)';
