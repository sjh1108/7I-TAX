package com.ssafy.tax7i.classification.repository;

import com.ssafy.tax7i.classification.entity.MccTaxRule;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface MccTaxRuleRepository extends JpaRepository<MccTaxRule, Long> {

    @Cacheable(value = "mccTaxRules", key = "#mcc")
    List<MccTaxRule> findByMccOrderByTierAscIdAsc(String mcc);

    List<MccTaxRule> findByMccAndTier(String mcc, String tier);
}
