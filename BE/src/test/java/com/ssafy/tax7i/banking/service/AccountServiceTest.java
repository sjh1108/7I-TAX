package com.ssafy.tax7i.banking.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.banking.dto.AccountResponse;
import com.ssafy.tax7i.banking.dto.BalanceResponse;
import com.ssafy.tax7i.banking.dto.CreateAccountRequest;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private SsafyFinanceClient ssafyFinanceClient;

    @InjectMocks
    private AccountService accountService;

    // ───────────── createAccount ─────────────

    @Test
    void createAccount_성공() {
        User user = createUserWithIdAndKey(1L, "user-key-123");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        SsafyCreateAccountResponse.AccountRec rec = new SsafyCreateAccountResponse.AccountRec("001", "1234567890");
        SsafyResponseHeader header = successHeader();
        given(ssafyFinanceClient.createAccount("user-key-123", "001-1-c7a09e90da4a43"))
                .willReturn(new SsafyCreateAccountResponse(header, rec));
        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            setField(account, "id", 1L);
            return account;
        });

        CreateAccountRequest request = new CreateAccountRequest(AccountType.BUSINESS, "사업용 계좌");
        AccountResponse response = accountService.createAccount(1L, request);

        assertThat(response.accountType()).isEqualTo(AccountType.BUSINESS);
        assertThat(response.alias()).isEqualTo("사업용 계좌");
        then(accountRepository).should().save(any(Account.class));
    }

    @Test
    void createAccount_사용자없음_예외() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        CreateAccountRequest request = new CreateAccountRequest(AccountType.PERSONAL, null);

        assertThatThrownBy(() -> accountService.createAccount(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void createAccount_금융키없음_예외() {
        User user = createUserWithIdAndKey(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        CreateAccountRequest request = new CreateAccountRequest(AccountType.PERSONAL, null);

        assertThatThrownBy(() -> accountService.createAccount(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_ARGUMENT));
    }

    // ───────────── getAccounts ─────────────

    @Test
    void getAccounts_전체조회() {
        User user = createUserWithIdAndKey(1L, "key");
        Account account = Account.builder()
                .user(user).accountType(AccountType.BUSINESS)
                .bankCode("001").accountNumber("1234567890").alias("테스트")
                .build();
        setField(account, "id", 1L);
        given(accountRepository.findByUserId(1L)).willReturn(List.of(account));

        List<AccountResponse> responses = accountService.getAccounts(1L, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).accountType()).isEqualTo(AccountType.BUSINESS);
    }

    @Test
    void getAccounts_타입필터() {
        given(accountRepository.findByUserIdAndAccountType(1L, AccountType.BUSINESS)).willReturn(List.of());

        List<AccountResponse> responses = accountService.getAccounts(1L, AccountType.BUSINESS);

        assertThat(responses).isEmpty();
        then(accountRepository).should().findByUserIdAndAccountType(1L, AccountType.BUSINESS);
    }

    // ───────────── getAccount ─────────────

    @Test
    void getAccount_성공_잔액포함() {
        User user = createUserWithIdAndKey(1L, "user-key-123");
        Account account = Account.builder()
                .user(user).accountType(AccountType.BUSINESS)
                .bankCode("001").accountNumber("1234567890").alias("테스트")
                .build();
        setField(account, "id", 1L);
        given(accountRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(account));

        SsafyBalanceResponse.BalanceRec balanceRec = new SsafyBalanceResponse.BalanceRec("001", "1234567890", "500000", "20260311");
        given(ssafyFinanceClient.getBalance("user-key-123", "1234567890"))
                .willReturn(new SsafyBalanceResponse(successHeader(), balanceRec));

        AccountResponse response = accountService.getAccount(1L, 1L);

        assertThat(response.balance()).isEqualTo(500000L);
    }

    @Test
    void getAccount_계좌없음_예외() {
        given(accountRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    // ───────────── getBalance ─────────────

    @Test
    void getBalance_성공() {
        User user = createUserWithIdAndKey(1L, "user-key-123");
        Account account = Account.builder()
                .user(user).accountType(AccountType.BUSINESS)
                .bankCode("001").accountNumber("1234567890").alias("테스트")
                .build();
        setField(account, "id", 1L);
        given(accountRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(account));

        SsafyBalanceResponse.BalanceRec balanceRec = new SsafyBalanceResponse.BalanceRec("001", "1234567890", "1000000", "20260311");
        given(ssafyFinanceClient.getBalance("user-key-123", "1234567890"))
                .willReturn(new SsafyBalanceResponse(successHeader(), balanceRec));

        BalanceResponse response = accountService.getBalance(1L, 1L);

        assertThat(response.balance()).isEqualTo(1000000L);
        assertThat(response.accountId()).isEqualTo(1L);
    }

    // ───────────── helpers ─────────────

    private User createUserWithIdAndKey(Long id, String userKey) {
        User user = User.builder()
                .ssafyUserId("ssafy-user-001")
                .email("test@ssafy.com")
                .name("홍길동")
                .build();
        setField(user, "id", id);
        if (userKey != null) {
            user.registerFinanceKey(userKey);
        }
        return user;
    }

    private SsafyResponseHeader successHeader() {
        return new SsafyResponseHeader("H0000", "정상처리 되었습니다.", "test", "20260311", "120000", "00100", "key", "test", "uniqueNo");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
