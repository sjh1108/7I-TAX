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

    private String requireParam(Map<String, String> params, String category, String key) {
        String value = params.get(key);
        if (value == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    category + " 파라미터에 " + key + " 값이 없습니다.");
        }
        return value;
    }

    /** 지방소득세율 (지방세법 §92) */
    public double getLocalTaxRate(int year) {
        Map<String, String> params = getParams(year, "LOCAL_TAX");
        return Double.parseDouble(requireParam(params, "LOCAL_TAX", "rate"));
    }

    /** 부가가치세율 (부가가치세법 §30) */
    public double getVatRate(int year) {
        Map<String, String> params = getParams(year, "VAT");
        return Double.parseDouble(requireParam(params, "VAT", "rate"));
    }

    /** 기본공제 본인 (소득세법 §50) */
    public long getBasicDeduction(int year) {
        Map<String, String> params = getParams(year, "BASIC_DEDUCTION");
        return Long.parseLong(requireParam(params, "BASIC_DEDUCTION", "personal"));
    }

    /** 노란우산공제 한도 (소기업소상공인공제부금법) */
    public long getNoranLimit(int year, long totalRevenue) {
        Map<String, String> params = getParams(year, "NORAN_DEDUCTION");
        long threshold = Long.parseLong(requireParam(params, "NORAN_DEDUCTION", "threshold"));
        return totalRevenue <= threshold
                ? Long.parseLong(requireParam(params, "NORAN_DEDUCTION", "limit_low"))
                : Long.parseLong(requireParam(params, "NORAN_DEDUCTION", "limit_high"));
    }

    /** 연금저축 세액공제율 (소득세법 §50) */
    public double getPensionCreditRate(int year, long totalRevenue) {
        Map<String, String> params = getParams(year, "PENSION_CREDIT");
        long threshold = Long.parseLong(requireParam(params, "PENSION_CREDIT", "threshold"));
        return totalRevenue <= threshold
                ? Double.parseDouble(requireParam(params, "PENSION_CREDIT", "rate_low"))
                : Double.parseDouble(requireParam(params, "PENSION_CREDIT", "rate_high"));
    }

    /** 연금저축 연간 한도 */
    public long getPensionLimit(int year) {
        Map<String, String> params = getParams(year, "PENSION_CREDIT");
        return Long.parseLong(requireParam(params, "PENSION_CREDIT", "limit"));
    }

    /** 접대비 연간 기본한도 (소득세법 §35) */
    public long getEntertainmentLimit(int year) {
        Map<String, String> params = getParams(year, "ENTERTAINMENT");
        return Long.parseLong(requireParam(params, "ENTERTAINMENT", "annual_limit"));
    }

    /** 교육훈련비 추천한도 (소득세법 §19) */
    public long getEducationLimit(int year) {
        Map<String, String> params = getParams(year, "EDUCATION");
        return Long.parseLong(requireParam(params, "EDUCATION", "recommended_limit"));
    }
}
