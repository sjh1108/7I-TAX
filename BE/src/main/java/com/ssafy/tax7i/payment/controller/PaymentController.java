package com.ssafy.tax7i.payment.controller;

import com.ssafy.tax7i.global.response.SuccessResponse;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/authorize")
    public ResponseEntity<SuccessResponse<PaymentAuthorizeResponse>> authorize(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PaymentAuthorizeRequest request) {
        PaymentAuthorizeResponse response = paymentService.authorize(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<SuccessResponse<PaymentCaptureResponse>> capture(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId) {
        PaymentCaptureResponse response = paymentService.capture(userId, paymentId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<SuccessResponse<PaymentCancelResponse>> cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentCancelRequest request) {
        PaymentCancelResponse response = paymentService.cancel(userId, paymentId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<SuccessResponse<PaymentDetailResponse>> getPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId) {
        PaymentDetailResponse response = paymentService.getPayment(userId, paymentId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<Page<PaymentDetailResponse>>> getPayments(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) PaymentStatus status,
            Pageable pageable) {
        Page<PaymentDetailResponse> response = paymentService.getPayments(userId, startDate, endDate, status, pageable);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/qr")
    public ResponseEntity<SuccessResponse<QrPaymentResponse>> processQrPayment(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QrPaymentRequest request) {
        QrPaymentResponse response = paymentService.processQrPayment(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
