package com.ssafy.tax7i.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PinSetupRequest(
        @NotBlank(message = "PIN은 필수입니다.")
        @Size(min = 6, max = 6, message = "PIN은 6자리여야 합니다.")
        String pin
) {
}
