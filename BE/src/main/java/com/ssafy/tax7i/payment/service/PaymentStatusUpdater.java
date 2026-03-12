package com.ssafy.tax7i.payment.service;

import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentStatusUpdater {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDeclined(Long paymentId) {
        paymentRepository.findById(paymentId).ifPresent(Payment::decline);
    }
}
