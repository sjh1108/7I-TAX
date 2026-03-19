package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NiceIdentityMockServiceTest {

    private final NiceIdentityMockService service = new NiceIdentityMockService();

    @Test
    void verify_CI_DI생성() {
        IdentityVerifyRequest request = new IdentityVerifyRequest(
                "홍길동", "1990-01-01", "M", "01012345678");

        NiceIdentityMockService.VerificationResult result = service.verify(request);

        assertThat(result.ci()).isNotNull();
        assertThat(result.di()).isNotNull();
        assertThat(result.ci()).hasSize(64); // SHA-256 hex length
        assertThat(result.di()).hasSize(64);
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.birthDate()).isEqualTo("1990-01-01");
        assertThat(result.gender()).isEqualTo("M");
        assertThat(result.phoneNumber()).isEqualTo("01012345678");
    }

    @Test
    void verify_CI와DI_서로다름() {
        IdentityVerifyRequest request = new IdentityVerifyRequest(
                "홍길동", "1990-01-01", "M", "01012345678");

        NiceIdentityMockService.VerificationResult result = service.verify(request);

        assertThat(result.ci()).isNotEqualTo(result.di());
    }

    @Test
    void verify_동일입력_동일CI() {
        IdentityVerifyRequest request = new IdentityVerifyRequest(
                "홍길동", "1990-01-01", "M", "01012345678");

        NiceIdentityMockService.VerificationResult result1 = service.verify(request);
        NiceIdentityMockService.VerificationResult result2 = service.verify(request);

        assertThat(result1.ci()).isEqualTo(result2.ci());
        assertThat(result1.di()).isEqualTo(result2.di());
    }

    @Test
    void verify_다른입력_다른CI() {
        NiceIdentityMockService.VerificationResult result1 = service.verify(
                new IdentityVerifyRequest("홍길동", "1990-01-01", "M", "01012345678"));
        NiceIdentityMockService.VerificationResult result2 = service.verify(
                new IdentityVerifyRequest("김철수", "1995-05-05", "M", "01087654321"));

        assertThat(result1.ci()).isNotEqualTo(result2.ci());
        assertThat(result1.di()).isNotEqualTo(result2.di());
    }
}
