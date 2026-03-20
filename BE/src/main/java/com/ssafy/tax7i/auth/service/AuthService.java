package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.domain.UserStatus;
import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import com.ssafy.tax7i.auth.dto.IdentityVerifyResponse;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.auth.service.NiceIdentityMockService.VerificationResult;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import io.hypersistence.tsid.TSID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BL:";
    private static final String VERIFY_SESSION_PREFIX = "verify-session:";
    private static final String PIN_FAIL_PREFIX = "pin-fail:";
    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final long PIN_FAIL_TTL_MINUTES = 5;

    private final NiceIdentityMockService niceIdentityMockService;
    private final PinService pinService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public IdentityVerifyResponse verifyIdentity(IdentityVerifyRequest request) {
        VerificationResult result = niceIdentityMockService.verify(request);

        Optional<User> existing = userRepository.findByCi(result.ci());
        boolean isNewUser = existing.isEmpty();

        User user = existing.orElseGet(() -> userRepository.save(
                User.builder()
                        .ci(result.ci())
                        .di(result.di())
                        .name(result.name())
                        .birthDate(LocalDate.parse(result.birthDate()))
                        .gender(result.gender())
                        .phoneNumber(result.phoneNumber())
                        .phoneLast4(result.phoneLast4())
                        .build()
        ));

        if (!isNewUser) {
            checkUserStatus(user);
        }

        boolean requiresPinSetup = user.getPinHash() == null;

        String verifyToken = TSID.fast().toString();
        redisTemplate.opsForValue().set(
                VERIFY_SESSION_PREFIX + verifyToken,
                String.valueOf(user.getId()),
                5, TimeUnit.MINUTES
        );

        return new IdentityVerifyResponse(user.getId(), isNewUser, requiresPinSetup, verifyToken);
    }

    @Transactional
    public LoginResponse setupPin(String verifyToken, String pin) {
        String userIdStr = redisTemplate.opsForValue().get(VERIFY_SESSION_PREFIX + verifyToken);
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "본인인증 세션이 만료되었습니다.");
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        checkUserStatus(user);

        user.setupPin(pinService.hashPin(pin));

        redisTemplate.delete(VERIFY_SESSION_PREFIX + verifyToken);

        return issueTokens(user.getId());
    }

    @Transactional
    public LoginResponse loginWithPin(String phoneNumber, String pin) {
        String failKey = PIN_FAIL_PREFIX + phoneNumber;
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null) {
            try {
                if (Long.parseLong(failCount) >= MAX_PIN_ATTEMPTS) {
                    throw new BusinessException(ErrorCode.PIN_ATTEMPTS_EXCEEDED);
                }
            } catch (NumberFormatException e) {
                redisTemplate.delete(failKey);
            }
        }

        String phoneLast4 = phoneNumber.substring(phoneNumber.length() - 4);
        List<User> candidates = userRepository.findByPhoneLast4(phoneLast4);

        User user = candidates.stream()
                .filter(u -> phoneNumber.equals(u.getPhoneNumber()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        checkUserStatus(user);

        if (user.getPinHash() == null) {
            throw new BusinessException(ErrorCode.PIN_NOT_SET);
        }

        if (!pinService.verifyPin(pin, user.getPinHash())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, PIN_FAIL_TTL_MINUTES, TimeUnit.MINUTES);
            throw new BusinessException(ErrorCode.PIN_INVALID);
        }

        redisTemplate.delete(failKey);

        user.updateLastLogin();
        return issueTokens(user.getId());
    }

    @Transactional(readOnly = true)
    public LoginResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                newRefreshToken,
                jwtTokenProvider.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String accessToken) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(accessToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        long remainingMs = jwtTokenProvider.getRemainingExpiration(accessToken);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "logout",
                    remainingMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    // ───────────── 테스트 로그인 ─────────────

    @Transactional
    public LoginResponse testLogin(String identifier) {
        String targetCi = (identifier != null && !identifier.isBlank())
                ? "test-ci-" + identifier
                : "test-ci-default";

        User user = userRepository.findByCi(targetCi)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .ci(targetCi)
                                .di("test-di-" + targetCi)
                                .name("테스트 사용자")
                                .birthDate(LocalDate.of(1990, 1, 1))
                                .gender("M")
                                .phoneNumber("01000000000")
                                .phoneLast4("0000")
                                .build()
                ));

        return issueTokens(user.getId());
    }

    // ───────────── 공통 ─────────────

    private LoginResponse issueTokens(Long userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                jwtTokenProvider.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );

        return new LoginResponse(accessToken, refreshToken);
    }

    private void checkUserStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWN);
        }
    }
}
