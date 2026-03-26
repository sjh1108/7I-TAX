package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.entity.TaxParameter;
import com.ssafy.tax7i.tax.repository.TaxParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxParameterService {

    private final TaxParameterRepository taxParameterRepository;

    @Cacheable(value = "taxParameters", key = "#year + ':' + #category")
    public Map<String, String> getParams(int year, String category) {
        List<TaxParameter> params = taxParameterRepository.findByYearAndCategory(year, category);
        if (params.isEmpty()) {
            log.info("{}년 {} 파라미터 없음, {}년 폴백 시도", year, category, year - 1);
            params = taxParameterRepository.findByYearAndCategory(year - 1, category);
        }
        if (params.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    year + "년 " + category + " 세금 파라미터가 없습니다.");
        }
        return params.stream()
                .collect(Collectors.toMap(TaxParameter::getParamKey, TaxParameter::getParamValue));
    }

    /** 지방소득세율 (지방세법 §92) */
    public double getLocalTaxRate(int year) {
        return Double.parseDouble(getParams(year, "LOCAL_TAX").get("rate"));
    }

    /** 부가가치세율 (부가가치세법 §30) */
    public double getVatRate(int year) {
        return Double.parseDouble(getParams(year, "VAT").get("rate"));
    }

    /** 기본공제 본인 (소득세법 §50) */
    public long getBasicDeduction(int year) {
        return Long.parseLong(getParams(year, "BASIC_DEDUCTION").get("personal"));
    }

    /** 노란우산공제 한도 (소기업소상공인공제부금법) */
    public long getNoranLimit(int year, long totalRevenue) {
        Map<String, String> params = getParams(year, "NORAN_DEDUCTION");
        long threshold = Long.parseLong(params.get("threshold"));
        return totalRevenue <= threshold
                ? Long.parseLong(params.get("limit_low"))
                : Long.parseLong(params.get("limit_high"));
    }

    /** 연금저축 세액공제율 (소득세법 §50) */
    public double getPensionCreditRate(int year, long totalRevenue) {
        Map<String, String> params = getParams(year, "PENSION_CREDIT");
        long threshold = Long.parseLong(params.get("threshold"));
        return totalRevenue <= threshold
                ? Double.parseDouble(params.get("rate_low"))
                : Double.parseDouble(params.get("rate_high"));
    }

    /** 연금저축 연간 한도 */
    public long getPensionLimit(int year) {
        return Long.parseLong(getParams(year, "PENSION_CREDIT").get("limit"));
    }

    /** 접대비 연간 기본한도 (소득세법 §35) */
    public long getEntertainmentLimit(int year) {
        return Long.parseLong(getParams(year, "ENTERTAINMENT").get("annual_limit"));
    }

    /** 교육훈련비 추천한도 (소득세법 §19) */
    public long getEducationLimit(int year) {
        return Long.parseLong(getParams(year, "EDUCATION").get("recommended_limit"));
    }
}
