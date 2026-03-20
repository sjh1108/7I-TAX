package com.ssafy.tax7i.payment.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyDepositResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyWithdrawResponse;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardTransaction;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.card.repository.CardTransactionRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentMethod;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardRepository cardRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;

    private record PaymentContext(User user, Card card, String userKey, long balance, String authCode) {}

    private PaymentContext preparePayment(Long userId, Long cardId, long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Card card = cardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        String userKey = getUserKey(user);
        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, card.getSsafyAccountNo());
        long balance = Long.parseLong(balanceResponse.rec().accountBalance());

        if (balance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        String authCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentContext(user, card, userKey, balance, authCode);
    }

    @Transactional
    public PaymentAuthorizeResponse authorize(Long userId, PaymentAuthorizeRequest request) {
        PaymentContext ctx = preparePayment(userId, request.cardId(), request.amount());

        Payment payment = Payment.builder()
                .user(ctx.user())
                .card(ctx.card())
                .amount(request.amount())
                .currency(request.currencyOrDefault())
                .merchantName(request.merchantName())
                .merchantCategoryCode(request.merchantCategoryCode())
                .paymentMethod(request.paymentMethod())
                .purpose(request.purpose())
                .authorizationCode(ctx.authCode())
                .build();

        paymentRepository.save(payment);
        return PaymentAuthorizeResponse.from(payment);
    }

    @Transactional
    public PaymentCaptureResponse capture(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserIdWithFetch(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new BusinessException(ErrorCode.PAYMENT_DECLINED, "승인 상태의 결제만 확정할 수 있습니다.");
        }

        Card card = payment.getCard();
        User user = payment.getUser();
        String userKey = getUserKey(user);

        SsafyWithdrawResponse withdrawResponse;
        try {
            withdrawResponse = ssafyFinanceClient.withdraw(
                    userKey,
                    card.getSsafyAccountNo(),
                    payment.getAmount(),
                    payment.getMerchantName()
            );
        } catch (BusinessException e) {
            payment.decline();
            throw e;
        }

        payment.capture();
        long remainingBalance = Long.parseLong(withdrawResponse.rec().accountBalance());

        cardTransactionRepository.save(CardTransaction.builder()
                .card(card)
                .transactionType(CardTransactionType.PAYMENT)
                .amount(payment.getAmount())
                .balanceAfter(remainingBalance)
                .description("결제: " + payment.getMerchantName())
                .build());

        return PaymentCaptureResponse.of(payment, remainingBalance);
    }

    @Transactional
    public PaymentCancelResponse cancel(Long userId, Long paymentId, PaymentCancelRequest request) {
        Payment payment = paymentRepository.findByIdAndUserIdWithFetch(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new BusinessException(ErrorCode.PAYMENT_DECLINED, "확정된 결제만 취소할 수 있습니다.");
        }

        long cancelAmount = request.cancelAmount() != null ? request.cancelAmount() : payment.getAmount();
        long alreadyCancelled = payment.getCancelledAmount() != null ? payment.getCancelledAmount() : 0L;
        if (cancelAmount > payment.getAmount() - alreadyCancelled) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "취소 금액이 남은 결제 금액을 초과할 수 없습니다.");
        }

        Card card = payment.getCard();
        User user = payment.getUser();
        String userKey = getUserKey(user);

        SsafyDepositResponse depositResponse = ssafyFinanceClient.deposit(
                userKey,
                card.getSsafyAccountNo(),
                cancelAmount,
                "결제취소: " + payment.getMerchantName()
        );

        payment.cancel(cancelAmount, request.reason());

        long balanceAfterRefund = Long.parseLong(depositResponse.rec().accountBalance());
        cardTransactionRepository.save(CardTransaction.builder()
                .card(card)
                .transactionType(CardTransactionType.REFUND)
                .amount(cancelAmount)
                .balanceAfter(balanceAfterRefund)
                .description("환불: " + payment.getMerchantName())
                .build());

        return PaymentCancelResponse.from(payment);
    }

    public PaymentDetailResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserIdWithFetch(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentDetailResponse.from(payment);
    }

    public Page<PaymentDetailResponse> getPayments(Long userId, LocalDate startDate, LocalDate endDate,
                                                    PaymentStatus status, Pageable pageable) {
        boolean hasDateRange = startDate != null && endDate != null;

        if (hasDateRange) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            if (status != null) {
                return paymentRepository.findByUserIdAndCapturedAtBetweenAndStatusWithFetch(userId, start, end, status, pageable)
                        .map(PaymentDetailResponse::from);
            }
            return paymentRepository.findByUserIdAndCapturedAtBetweenWithFetch(userId, start, end, pageable)
                    .map(PaymentDetailResponse::from);
        }
        if (status != null) {
            return paymentRepository.findByUserIdAndStatus(userId, status, pageable)
                    .map(PaymentDetailResponse::from);
        }
        return paymentRepository.findByUserIdWithFetch(userId, pageable)
                .map(PaymentDetailResponse::from);
    }

    @Transactional
    public QrPaymentResponse processQrPayment(Long userId, QrPaymentRequest request) {
        PaymentContext ctx = preparePayment(userId, request.cardId(), request.amount());

        Payment payment = Payment.builder()
                .user(ctx.user())
                .card(ctx.card())
                .amount(request.amount())
                .currency("KRW")
                .merchantName(request.merchantName())
                .merchantCategoryCode(request.merchantCategoryCode())
                .paymentMethod(PaymentMethod.OFFLINE)
                .purpose(request.purpose())
                .authorizationCode(ctx.authCode())
                .build();

        paymentRepository.save(payment);

        SsafyWithdrawResponse withdrawResponse;
        try {
            withdrawResponse = ssafyFinanceClient.withdraw(
                    ctx.userKey(), ctx.card().getSsafyAccountNo(), request.amount(), request.merchantName());
        } catch (BusinessException e) {
            payment.decline();
            throw e;
        }

        payment.capture();
        long remainingBalance = Long.parseLong(withdrawResponse.rec().accountBalance());

        return QrPaymentResponse.of(payment, remainingBalance);
    }

    private String getUserKey(User user) {
        if (user.getSsafyUserKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 금융망 사용자 키가 등록되지 않았습니다.");
        }
        return user.getSsafyUserKey();
    }
}
