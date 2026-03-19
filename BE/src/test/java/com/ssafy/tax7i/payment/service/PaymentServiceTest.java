package com.ssafy.tax7i.payment.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentMethod;
import com.ssafy.tax7i.payment.entity.PaymentPurpose;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private SsafyFinanceClient ssafyFinanceClient;

    @InjectMocks
    private PaymentService paymentService;

    // ───────────── authorize ─────────────

    @Test
    void authorize_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyFinanceClient.getBalance("user-key", "1234567890"))
                .willReturn(balanceResponse("500000"));
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            setField(p, "id", 1L);
            return p;
        });

        PaymentAuthorizeRequest request = new PaymentAuthorizeRequest(
                1L, 10000L, "KRW", "스타벅스", "5812", PaymentMethod.OFFLINE, PaymentPurpose.BUSINESS);

        PaymentAuthorizeResponse response = paymentService.authorize(1L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(response.amount()).isEqualTo(10000L);
    }

    @Test
    void authorize_잔액부족_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyFinanceClient.getBalance("user-key", "1234567890"))
                .willReturn(balanceResponse("5000"));

        PaymentAuthorizeRequest request = new PaymentAuthorizeRequest(
                1L, 10000L, null, "스타벅스", null, PaymentMethod.OFFLINE, PaymentPurpose.PERSONAL);

        assertThatThrownBy(() -> paymentService.authorize(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    void authorize_카드없음_예외() {
        User user = createUser(1L, "user-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_Id(99L, 1L)).willReturn(Optional.empty());

        PaymentAuthorizeRequest request = new PaymentAuthorizeRequest(
                99L, 10000L, null, "테스트", null, PaymentMethod.ONLINE, PaymentPurpose.PERSONAL);

        assertThatThrownBy(() -> paymentService.authorize(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_NOT_FOUND));
    }

    // ───────────── capture ─────────────

    @Test
    void capture_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.AUTHORIZED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));
        given(ssafyFinanceClient.withdraw("user-key", "1234567890", 10000L, "스타벅스"))
                .willReturn(withdrawResponse("490000"));

        PaymentCaptureResponse response = paymentService.capture(1L, 1L);

        assertThat(response.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(response.cardDebited().remainingBalance()).isEqualTo(490000L);
    }

    @Test
    void capture_승인상태아님_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.CAPTURED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.capture(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_DECLINED));
    }

    @Test
    void capture_결제없음_예외() {
        given(paymentRepository.findByIdAndUserIdWithFetch(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.capture(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // ───────────── cancel ─────────────

    @Test
    void cancel_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.CAPTURED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));
        given(ssafyFinanceClient.deposit("user-key", "1234567890", 10000L, "결제취소: 스타벅스"))
                .willReturn(depositResponse());

        PaymentCancelRequest request = new PaymentCancelRequest(10000L, "고객 요청");
        PaymentCancelResponse response = paymentService.cancel(1L, 1L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(response.cancelledAmount()).isEqualTo(10000L);
    }

    @Test
    void cancel_확정되지않은결제_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.AUTHORIZED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));

        PaymentCancelRequest request = new PaymentCancelRequest(10000L, "취소 사유");

        assertThatThrownBy(() -> paymentService.cancel(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_DECLINED));
    }

    @Test
    void cancel_금액미지정_전액취소() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.CAPTURED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));
        given(ssafyFinanceClient.deposit("user-key", "1234567890", 10000L, "결제취소: 스타벅스"))
                .willReturn(depositResponse());

        PaymentCancelRequest request = new PaymentCancelRequest(null, "전액 취소");
        PaymentCancelResponse response = paymentService.cancel(1L, 1L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(response.cancelledAmount()).isEqualTo(10000L);
    }

    // ───────────── getPayment ─────────────

    @Test
    void getPayment_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 15000L, PaymentStatus.CAPTURED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));

        PaymentDetailResponse response = paymentService.getPayment(1L, 1L);

        assertThat(response.amount()).isEqualTo(15000L);
        assertThat(response.status()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void getPayment_결제없음_예외() {
        given(paymentRepository.findByIdAndUserIdWithFetch(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // ───────────── processQrPayment ─────────────

    @Test
    void processQrPayment_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyFinanceClient.getBalance("user-key", "1234567890"))
                .willReturn(balanceResponse("500000"));
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            setField(p, "id", 1L);
            return p;
        });
        given(ssafyFinanceClient.withdraw("user-key", "1234567890", 10000L, "스타벅스"))
                .willReturn(withdrawResponse("490000"));

        QrPaymentRequest request = new QrPaymentRequest(1L, 10000L, "스타벅스", "5812", PaymentPurpose.BUSINESS);

        QrPaymentResponse response = paymentService.processQrPayment(1L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(response.remainingBalance()).isEqualTo(490000L);
    }

    @Test
    void processQrPayment_잔액부족_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyFinanceClient.getBalance("user-key", "1234567890"))
                .willReturn(balanceResponse("5000"));

        QrPaymentRequest request = new QrPaymentRequest(1L, 10000L, "스타벅스", "5812", PaymentPurpose.BUSINESS);

        assertThatThrownBy(() -> paymentService.processQrPayment(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    void processQrPayment_출금실패_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_Id(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyFinanceClient.getBalance("user-key", "1234567890"))
                .willReturn(balanceResponse("500000"));
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            setField(p, "id", 1L);
            return p;
        });
        given(ssafyFinanceClient.withdraw("user-key", "1234567890", 10000L, "스타벅스"))
                .willThrow(new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE));

        QrPaymentRequest request = new QrPaymentRequest(1L, 10000L, "스타벅스", "5812", PaymentPurpose.BUSINESS);

        assertThatThrownBy(() -> paymentService.processQrPayment(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BANK_SERVICE_UNAVAILABLE));
    }

    // ───────────── getPayments ─────────────

    @Test
    void getPayments_날짜필터없음() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.CAPTURED);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        given(paymentRepository.findByUserIdWithFetch(eq(1L), any())).willReturn(page);

        Page<PaymentDetailResponse> result = paymentService.getPayments(1L, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).amount()).isEqualTo(10000L);
    }

    @Test
    void getPayments_날짜필터() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 20000L, PaymentStatus.CAPTURED);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        given(paymentRepository.findByUserIdAndCapturedAtBetweenWithFetch(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .willReturn(page);

        Page<PaymentDetailResponse> result = paymentService.getPayments(1L, start, end, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).amount()).isEqualTo(20000L);
    }

    @Test
    void getPayments_상태필터() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");
        Payment payment = createPayment(1L, user, card, 15000L, PaymentStatus.CANCELLED);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        given(paymentRepository.findByUserIdAndStatus(eq(1L), eq(PaymentStatus.CANCELLED), any()))
                .willReturn(page);

        Page<PaymentDetailResponse> result = paymentService.getPayments(
                1L, null, null, PaymentStatus.CANCELLED, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(PaymentStatus.CANCELLED);
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

    private Payment createPayment(Long id, User user, Card card, Long amount, PaymentStatus status) {
        Payment payment = Payment.builder()
                .user(user)
                .card(card)
                .amount(amount)
                .currency("KRW")
                .merchantName("스타벅스")
                .merchantCategoryCode("5812")
                .paymentMethod(PaymentMethod.OFFLINE)
                .purpose(PaymentPurpose.BUSINESS)
                .authorizationCode("AUTH1234")
                .build();
        setField(payment, "id", id);
        setField(payment, "status", status);
        return payment;
    }

    private SsafyBalanceResponse balanceResponse(String balance) {
        return new SsafyBalanceResponse(
                successHeader(),
                new SsafyBalanceResponse.BalanceRec("001", "1234567890", balance, "20260311")
        );
    }

    private SsafyWithdrawResponse withdrawResponse(String remainingBalance) {
        return new SsafyWithdrawResponse(
                successHeader(),
                new SsafyWithdrawResponse.WithdrawRec("TX001", "1234567890", "20260311", "10000", remainingBalance)
        );
    }

    private SsafyDepositResponse depositResponse() {
        return new SsafyDepositResponse(
                successHeader(),
                new SsafyDepositResponse.DepositRec("TX002", "1234567890", "20260311", "10000", "510000")
        );
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
