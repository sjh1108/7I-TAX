package com.ssafy.tax7i.card.controller;

import com.ssafy.tax7i.card.dto.CardCreateRequest;
import com.ssafy.tax7i.card.dto.CardResponse;
import com.ssafy.tax7i.card.service.CardService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
            @Valid @RequestBody CardCreateRequest request) {
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
        CardResponse response = cardService.setDefault(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<SuccessResponse<Void>> deleteCard(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {
        cardService.deleteCard(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.ok());
    }
}
