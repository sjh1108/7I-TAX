package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * NICE 본인인증 Mock 서비스.
 * 실제 서비스에서는 NICE API(https://www.niceid.co.kr) 호출로 대체해야 합니다.
 */
@Slf4j
@Service
@Profile({"local", "dev", "test"})
public class NiceIdentityMockService {

    public VerificationResult verify(IdentityVerifyRequest request) {
        try {
            String ciSource = request.name() + request.birthDate() + request.phoneNumber();
            String ci = sha256(ciSource);

            // DI는 서비스 제공자별 고유값 (여기서는 "tax7i" prefix로 구분)
            String diSource = "tax7i:" + request.name() + request.birthDate() + request.phoneNumber();
            String di = sha256(diSource);

            log.info("Mock 본인인증 완료: name={}, ci={}", request.name(), ci.substring(0, 8) + "...");

            return new VerificationResult(
                    ci, di,
                    request.name(), request.birthDate(),
                    request.gender(), request.phoneNumber()
            );
        } catch (Exception e) {
            log.error("본인인증 처리 실패", e);
            throw new BusinessException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
        }
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    public record VerificationResult(
            String ci,
            String di,
            String name,
            String birthDate,
            String gender,
            String phoneNumber
    ) {
    }
}
