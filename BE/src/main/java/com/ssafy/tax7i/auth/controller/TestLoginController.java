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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 개발/테스트 전용 로그인 컨트롤러.
 *
 * POST /api/auth/test-login?email=xxx@yyy.com
 * - email로 CI/DI를 자동 생성하여 테스트 유저를 만들고 JWT를 발급합니다.
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
    private final RedisTemplate<String, String> redisTemplate;

    @PostMapping("/test-login")
    public ResponseEntity<SuccessResponse<LoginResponse>> testLogin(
            @RequestParam(required = false) String email) {

        if (email == null || email.isBlank()) {
            email = "qkrrlxor627@naver.com";
        }

        final String loginEmail = email;

        // email 기반으로 mock CI/DI 생성
        String ci = sha256("ci:" + loginEmail);
        String di = sha256("di:tax7i:" + loginEmail);

        User user = userRepository.findByCi(ci)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .ci(ci)
                                .di(di)
                                .name(loginEmail.split("@")[0])
                                .birthDate(LocalDate.of(1990, 1, 1))
                                .gender("M")
                                .phoneNumber("01012345678")
                                .phoneLast4("5678")
                                .build()));

        // PIN 자동 설정 (테스트용)
        if (user.getPinHash() == null) {
            user.setupPin("$2a$10$testPinHashForDevelopment");
        }

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
        redisTemplate.opsForValue().set(
                "RT:" + user.getId(), refreshToken,
                jwtTokenProvider.getRefreshExpiration(), TimeUnit.MILLISECONDS);
        return ResponseEntity.ok(SuccessResponse.of(new LoginResponse(accessToken, refreshToken)));
    }

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

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 해싱 실패", e);
        }
    }
}
