package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.dto.SsafyTokenResponse;
import com.ssafy.tax7i.auth.dto.SsafyUserInfoResponse;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private SsafyOAuthClient ssafyOAuthClient;
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private SsafyTokenResponse mockSsafyToken;
    private SsafyUserInfoResponse mockUserInfo;

    @BeforeEach
    void setUp() {
        mockSsafyToken = new SsafyTokenResponse(
                "bearer", "ssafy-at", "ssafy-rt", 3600L, "read", 2592000L);
        mockUserInfo = new SsafyUserInfoResponse("ssafy-user-001", "test@ssafy.com", "홍길동");
    }

    // ───────────── login ─────────────

    @Test
    void login_신규유저_회원가입후토큰반환() {
        User savedUser = createUserWithId(1L);
        given(ssafyOAuthClient.getToken("auth-code")).willReturn(mockSsafyToken);
        given(ssafyOAuthClient.getUserInfo("ssafy-at")).willReturn(mockUserInfo);
        given(userRepository.findBySsafyUserId("ssafy-user-001")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        LoginResponse response = authService.login("auth-code");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        then(userRepository).should().save(any(User.class));
        then(valueOperations).should().set(eq("RT:1"), eq("refresh-token"), anyLong(), any());
    }

    @Test
    void login_기존유저_로그인후토큰반환() {
        User existingUser = createUserWithId(1L);
        given(ssafyOAuthClient.getToken("auth-code")).willReturn(mockSsafyToken);
        given(ssafyOAuthClient.getUserInfo("ssafy-at")).willReturn(mockUserInfo);
        given(userRepository.findBySsafyUserId("ssafy-user-001")).willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        LoginResponse response = authService.login("auth-code");

        assertThat(response).isNotNull();
        then(userRepository).should(never()).save(any(User.class));
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
        then(valueOperations).should().set(eq("RT:1"), eq("new-rt"), anyLong(), any());
    }

    @Test
    void reissue_유효하지않은토큰_예외발생() {
        given(jwtTokenProvider.validateToken("invalid-rt")).willReturn(false);

        assertThatThrownBy(() -> authService.reissue("invalid-rt"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    @Test
    void reissue_Redis에토큰없음_예외발생() {
        given(jwtTokenProvider.validateToken("rt")).willReturn(true);
        given(jwtTokenProvider.getUserId("rt")).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:1")).willReturn(null);

        assertThatThrownBy(() -> authService.reissue("rt"))
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

    @Test
    void logout_잔여만료시간0이하_BL등록안함() {
        given(jwtTokenProvider.validateToken("at")).willReturn(true);
        given(jwtTokenProvider.getUserId("at")).willReturn(1L);
        given(jwtTokenProvider.getRemainingExpiration("at")).willReturn(0L);

        authService.logout("at");

        then(redisTemplate).should().delete("RT:1");
        then(redisTemplate).should(never()).opsForValue();
    }

    // ───────────── helper ─────────────

    private User createUserWithId(Long id) {
        User user = User.builder()
                .ssafyUserId("ssafy-user-001")
                .email("test@ssafy.com")
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
