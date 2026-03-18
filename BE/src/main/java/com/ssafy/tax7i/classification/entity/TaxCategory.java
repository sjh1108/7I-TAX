package com.ssafy.tax7i.classification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 간편장부 세목 22개 (1인 IT개발자 기준)
 */
@Getter
@RequiredArgsConstructor
public enum TaxCategory {

    // A. 수입
    SALES("01", "매출", "사업수입"),

    // B. 매출원가
    PURCHASE("02", "상품·원재료 매입", "매출원가 관련 매입"),
    INVENTORY("03", "기초/기말 재고", "재고자산 증감"),

    // C. 일반관리비·판매비
    SALARY("04", "급료", "직원 급여·임금"),
    TAX_DUES("05", "제세공과금", "사업 관련 세금·공과금"),
    RENT("06", "임차료", "사무실·장비 대여료"),
    INTEREST("07", "지급이자", "사업자금 차입 이자"),
    ENTERTAINMENT("08", "접대비", "거래처 관계 유지 지출"),
    DONATION("09", "기부금", "기부·후원금"),
    DEPRECIATION("10", "감가상각비", "고정자산 상각"),
    VEHICLE("11", "차량유지비", "업무용 차량 유지비"),
    SERVICE_FEE("12", "지급수수료", "용역·서비스 이용 대가"),
    SUPPLIES("13", "소모품비", "100만원 미만 소규모 물품"),
    WELFARE("14", "복리후생비", "직원 복지·후생 지출"),
    SHIPPING("15", "운반비", "물품 배송·운송 비용"),
    ADVERTISING("16", "광고선전비", "홍보·마케팅 지출"),
    TRAVEL("17", "여비교통비", "출장·업무 이동 비용"),
    OTHER_EXPENSE("18", "기타 경비", "기타 사업 관련 지출"),

    // D. 고정자산
    ASSET_PURCHASE("19", "고정자산 매입", "100만원 이상 사업용 자산 취득"),
    ASSET_SALE("20", "고정자산 매도", "사업용 자산 처분");

    private final String code;
    private final String name;
    private final String description;
}
