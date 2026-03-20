package com.ssafy.tax7i.payment.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyWithdrawResponse;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.repository.PaymentRepository;
import com.ssafy.tax7i.bookentry.event.PaymentCapturedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;
    private final PaymentStatusUpdater paymentStatusUpdater;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentAuthorizeResponse authorize(Long userId, PaymentAuthorizeRequest request) {
        User user = findUser(userId);
        Card card = cardRepository.findByIdAndUserId(request.cardId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));

        if (card.getSsafyAccountNo() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "카드에 연결된 계좌가 없습니다.");
        }

        String userKey = getUserKey(user);
        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, card.getSsafyAccountNo());
        long balance = Long.parseLong(balanceResponse.rec().accountBalance());

        if (balance < request.amount()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        String authCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = Payment.builder()
                .user(user)
                .card(card)
                .amount(request.amount())
                .currency(request.currencyOrDefault())
                .merchantName(request.merchantName())
                .merchantCategoryCode(request.merchantCategoryCode())
                .paymentMethod(request.paymentMethod())
                .purpose(request.purpose())
                .authorizationCode(authCode)
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
            paymentStatusUpdater.markDeclined(paymentId);
            throw e;
        }

        payment.capture();
        String balanceStr = withdrawResponse.rec().accountBalance();
        long remainingBalance = balanceStr != null ? Long.parseLong(balanceStr) : 0L;

        eventPublisher.publishEvent(new PaymentCapturedEvent(
                payment.getId(),
                user.getId(),
                payment.getAmount(),
                payment.getMerchantName(),
                payment.getMerchantCategoryCode(),
                payment.getPurpose().name(),
                payment.getCapturedAt()
        ));

        return PaymentCaptureResponse.of(payment, remainingBalance);
    }

    @Transactional
    public PaymentCancelResponse cancel(Long userId, Long paymentId, PaymentCancelRequest request) {
        Payment payment = paymentRepository.findByIdAndUserIdWithFetch(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new BusinessException(ErrorCode.PAYMENT_DECLINED, "확정된 결제만 취소할 수 있습니다.");
        }

        long alreadyCancelled = payment.getCancelledAmount() != null ? payment.getCancelledAmount() : 0L;
        long cancelAmount = request.cancelAmount() != null ? request.cancelAmount() : (payment.getAmount() - alreadyCancelled);
        if (cancelAmount + alreadyCancelled > payment.getAmount()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "취소 금액이 결제 금액을 초과할 수 없습니다.");
        }

        Card card = payment.getCard();
        User user = payment.getUser();
        String userKey = getUserKey(user);

        try {
            ssafyFinanceClient.deposit(
                    userKey,
                    card.getSsafyAccountNo(),
                    cancelAmount,
                    "결제취소: " + payment.getMerchantName()
            );
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "환불 입금 실패");
        }

        payment.cancel(cancelAmount, request.reason());
        return PaymentCancelResponse.from(payment);
    }

    public PaymentDetailResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserIdWithFetch(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentDetailResponse.from(payment);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String getUserKey(User user) {
        if (user.getSsafyUserKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 금융망 사용자 키가 등록되지 않았습니다.");
        }
        return user.getSsafyUserKey();
    }
}
