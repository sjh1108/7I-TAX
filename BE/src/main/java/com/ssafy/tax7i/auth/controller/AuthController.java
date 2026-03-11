package com.ssafy.tax7i.auth.controller;

import com.ssafy.tax7i.auth.dto.AuthorizationUrlResponse;
import com.ssafy.tax7i.auth.dto.CallbackRequest;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.dto.TokenReissueRequest;
import com.ssafy.tax7i.auth.service.AuthService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${ssafy.oauth.authorization-uri}")
    private String authorizationUri;

    @Value("${ssafy.oauth.client-id}")
    private String clientId;

    @Value("${ssafy.oauth.redirect-uri}")
    private String redirectUri;

    @GetMapping("/login")
    public ResponseEntity<SuccessResponse<AuthorizationUrlResponse>> login() {
        String url = authorizationUri
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";
        return ResponseEntity.ok(SuccessResponse.of(new AuthorizationUrlResponse(url)));
    }

    @PostMapping("/callback")
    public ResponseEntity<SuccessResponse<LoginResponse>> callback(@Valid @RequestBody CallbackRequest request) {
        LoginResponse loginResponse = authService.login(request.code());
        return ResponseEntity.ok(SuccessResponse.of(loginResponse));
    }

    @PostMapping("/reissue")
    public ResponseEntity<SuccessResponse<LoginResponse>> reissue(@Valid @RequestBody TokenReissueRequest request) {
        LoginResponse loginResponse = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(SuccessResponse.of(loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(@RequestHeader("Authorization") String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        String accessToken = authorization.substring(7);
        authService.logout(accessToken);
        return ResponseEntity.ok(SuccessResponse.ok());
    }
}
