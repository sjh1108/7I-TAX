package com.ssafy.tax7i.payment.repository;

import com.ssafy.tax7i.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.user JOIN FETCH p.account WHERE p.id = :id AND p.user.id = :userId")
    Optional<Payment> findByIdAndUserIdWithFetch(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.user JOIN FETCH p.account WHERE p.id = :id")
    Optional<Payment> findByIdWithFetch(@Param("id") Long id);
}
