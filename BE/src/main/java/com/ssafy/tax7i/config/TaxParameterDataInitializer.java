package com.ssafy.tax7i.config;

import com.ssafy.tax7i.tax.entity.TaxParameter;
import com.ssafy.tax7i.tax.repository.TaxParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class TaxParameterDataInitializer implements CommandLineRunner {

    private final TaxParameterRepository taxParameterRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (taxParameterRepository.countByYear(2025) > 0) {
            log.info("[TaxParameterDataInitializer] 2025년 세금 파라미터가 이미 존재합니다. 건너뜁니다.");
            return;
        }

        log.info("[TaxParameterDataInitializer] 2025년 세금 파라미터를 초기화합니다.");

        List<TaxParameter> params = List.of(
                // 지방소득세율 (지방세법 §92)
                TaxParameter.builder()
                        .year(2025).category("LOCAL_TAX").paramKey("rate").paramValue("0.10")
                        .description("지방소득세율").legalBasis("지방세법 §92").build(),

                // 부가가치세율 (부가가치세법 §30)
                TaxParameter.builder()
                        .year(2025).category("VAT").paramKey("rate").paramValue("0.10")
                        .description("부가가치세율").legalBasis("부가가치세법 §30").build(),

                // 기본공제 본인 (소득세법 §50)
                TaxParameter.builder()
                        .year(2025).category("BASIC_DEDUCTION").paramKey("personal").paramValue("1500000")
                        .description("기본공제 본인").legalBasis("소득세법 §50").build(),

                // 노란우산공제 (소기업소상공인공제부금법)
                TaxParameter.builder()
                        .year(2025).category("NORAN_DEDUCTION").paramKey("threshold").paramValue("40000000")
                        .description("노란우산공제 소득분기점").legalBasis("소기업소상공인공제부금법").build(),
                TaxParameter.builder()
                        .year(2025).category("NORAN_DEDUCTION").paramKey("limit_low").paramValue("5000000")
                        .description("소득 이하 공제한도").legalBasis("소기업소상공인공제부금법").build(),
                TaxParameter.builder()
                        .year(2025).category("NORAN_DEDUCTION").paramKey("limit_high").paramValue("3000000")
                        .description("소득 초과 공제한도").legalBasis("소기업소상공인공제부금법").build(),

                // 연금저축 세액공제 (소득세법 §50)
                TaxParameter.builder()
                        .year(2025).category("PENSION_CREDIT").paramKey("threshold").paramValue("55000000")
                        .description("연금저축 소득분기점").legalBasis("소득세법 §50").build(),
                TaxParameter.builder()
                        .year(2025).category("PENSION_CREDIT").paramKey("rate_low").paramValue("0.15")
                        .description("소득 이하 세액공제율").legalBasis("소득세법 §50").build(),
                TaxParameter.builder()
                        .year(2025).category("PENSION_CREDIT").paramKey("rate_high").paramValue("0.132")
                        .description("소득 초과 세액공제율").legalBasis("소득세법 §50").build(),
                TaxParameter.builder()
                        .year(2025).category("PENSION_CREDIT").paramKey("limit").paramValue("6000000")
                        .description("연금저축 연간 한도").legalBasis("소득세법 §50").build(),

                // 접대비 (소득세법 §35)
                TaxParameter.builder()
                        .year(2025).category("ENTERTAINMENT").paramKey("annual_limit").paramValue("12000000")
                        .description("접대비 연간 기본한도").legalBasis("소득세법 §35").build(),

                // 교육훈련비 (소득세법 §19)
                TaxParameter.builder()
                        .year(2025).category("EDUCATION").paramKey("recommended_limit").paramValue("1500000")
                        .description("교육훈련비 추천한도").legalBasis("소득세법 §19").build()
        );

        taxParameterRepository.saveAll(params);
        log.info("[TaxParameterDataInitializer] 세금 파라미터 {}개 저장 완료.", params.size());
    }
}
