package com.ssafy.tax7i.card.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.card.dto.*;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.service.CardService;
import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CardService cardService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── POST /api/cards ─────────────

    @Test
    void createCard_201() throws Exception {
        CardResponse response = new CardResponse(1L, "사업용 카드", CardType.BUSINESS, "7890", false);
        given(cardService.createCard(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardName", "사업용 카드",
                                "cardType", "BUSINESS",
                                "accountTypeUniqueNo", "001-1-xxxxxxx"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.cardName").value("사업용 카드"))
                .andExpect(jsonPath("$.data.cardType").value("BUSINESS"))
                .andExpect(jsonPath("$.data.last4Digits").value("7890"));
    }

    @Test
    void createCard_필수값누락_400() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cardName", "테스트"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── GET /api/cards ─────────────

    @Test
    void getCards_200() throws Exception {
        List<CardResponse> responses = List.of(
                new CardResponse(1L, "카드1", CardType.BUSINESS, "1234", true),
                new CardResponse(2L, "카드2", CardType.PERSONAL, "5678", false));
        given(cardService.getCards(any())).willReturn(responses);

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ───────────── GET /api/cards/{id} ─────────────

    @Test
    void getCard_200() throws Exception {
        CardResponse response = new CardResponse(1L, "사업용 카드", CardType.BUSINESS, "7890", true);
        given(cardService.getCard(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardName").value("사업용 카드"))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void getCard_없는카드_404() throws Exception {
        given(cardService.getCard(any(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.CARD_NOT_FOUND));

        mockMvc.perform(get("/api/cards/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CARD_NOT_FOUND"));
    }

    // ───────────── PATCH /api/cards/{id}/default ─────────────

    @Test
    void setDefault_200() throws Exception {
        CardResponse response = new CardResponse(1L, "사업용 카드", CardType.BUSINESS, "7890", true);
        given(cardService.setDefaultCard(any(), eq(1L))).willReturn(response);

        mockMvc.perform(patch("/api/cards/1/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    // ───────────── DELETE /api/cards/{id} ─────────────

    @Test
    void deleteCard_200() throws Exception {
        doNothing().when(cardService).deleteCard(any(), eq(1L));

        mockMvc.perform(delete("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    // ───────────── POST /api/cards/{id}/deposit ─────────────

    @Test
    void deposit_200() throws Exception {
        CardDepositResponse response = new CardDepositResponse(1L, 50000L, 150000L);
        given(cardService.deposit(any(), eq(1L), any())).willReturn(response);

        mockMvc.perform(post("/api/cards/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 50000,
                                "sourceAccountNo", "1234567890"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.depositAmount").value(50000))
                .andExpect(jsonPath("$.data.balance").value(150000));
    }

    @Test
    void deposit_금액누락_400() throws Exception {
        mockMvc.perform(post("/api/cards/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── GET /api/cards/{id}/balance ─────────────

    @Test
    void getBalance_200() throws Exception {
        CardBalanceResponse response = new CardBalanceResponse(1L, "사업용 카드", 500000L);
        given(cardService.getBalance(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/cards/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(500000))
                .andExpect(jsonPath("$.data.cardName").value("사업용 카드"));
    }

    // ───────────── GET /api/cards/{id}/transactions ─────────────

    @Test
    void getTransactions_200() throws Exception {
        CardTransactionResponse txResponse = new CardTransactionResponse(
                1L, CardTransactionType.CHARGE, 50000L, 150000L, "카드 충전", LocalDateTime.now());
        Page<CardTransactionResponse> page = new PageImpl<>(List.of(txResponse));
        given(cardService.getTransactions(any(), eq(1L), any(), any())).willReturn(page);

        mockMvc.perform(get("/api/cards/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].transactionType").value("CHARGE"))
                .andExpect(jsonPath("$.data.content[0].amount").value(50000));
    }
}
