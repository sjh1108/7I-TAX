package com.ssafy.tax7i.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyUserInfoResponse(
        @JsonProperty("userId") String userId,
        @JsonProperty("email") String email,
        @JsonProperty("name") String name
) {
}
