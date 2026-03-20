package com.ssafy.tax7i.card.controller;

import com.ssafy.tax7i.card.dto.*;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import com.ssafy.tax7i.card.service.CardService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<SuccessResponse<CardResponse>> createCard(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateCardRequest request) {
        CardResponse response = cardService.createCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<CardResponse>>> getCards(
            @AuthenticationPrincipal Long userId) {
        List<CardResponse> response = cardService.getCards(userId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<SuccessResponse<CardResponse>> getCard(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {
        CardResponse response = cardService.getCard(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PatchMapping("/{cardId}/default")
    public ResponseEntity<SuccessResponse<CardResponse>> setDefault(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {
        CardResponse response = cardService.setDefaultCard(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<SuccessResponse<Void>> deleteCard(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {
        cardService.deleteCard(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    @PostMapping("/{cardId}/deposit")
    public ResponseEntity<SuccessResponse<CardDepositResponse>> deposit(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @Valid @RequestBody CardDepositRequest request) {
        CardDepositResponse response = cardService.deposit(userId, cardId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{cardId}/balance")
    public ResponseEntity<SuccessResponse<CardBalanceResponse>> getBalance(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {
        CardBalanceResponse response = cardService.getBalance(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{cardId}/transactions")
    public ResponseEntity<SuccessResponse<Page<CardTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @RequestParam(required = false) CardTransactionType type,
            Pageable pageable) {
        Page<CardTransactionResponse> response = cardService.getTransactions(userId, cardId, type, pageable);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
