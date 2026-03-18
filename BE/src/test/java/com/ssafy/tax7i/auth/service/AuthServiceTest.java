package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.domain.UserStatus;
import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import com.ssafy.tax7i.auth.dto.IdentityVerifyResponse;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.repository.UserConsentRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private NiceIdentityMockService niceIdentityMockService;
    @Mock private PinService pinService;
    @Mock private UserRepository userRepository;
    @Mock private UserConsentRepository userConsentRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    // ───────────── verifyIdentity ─────────────

    @Test
    void verifyIdentity_신규유저_회원가입() {
        IdentityVerifyRequest request = new IdentityVerifyRequest("홍길동", "1990-01-01", "M", "01012345678");
        NiceIdentityMockService.VerificationResult result = new NiceIdentityMockService.VerificationResult(
                "test-ci", "test-di", "홍길동", "1990-01-01", "M", "01012345678");

        given(niceIdentityMockService.verify(request)).willReturn(result);
        given(userRepository.findByCi("test-ci")).willReturn(Optional.empty());
        User savedUser = createUserWithId(1L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(userConsentRepository.findByUserId(1L)).willReturn(Collections.emptyList());

        IdentityVerifyResponse response = authService.verifyIdentity(request);

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.requiresPinSetup()).isTrue();
        assertThat(response.requiresConsent()).isTrue();
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void verifyIdentity_기존유저_조회() {
        IdentityVerifyRequest request = new IdentityVerifyRequest("홍길동", "1990-01-01", "M", "01012345678");
        NiceIdentityMockService.VerificationResult result = new NiceIdentityMockService.VerificationResult(
                "test-ci", "test-di", "홍길동", "1990-01-01", "M", "01012345678");

        User existingUser = createUserWithId(1L);
        given(niceIdentityMockService.verify(request)).willReturn(result);
        given(userRepository.findByCi("test-ci")).willReturn(Optional.of(existingUser));
        given(userConsentRepository.findByUserId(1L)).willReturn(Collections.emptyList());

        IdentityVerifyResponse response = authService.verifyIdentity(request);

        assertThat(response.isNewUser()).isFalse();
        then(userRepository).should(never()).save(any(User.class));
    }

    // ───────────── setupPin ─────────────

    @Test
    void setupPin_성공_토큰반환() {
        User user = createUserWithId(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(pinService.hashPin("123456")).willReturn("hashed-pin");
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        LoginResponse response = authService.setupPin(1L, "123456");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    // ───────────── loginWithPin ─────────────

    @Test
    void loginWithPin_성공() {
        User user = createUserWithId(1L);
        user.setupPin("hashed-pin");

        given(userRepository.findByCi("test-ci")).willReturn(Optional.of(user));
        given(pinService.verifyPin("123456", "hashed-pin")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        LoginResponse response = authService.loginWithPin("test-ci", "123456");

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void loginWithPin_PIN불일치_예외() {
        User user = createUserWithId(1L);
        user.setupPin("hashed-pin");

        given(userRepository.findByCi("test-ci")).willReturn(Optional.of(user));
        given(pinService.verifyPin("999999", "hashed-pin")).willReturn(false);

        assertThatThrownBy(() -> authService.loginWithPin("test-ci", "999999"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PIN_INVALID));
    }

    @Test
    void loginWithPin_PIN미설정_예외() {
        User user = createUserWithId(1L);

        given(userRepository.findByCi("test-ci")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.loginWithPin("test-ci", "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PIN_NOT_SET));
    }

    // ───────────── reissue ─────────────

    @Test
    void reissue_유효한refreshToken_새토큰반환() {
        given(jwtTokenProvider.validateToken("old-rt")).willReturn(true);
        given(jwtTokenProvider.getUserId("old-rt")).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:1")).willReturn("old-rt");
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("new-at");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("new-rt");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);

        LoginResponse response = authService.reissue("old-rt");

        assertThat(response.accessToken()).isEqualTo("new-at");
        assertThat(response.refreshToken()).isEqualTo("new-rt");
    }

    @Test
    void reissue_유효하지않은토큰_예외발생() {
        given(jwtTokenProvider.validateToken("invalid-rt")).willReturn(false);

        assertThatThrownBy(() -> authService.reissue("invalid-rt"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    // ───────────── logout ─────────────

    @Test
    void logout_유효한accessToken_RT삭제_BL등록() {
        given(jwtTokenProvider.validateToken("at")).willReturn(true);
        given(jwtTokenProvider.getUserId("at")).willReturn(1L);
        given(jwtTokenProvider.getRemainingExpiration("at")).willReturn(900000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        authService.logout("at");

        then(redisTemplate).should().delete("RT:1");
        then(valueOperations).should().set(eq("BL:at"), eq("logout"), anyLong(), any());
    }

    @Test
    void logout_유효하지않은토큰_예외발생() {
        given(jwtTokenProvider.validateToken("invalid-at")).willReturn(false);

        assertThatThrownBy(() -> authService.logout("invalid-at"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    // ───────────── helper ─────────────

    private User createUserWithId(Long id) {
        User user = User.builder()
                .ci("test-ci")
                .di("test-di")
                .name("홍길동")
                .build();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
