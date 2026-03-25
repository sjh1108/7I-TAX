package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.entity.TaxBracket;
import com.ssafy.tax7i.tax.repository.TaxBracketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaxCalculationEngine {

    private final TaxBracketRepository taxBracketRepository;
    private final BookEntryRepository bookEntryRepository;

    @Cacheable(value = "taxBrackets", key = "#year")
    public List<TaxBracket> loadBrackets(int year) {
        return taxBracketRepository.findByYearOrderByBracketMinAsc(year);
    }

    /** 장부 집계 + 세금 계산을 한 번에 수행 (Controller → Service 레이어 규칙 준수) */
    public TaxCalculationResult calculateForUser(Long userId, int taxYear,
                                                  Long prepaidTaxInput, Map<String, Long> deductionsInput) {
        LocalDate start = LocalDate.of(taxYear, 1, 1);
        LocalDate end = LocalDate.of(taxYear, 12, 31);
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long prepaidTax = prepaidTaxInput != null ? prepaidTaxInput : 0L;
        Map<String, Long> deductions = deductionsInput != null ? deductionsInput : Collections.emptyMap();
        return calculate(taxYear, agg.totalIncome(), agg.totalExpense(), prepaidTax, deductions);
    }

    public TaxCalculationResult calculate(int taxYear, long totalRevenue, long totalExpense,
                                          long prepaidTax, Map<String, Long> deductions) {

        // 1. 소득금액 = 총수입 - 총경비
        long incomeAmount = totalRevenue - totalExpense;

        // 2. 총 공제액 합산
        long totalDeductions = 0L;
        if (deductions != null) {
            for (Long value : deductions.values()) {
                if (value != null) {
                    totalDeductions += value;
                }
            }
        }

        // 3. 과세표준 = max(0, 소득금액 - 총공제액)
        long taxableIncome = Math.max(0, incomeAmount - totalDeductions);

        // 4. 세율 구간 로드
        List<TaxBracket> brackets = loadBrackets(taxYear);
        if (brackets.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    taxYear + "년도 세율 구간 데이터가 없습니다.");
        }

        // 5. 적용 세율 및 산출세액 계산
        double taxRate = 0.0;
        long calculatedTax = 0L;
        boolean matched = false;

        for (TaxBracket bracket : brackets) {
            if (taxableIncome >= bracket.getBracketMin() && taxableIncome <= bracket.getBracketMax()) {
                taxRate = bracket.getRate();
                calculatedTax = (long) Math.floor(taxableIncome * bracket.getRate() - bracket.getProgressiveDeduction());
                matched = true;
                break;
            }
        }

        if (!matched && taxableIncome > 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "과세표준에 해당하는 세율 구간이 없습니다.");
        }

        // 6. 결정세액 = max(0, 산출세액)
        long determinedTax = Math.max(0, calculatedTax);

        // 7. 최종 납부/환급 세액 = 결정세액 - 기납부세액
        long finalTax = determinedTax - prepaidTax;

        // 8. 지방소득세 = floor(결정세액 × 0.10)
        long localTax = (long) Math.floor(determinedTax * 0.10);

        // 9. 환급 여부
        boolean isRefund = finalTax < 0;

        return new TaxCalculationResult(
                totalRevenue,
                totalExpense,
                incomeAmount,
                totalDeductions,
                taxableIncome,
                taxRate,
                calculatedTax,
                determinedTax,
                prepaidTax,
                finalTax,
                localTax,
                isRefund
        );
    }
}
