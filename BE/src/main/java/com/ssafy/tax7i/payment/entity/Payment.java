package com.ssafy.tax7i.payment.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String merchantName;

    private String merchantCategoryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String authorizationCode;

    private Long transactionId;

    private Long cancelledAmount;

    private String cancelReason;

    private LocalDateTime authorizedAt;

    private LocalDateTime capturedAt;

    private LocalDateTime cancelledAt;

    @Builder
    public Payment(User user, Account account, Long amount, String currency,
                   String merchantName, String merchantCategoryCode,
                   PaymentMethod paymentMethod, PaymentPurpose purpose,
                   String authorizationCode) {
        this.user = user;
        this.account = account;
        this.amount = amount;
        this.currency = currency;
        this.merchantName = merchantName;
        this.merchantCategoryCode = merchantCategoryCode;
        this.paymentMethod = paymentMethod;
        this.purpose = purpose;
        this.authorizationCode = authorizationCode;
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = LocalDateTime.now();
    }

    public void capture() {
        this.status = PaymentStatus.CAPTURED;
        this.capturedAt = LocalDateTime.now();
    }

    public void cancel(Long cancelAmount, String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAmount = cancelAmount;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = PaymentStatus.DECLINED;
    }

    public void linkTransaction(Long txId) {
        this.transactionId = txId;
    }
}
