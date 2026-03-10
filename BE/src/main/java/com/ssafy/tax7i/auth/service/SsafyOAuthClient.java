package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.dto.SsafyTokenResponse;
import com.ssafy.tax7i.auth.dto.SsafyUserInfoResponse;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SsafyOAuthClient {

    private final RestTemplate restTemplate;

    @Value("${ssafy.oauth.client-id}")
    private String clientId;

    @Value("${ssafy.oauth.client-secret}")
    private String clientSecret;

    @Value("${ssafy.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${ssafy.oauth.token-uri}")
    private String tokenUri;

    @Value("${ssafy.oauth.user-info-uri}")
    private String userInfoUri;

    public SsafyTokenResponse getToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<SsafyTokenResponse> response =
                    restTemplate.postForEntity(tokenUri, request, SsafyTokenResponse.class);
            SsafyTokenResponse body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "SSAFY OAuth 토큰 응답이 비어있습니다.");
            }
            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("SSAFY OAuth 토큰 교환 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "SSAFY OAuth 토큰 교환에 실패했습니다.");
        }
    }

    public SsafyUserInfoResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<SsafyUserInfoResponse> response =
                    restTemplate.exchange(userInfoUri, HttpMethod.GET, request, SsafyUserInfoResponse.class);
            SsafyUserInfoResponse body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "SSAFY 유저 정보 응답이 비어있습니다.");
            }
            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("SSAFY 유저 정보 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "SSAFY 유저 정보 조회에 실패했습니다.");
        }
    }
}
