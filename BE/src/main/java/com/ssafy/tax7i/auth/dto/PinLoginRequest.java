package com.ssafy.tax7i.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PinLoginRequest(
        @NotBlank(message = "휴대폰번호는 필수입니다.")
        @Pattern(regexp = "^01[0-9]\\d{7,8}$", message = "휴대폰번호 형식이 올바르지 않습니다.")
        String phoneNumber,

        @NotBlank(message = "PIN은 필수입니다.")
        @Size(min = 6, max = 6, message = "PIN은 6자리여야 합니다.")
        String pin
) {
}
