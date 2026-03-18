package com.ssafy.tax7i.card.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long accountId;

    @Column(nullable = false, length = 50)
    private String cardName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardType cardType;

    @Column(length = 4)
    private String last4Digits;

    @Column(nullable = false)
    private Boolean isDefault;

    @Builder
    public Card(Long userId, Long accountId, String cardName, CardType cardType, String last4Digits) {
        this.userId = userId;
        this.accountId = accountId;
        this.cardName = cardName;
        this.cardType = cardType;
        this.last4Digits = last4Digits;
        this.isDefault = false;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}
