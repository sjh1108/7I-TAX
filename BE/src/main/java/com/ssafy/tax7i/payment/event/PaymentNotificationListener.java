package com.ssafy.tax7i.payment.event;

import com.ssafy.tax7i.bookentry.event.PaymentCapturedEvent;
import com.ssafy.tax7i.fcm.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationListener {

    private final FcmService fcmService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        try {
            String title = "결제 완료";
            String body = String.format("%s %,d원 결제가 완료되었습니다.",
                    event.merchantName(), event.amount());
            Map<String, String> data = Map.of(
                    "type", "payment",
                    "paymentId", String.valueOf(event.paymentId()),
                    "amount", String.valueOf(event.amount()),
                    "merchantName", event.merchantName());
            fcmService.sendNotification(event.userId(), title, body, data);
        } catch (Exception e) {
            log.warn("결제 완료 FCM 알림 실패: paymentId={}, error={}", event.paymentId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        try {
            String title = "결제 취소";
            String body = String.format("%s %,d원 결제가 취소되었습니다.",
                    event.merchantName(), event.amount());
            Map<String, String> data = Map.of(
                    "type", "payment_cancel",
                    "paymentId", String.valueOf(event.paymentId()),
                    "amount", String.valueOf(event.amount()),
                    "merchantName", event.merchantName());
            fcmService.sendNotification(event.userId(), title, body, data);
        } catch (Exception e) {
            log.warn("결제 취소 FCM 알림 실패: paymentId={}, error={}", event.paymentId(), e.getMessage());
        }
    }
}
