package com.ssafy.tax7i.banking.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false)
    private String bankCode;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false)
    private Long reservedAmount;

    @Builder
    public Account(User user, AccountType accountType, String bankCode, String accountNumber, String alias) {
        this.user = user;
        this.accountType = accountType;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.alias = alias;
        this.status = AccountStatus.ACTIVE;
        this.reservedAmount = 0L;
    }

    public void reserve(Long amount) {
        this.reservedAmount += amount;
    }

    public void releaseReservation(Long amount) {
        this.reservedAmount = Math.max(0L, this.reservedAmount - amount);
    }

    public long getAvailableBalance(long actualBalance) {
        return actualBalance - this.reservedAmount;
    }

    public void close() {
        this.status = AccountStatus.CLOSED;
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }
}
