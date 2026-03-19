package com.ssafy.tax7i.card.repository;

import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserId(Long userId);

    List<Card> findByUserIdAndCardType(Long userId, CardType cardType);

    Optional<Card> findByIdAndUserId(Long id, Long userId);

    Optional<Card> findByUserIdAndIsDefaultTrue(Long userId);
}
