package com.ssafy.tax7i.banking.repository;

import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    List<Account> findByUserIdAndAccountType(Long userId, AccountType accountType);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    Optional<Account> findByAccountNumber(String accountNumber);
}
