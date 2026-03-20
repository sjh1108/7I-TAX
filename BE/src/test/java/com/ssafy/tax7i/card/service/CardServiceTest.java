package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.card.dto.*;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.card.repository.CardTransactionRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private CardTransactionRepository cardTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SsafyFinanceClient ssafyFinanceClient;

    @InjectMocks
    private CardService cardService;

    // ───────────── createCard ─────────────

    @Test
    void createCard_성공() {
        User user = createUser(1L, "user-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(ssafyFinanceClient.createAccount("user-key", "001-1-xxx"))
                .willReturn(createAccountResponse("9876543210"));
        given(cardRepository.save(any(Card.class))).willAnswer(invocation -> {
            Card c = invocation.getArgument(0);
            setField(c, "id", 1L);
            return c;
        });

        CreateCardRequest request = new CreateCardRequest("사업용 카드", CardType.BUSINESS, "001-1-xxx");
        CardResponse response = cardService.createCard(1L, request);

        assertThat(response.cardName()).isEqualTo("사업용 카드");
        assertThat(response.cardType()).isEqualTo(CardType.BUSINESS);
        assertThat(response.last4Digits()).isEqualTo("3210");
    }

    // ───────────── setDefaultCard ─────────────

    @Test
    void setDefaultCard_성공() {
        User user = createUser(1L, "user-key");
        Card oldDefault = createCard(2L, user, "1111111111");
        setField(oldDefault, "isDefault", true);
        Card newDefault = createCard(1L, user, "2222222222");

        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(newDefault));
        given(cardRepository.findByUser_IdAndIsDefaultTrue(1L)).willReturn(Optional.of(oldDefault));

        CardResponse response = cardService.setDefaultCard(1L, 1L);

        assertThat(response.isDefault()).isTrue();
        assertThat(oldDefault.isDefault()).isFalse();
    }

    // ───────────── deposit ─────────────

    @Test
    void deposit_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyFinanceClient.withdraw("user-key", "9999999999", 50000L, "카드 충전"))
                .willReturn(withdrawResponse("450000"));
        given(ssafyFinanceClient.deposit("user-key", "1234567890", 50000L, "카드 충전"))
                .willReturn(depositResponse("150000"));

        CardDepositRequest request = new CardDepositRequest(50000L, "9999999999");
        CardDepositResponse response = cardService.deposit(1L, 1L, request);

        assertThat(response.depositAmount()).isEqualTo(50000L);
        assertThat(response.balance()).isEqualTo(150000L);
        verify(cardTransactionRepository).save(any());
    }

    // ───────────── getBalance ─────────────

    @Test
    void getBalance_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyFinanceClient.getBalance("user-key", "1234567890"))
                .willReturn(balanceResponse("500000"));

        CardBalanceResponse response = cardService.getBalance(1L, 1L);

        assertThat(response.balance()).isEqualTo(500000L);
        assertThat(response.cardName()).isEqualTo("테스트 카드");
    }

    // ───────────── getCardWithOwnership ─────────────

    @Test
    void getCard_소유권불일치_예외() {
        given(cardRepository.findByIdAndUser_Id(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_NOT_FOUND));
    }

    // ───────────── deleteCard ─────────────

    @Test
    void deleteCard_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(card));

        cardService.deleteCard(1L, 1L);

        verify(cardRepository).delete(card);
    }

    // ───────────── helpers ─────────────

    private User createUser(Long id, String userKey) {
        User user = User.builder()
                .ci("ci-hash")
                .di("di-hash")
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("M")
                .phoneNumber("01012345678")
                .phoneLast4("5678")
                .build();
        setField(user, "id", id);
        user.registerFinanceKey(userKey);
        return user;
    }

    private Card createCard(Long id, User user, String accountNo) {
        Card card = Card.builder()
                .user(user)
                .cardName("테스트 카드")
                .cardType(CardType.BUSINESS)
                .last4Digits("7890")
                .ssafyAccountNo(accountNo)
                .build();
        setField(card, "id", id);
        return card;
    }

    private SsafyCreateAccountResponse createAccountResponse(String accountNo) {
        return new SsafyCreateAccountResponse(
                successHeader(),
                new SsafyCreateAccountResponse.AccountRec("001", accountNo)
        );
    }

    private SsafyBalanceResponse balanceResponse(String balance) {
        return new SsafyBalanceResponse(
                successHeader(),
                new SsafyBalanceResponse.BalanceRec("001", "1234567890", balance, "20260320")
        );
    }

    private SsafyWithdrawResponse withdrawResponse(String remainingBalance) {
        return new SsafyWithdrawResponse(
                successHeader(),
                new SsafyWithdrawResponse.WithdrawRec("TX001", "9999999999", "20260320", "50000", remainingBalance)
        );
    }

    private SsafyDepositResponse depositResponse(String balance) {
        return new SsafyDepositResponse(
                successHeader(),
                new SsafyDepositResponse.DepositRec("TX002", "1234567890", "20260320", "50000", balance)
        );
    }

    private SsafyResponseHeader successHeader() {
        return new SsafyResponseHeader("H0000", "정상처리 되었습니다.", "test", "20260320", "120000", "00100", "key", "test", "uniqueNo");
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
