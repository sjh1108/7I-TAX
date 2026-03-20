package com.ssafy.tax7i.card.repository;

import com.ssafy.tax7i.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByIdAndUser_Id(Long id, Long userId);

    List<Card> findByUser_Id(Long userId);

    Optional<Card> findByUser_IdAndIsDefaultTrue(Long userId);
}
