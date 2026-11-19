package com.comunidad360.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String messageKey;
    private final Map<String, Object> params;

    public BusinessException(HttpStatus status, String code, String messageKey) {
        this(status, code, messageKey, Map.of());
    }

    public BusinessException(HttpStatus status, String code, String messageKey, Map<String, Object> params) {
        super(messageKey);
        this.status = status;
        this.code = code;
        this.messageKey = messageKey;
        this.params = params == null ? Map.of() : Map.copyOf(params);
    }

    public static BusinessException unauthorized(String code, String messageKey) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, code, messageKey);
    }

    public static BusinessException forbidden(String code, String messageKey) {
        return new BusinessException(HttpStatus.FORBIDDEN, code, messageKey);
    }

    public static BusinessException notFound(String code, String messageKey) {
        return new BusinessException(HttpStatus.NOT_FOUND, code, messageKey);
    }

    public static BusinessException conflict(String code, String messageKey) {
        return new BusinessException(HttpStatus.CONFLICT, code, messageKey);
    }

    public static BusinessException unprocessable(String code, String messageKey) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, code, messageKey);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
