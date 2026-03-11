package com.ssafy.tax7i.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CallbackRequest(
        @NotBlank(message = "인가 코드는 필수입니다.")
        String code
) {
}
