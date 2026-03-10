package com.ssafy.tax7i.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyTokenResponse(
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("scope") String scope,
        @JsonProperty("refresh_token_expires_in") Long refreshTokenExpiresIn
) {
}
