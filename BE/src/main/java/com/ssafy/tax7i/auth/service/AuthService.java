package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.domain.UserStatus;
import com.ssafy.tax7i.auth.dto.*;
import com.ssafy.tax7i.auth.repository.UserConsentRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BL:";

    private final NiceIdentityMockService niceIdentityMockService;
    private final PinService pinService;
    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public IdentityVerifyResponse verifyIdentity(IdentityVerifyRequest request) {
        NiceIdentityMockService.VerificationResult result = niceIdentityMockService.verify(request);

        User user = userRepository.findByCi(result.ci()).orElse(null);
        boolean isNewUser = (user == null);

        if (isNewUser) {
            String phoneLast4 = result.phoneNumber().length() >= 4
                    ? result.phoneNumber().substring(result.phoneNumber().length() - 4)
                    : result.phoneNumber();

            user = userRepository.save(User.builder()
                    .ci(result.ci())
                    .di(result.di())
                    .name(result.name())
                    .birthDate(LocalDate.parse(result.birthDate()))
                    .gender(result.gender())
                    .phoneNumber(result.phoneNumber())
                    .phoneLast4(phoneLast4)
                    .build());
        } else {
            checkUserStatus(user);
        }

        user.markKycVerified();

        boolean requiresPinSetup = user.getPinHash() == null;
        boolean requiresConsent = userConsentRepository.findByUserId(user.getId()).isEmpty();

        return new IdentityVerifyResponse(user.getId(), isNewUser, requiresPinSetup, requiresConsent);
    }

    @Transactional
    public LoginResponse setupPin(Long userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        checkUserStatus(user);

        String pinHash = pinService.hashPin(pin);
        user.setupPin(pinHash);
        user.updateLastLogin();

        return issueTokens(user);
    }

    @Transactional
    public LoginResponse loginWithPin(String ci, String pin) {
        User user = userRepository.findByCi(ci)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        checkUserStatus(user);

        if (user.getPinHash() == null) {
            throw new BusinessException(ErrorCode.PIN_NOT_SET);
        }

        if (!pinService.verifyPin(pin, user.getPinHash())) {
            throw new BusinessException(ErrorCode.PIN_INVALID);
        }

        user.updateLastLogin();
        return issueTokens(user);
    }

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

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
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
