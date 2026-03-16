package com.ssafy.tax7i.payment.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyWithdrawResponse;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.service.AccountService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.repository.PaymentRepository;
import com.ssafy.tax7i.bookentry.event.PaymentCapturedEvent; // 기택 추가: 결제 확정 시 장부 자동 생성 이벤트
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher; // 기택 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;
    private final PaymentStatusUpdater paymentStatusUpdater;
    private final ApplicationEventPublisher eventPublisher; // 기택 추가: 장부 연동

    @Transactional
    public PaymentAuthorizeResponse authorize(Long userId, PaymentAuthorizeRequest request) {
        User user = findUser(userId);
        Account account = accountService.findAccountByUser(request.accountId(), userId);

        if (!account.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "비활성 계좌로는 결제할 수 없습니다.");
        }

        String userKey = getUserKey(user);
        SsafyBalanceResponse balanceResponse = ssafyFinanceClient.getBalance(userKey, account.getAccountNumber());
        long balance = Long.parseLong(balanceResponse.rec().accountBalance());
        long availableBalance = account.getAvailableBalance(balance);

        if (availableBalance < request.amount()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        account.reserve(request.amount());

        String authCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = Payment.builder()
                .user(user)
                .account(account)
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

        Account account = payment.getAccount();
        User user = payment.getUser();
        String userKey = getUserKey(user);

        SsafyWithdrawResponse withdrawResponse;
        try {
            withdrawResponse = ssafyFinanceClient.withdraw(
                    userKey,
                    account.getAccountNumber(),
                    payment.getAmount(),
                    payment.getMerchantName()
            );
        } catch (BusinessException e) {
            account.releaseReservation(payment.getAmount());
            paymentStatusUpdater.markDeclined(paymentId);
            throw e;
        }

        account.releaseReservation(payment.getAmount());
        payment.capture();
        long remainingBalance = Long.parseLong(withdrawResponse.rec().accountBalance());

        // 기택 추가: 결제 확정 후 장부 자동 생성 이벤트 발행 (AFTER_COMMIT으로 결제 영향 없음)
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

        long cancelAmount = request.cancelAmount() != null ? request.cancelAmount() : payment.getAmount();
        if (cancelAmount > payment.getAmount()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "취소 금액이 결제 금액을 초과할 수 없습니다.");
        }

        Account account = payment.getAccount();
        User user = payment.getUser();
        String userKey = getUserKey(user);

        ssafyFinanceClient.deposit(
                userKey,
                account.getAccountNumber(),
                cancelAmount,
                "결제취소: " + payment.getMerchantName()
        );

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
