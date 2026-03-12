package com.ssafy.tax7i.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    MISSING_FIELD(HttpStatus.BAD_REQUEST, "필수 항목이 누락되었습니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "데이터 형식이 올바르지 않습니다."),
    DUPLICATE_VALUE(HttpStatus.BAD_REQUEST, "중복된 값입니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // 409 Conflict
    CONFLICT(HttpStatus.CONFLICT, "리소스 충돌이 발생했습니다."),

    // 402 Payment Required
    PAYMENT_DECLINED(HttpStatus.valueOf(402), "결제가 거절되었습니다."),

    // 404 Not Found
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),

    // 422 Unprocessable Entity
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "잔액이 부족합니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    OAUTH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAuth 인증에 실패했습니다."),

    // 503 Service Unavailable
    BANK_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "은행 연동 서비스가 일시적으로 불가합니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getHttpStatusCode() {
        return httpStatus.value();
    }
}
