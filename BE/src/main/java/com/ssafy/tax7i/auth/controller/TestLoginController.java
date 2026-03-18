package com.ssafy.tax7i.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.config.SsafyFinanceProperties;
import com.ssafy.tax7i.global.jwt.JwtTokenProvider;
import com.ssafy.tax7i.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 개발/테스트 전용 로그인 컨트롤러.
 *
 * POST /api/auth/test-login?email=xxx@yyy.com
 * - email: SSAFY 금융망에 등록된 이메일 (필수)
 * - 해당 이메일로 금융망 회원 조회 → userKey 발급 → DB 저장 → JWT 발급
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TestLoginController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SsafyFinanceProperties financeProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping("/test-login")
    public ResponseEntity<SuccessResponse<LoginResponse>> testLogin(
            @RequestParam(required = false) String email) {

        if (email == null || email.isBlank()) {
            email = "qkrrlxor627@naver.com"; // 기본 테스트 계정
        }

        final String loginEmail = email;
        final String ssafyUserId = "test-" + email.split("@")[0];

        User user = userRepository.findBySsafyUserId(ssafyUserId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .ssafyUserId(ssafyUserId)
                                .email(loginEmail)
                                .name(loginEmail.split("@")[0])
                                .build()));

        // 금융망 키가 없으면 이메일로 SSAFY 금융망 조회/등록
        if (user.getSsafyUserKey() == null || user.getSsafyUserKey().isBlank()) {
            String userKey = searchOrCreateMember(loginEmail);
            if (userKey != null) {
                user.registerFinanceKey(userKey);
                userRepository.save(user);
                log.info("금융망 키 등록 완료: userId={}, email={}, userKey={}", user.getId(), loginEmail, userKey);
            } else {
                log.error("금융망 키 발급 실패: email={}", loginEmail);
            }
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        return ResponseEntity.ok(SuccessResponse.of(new LoginResponse(accessToken, refreshToken)));
    }

    /**
     * SSAFY 금융망 회원 조회 → 실패 시 가입 → 재조회
     */
    private String searchOrCreateMember(String email) {
        String userKey = callMemberApi("/member/search", email);
        if (userKey != null) return userKey;

        userKey = callMemberApi("/member/", email);
        if (userKey != null) return userKey;

        return callMemberApi("/member/search", email);
    }

    @SuppressWarnings("unchecked")
    private String callMemberApi(String path, String userId) {
        String url = financeProperties.baseUrl().replace("/edu", "") + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("apiKey", financeProperties.apiKey(), "userId", userId);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return extractUserKey(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.info("SSAFY {} 응답 {}: {}", path, e.getStatusCode(), e.getResponseBodyAsString());
            try {
                Map<String, Object> errorBody = objectMapper.readValue(e.getResponseBodyAsString(), Map.class);
                return extractUserKey(errorBody);
            } catch (Exception parseEx) {
                log.warn("응답 body 파싱 실패: {}", parseEx.getMessage());
            }
        } catch (Exception e) {
            log.error("SSAFY API 호출 실패 {}: {}", path, e.getMessage());
        }
        return null;
    }

    private String extractUserKey(Map<String, ?> body) {
        if (body == null) return null;
        Object key = body.get("userKey");
        if (key instanceof String s && !s.isBlank()) return s;
        return null;
    }
}
