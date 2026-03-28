package com.ssafy.tax7i.payment.event;

import java.time.LocalDateTime;

public record PaymentCancelledEvent(
        Long paymentId,
        Long userId,
        Long amount,
        String merchantName
) {}
