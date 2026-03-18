package com.ssafy.tax7i.auth.controller;

import com.ssafy.tax7i.auth.dto.*;
import com.ssafy.tax7i.auth.service.AuthService;
import com.ssafy.tax7i.auth.service.ConsentService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ConsentService consentService;

    @PostMapping("/verify-identity")
    public ResponseEntity<SuccessResponse<IdentityVerifyResponse>> verifyIdentity(
            @Valid @RequestBody IdentityVerifyRequest request) {
        IdentityVerifyResponse response = authService.verifyIdentity(request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/setup-pin")
    public ResponseEntity<SuccessResponse<LoginResponse>> setupPin(
            @Valid @RequestBody PinSetupRequest request,
            @RequestParam Long userId) {
        LoginResponse response = authService.setupPin(userId, request.pin());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(
            @Valid @RequestBody PinLoginRequest request) {
        LoginResponse response = authService.loginWithPin(request.phoneNumber(), request.pin());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/consents")
    public ResponseEntity<SuccessResponse<Void>> saveConsents(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody List<ConsentRequest> requests) {
        consentService.saveConsents(userId, requests);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    @PostMapping("/reissue")
    public ResponseEntity<SuccessResponse<LoginResponse>> reissue(
            @Valid @RequestBody TokenReissueRequest request) {
        LoginResponse loginResponse = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(SuccessResponse.of(loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(
            @RequestHeader("Authorization") String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        String accessToken = authorization.substring(7);
        authService.logout(accessToken);
        return ResponseEntity.ok(SuccessResponse.ok());
    }
}
