package com.ssafy.tax7i.bookentry.event;

import com.ssafy.tax7i.bookentry.dto.BookEntryCreateRequest;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.service.BookEntryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 확정 이벤트를 수신하여 간편장부를 자동 생성하는 리스너.
 * - AFTER_COMMIT: 결제 트랜잭션 커밋 후 실행 → 장부 실패해도 결제 롤백 안 됨
 * - 사업자 카드 결제(BUSINESS)만 장부 생성, 개인 결제(PERSONAL)는 스킵
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookEntryEventListener {

    private final BookEntryService bookEntryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        // 기택 추가: 사업자 카드 결제만 장부 생성 (개인 결제는 장부 불필요)
        if (!"BUSINESS".equals(event.purpose())) {
            log.debug("개인 결제는 장부 생성 스킵: paymentId={}", event.paymentId());
            return;
        }

        try {
            BookEntryCreateRequest request = new BookEntryCreateRequest(
                    event.paymentId(),
                    event.capturedAt().toLocalDate(),
                    event.merchantName() + " 결제",
                    event.merchantName(),
                    EntryType.EXPENSE,
                    event.amount(),
                    false,
                    null,
                    null,
                    null
            );

            bookEntryService.create(event.userId(), request);

        } catch (Exception e) {
            // 장부 생성 실패해도 결제에 영향 없음 (AFTER_COMMIT)
            // 중복 생성(DUPLICATE_BOOK_ENTRY) 포함 — 이벤트 재발행 시 안전하게 무시
            log.error("장부 자동 생성 실패: paymentId={}, userId={}, amount={}, error={}",
                    event.paymentId(), event.userId(), event.amount(), e.getMessage(), e);
        }
    }
}
