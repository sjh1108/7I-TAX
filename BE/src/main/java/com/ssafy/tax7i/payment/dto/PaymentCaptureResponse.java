package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentCaptureResponse(
        Long paymentId,
        PaymentStatus status,
        CardDebit cardDebited,
        LocalDateTime capturedAt
) {
    public record CardDebit(
            Long cardId,
            Long debitedAmount,
            Long remainingBalance
    ) {
    }

    public static PaymentCaptureResponse of(Payment payment, Long remainingBalance) {
        return new PaymentCaptureResponse(
                payment.getId(),
                payment.getStatus(),
                new CardDebit(
                        payment.getCard().getId(),
                        payment.getAmount(),
                        remainingBalance
                ),
                payment.getCapturedAt()
        );
    }
}
