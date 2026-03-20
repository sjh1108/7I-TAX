package com.ssafy.tax7i.card.controller;

import com.ssafy.tax7i.card.service.CardDepositService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardDepositController {

    private final CardDepositService cardDepositService;

    @PostMapping("/{cardId}/deposit")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> deposit(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @RequestBody Map<String, Long> request) {

        Map<String, Object> result = cardDepositService.deposit(userId, cardId, request.get("amount"));
        return ResponseEntity.ok(SuccessResponse.of(result));
    }

    @GetMapping("/{cardId}/balance")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getBalance(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {

        Map<String, Object> result = cardDepositService.getBalance(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.of(result));
    }
}
