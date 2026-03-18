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

    @Column(nullable = false, length = 50)
    private String cardName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardType cardType;

    @Column(length = 4)
    private String last4Digits;

    @Column(nullable = false)
    private Boolean isDefault;

    private String ssafyAccountNo;

    @Builder
    public Card(Long userId, String cardName, CardType cardType, String last4Digits, String ssafyAccountNo) {
        this.userId = userId;
        this.cardName = cardName;
        this.cardType = cardType;
        this.last4Digits = last4Digits;
        this.ssafyAccountNo = ssafyAccountNo;
        this.isDefault = false;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setSsafyAccountNo(String ssafyAccountNo) {
        this.ssafyAccountNo = ssafyAccountNo;
    }
}
