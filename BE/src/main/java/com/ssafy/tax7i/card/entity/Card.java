package com.ssafy.tax7i.card.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String cardName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Column(nullable = false, length = 4)
    private String last4Digits;

    @Column(nullable = false)
    private boolean isDefault;

    @Column(nullable = false)
    private String ssafyAccountNo;

    @Builder
    public Card(User user, String cardName, CardType cardType, String last4Digits, String ssafyAccountNo) {
        this.user = user;
        this.cardName = cardName;
        this.cardType = cardType;
        this.last4Digits = last4Digits;
        this.isDefault = false;
        this.ssafyAccountNo = ssafyAccountNo;
    }

    public void markDefault() {
        this.isDefault = true;
    }

    public void unmarkDefault() {
        this.isDefault = false;
    }
}
