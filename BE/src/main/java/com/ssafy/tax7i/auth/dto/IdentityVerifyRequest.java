package com.ssafy.tax7i.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IdentityVerifyRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "생년월일은 필수입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 YYYY-MM-DD 형식이어야 합니다.")
        String birthDate,

        @NotBlank(message = "성별은 필수입니다.")
        @Pattern(regexp = "^[MF]$", message = "성별은 M 또는 F여야 합니다.")
        String gender,

        @NotBlank(message = "휴대폰번호는 필수입니다.")
        @Pattern(regexp = "^01[0-9]\\d{7,8}$", message = "휴대폰번호 형식이 올바르지 않습니다.")
        String phoneNumber
) {
}
