package com.ssafy.tax7i.banking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.banking.dto.AccountResponse;
import com.ssafy.tax7i.banking.dto.BalanceResponse;
import com.ssafy.tax7i.banking.entity.AccountStatus;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.service.AccountService;
import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AccountService accountService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── POST /api/banking/accounts ─────────────

    @Test
    void createAccount_200_계좌생성() throws Exception {
        AccountResponse response = new AccountResponse(
                1L, AccountType.BUSINESS, "001", "******7890", "사업용", null, AccountStatus.ACTIVE, LocalDateTime.now());
        given(accountService.createAccount(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/banking/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("accountType", "BUSINESS", "alias", "사업용"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.accountType").value("BUSINESS"))
                .andExpect(jsonPath("$.data.alias").value("사업용"));
    }

    @Test
    void createAccount_accountType없음_400() throws Exception {
        mockMvc.perform(post("/api/banking/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("alias", "사업용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── GET /api/banking/accounts ─────────────

    @Test
    void getAccounts_200_목록반환() throws Exception {
        AccountResponse response = new AccountResponse(
                1L, AccountType.BUSINESS, "001", "******7890", "사업용", null, AccountStatus.ACTIVE, LocalDateTime.now());
        given(accountService.getAccounts(any(), eq(null))).willReturn(List.of(response));

        mockMvc.perform(get("/api/banking/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].accountType").value("BUSINESS"));
    }

    @Test
    void getAccounts_타입필터_200() throws Exception {
        given(accountService.getAccounts(any(), eq(AccountType.PERSONAL))).willReturn(List.of());

        mockMvc.perform(get("/api/banking/accounts")
                        .param("accountType", "PERSONAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ───────────── GET /api/banking/accounts/{id} ─────────────

    @Test
    void getAccount_200_상세조회() throws Exception {
        AccountResponse response = new AccountResponse(
                1L, AccountType.BUSINESS, "001", "******7890", "사업용", 500000L, AccountStatus.ACTIVE, LocalDateTime.now());
        given(accountService.getAccount(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/banking/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(500000));
    }

    @Test
    void getAccount_404_계좌없음() throws Exception {
        given(accountService.getAccount(any(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        mockMvc.perform(get("/api/banking/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));
    }

    // ───────────── GET /api/banking/accounts/{id}/balance ─────────────

    @Test
    void getBalance_200_잔액조회() throws Exception {
        BalanceResponse response = new BalanceResponse(1L, 1000000L, LocalDateTime.now());
        given(accountService.getBalance(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/banking/accounts/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(1000000))
                .andExpect(jsonPath("$.data.accountId").value(1));
    }
}
