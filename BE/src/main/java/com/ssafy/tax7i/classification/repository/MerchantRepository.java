package com.ssafy.tax7i.classification.repository;

import com.ssafy.tax7i.classification.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    @Cacheable(value = "merchants", key = "#merchantName")
    Optional<Merchant> findFirstByMerchantName(String merchantName);

    @Query("SELECT m FROM Merchant m WHERE :name LIKE CONCAT('%', m.merchantName, '%')")
    List<Merchant> findByMerchantNameContainedIn(String name);

    List<Merchant> findByMcc(String mcc);
}
