package com.wimone.enjoytix.framework.convention.errorcode;

public enum BaseErrorCode implements IErrorCode {

    SUCCESS("0", "Success"),
    CLIENT_ERROR("A0001", "Client request error"),
    VALIDATION_ERROR("A0002", "Request parameter validation failed"),
    UNAUTHORIZED("A0003", "Unauthorized"),
    FORBIDDEN("A0004", "Forbidden"),
    NOT_FOUND("A0005", "Resource not found"),
    SERVICE_ERROR("B0001", "Service processing failed"),
    REMOTE_ERROR("C0001", "Remote service call failed"),
    TOO_MANY_REQUESTS("C0002", "Too many requests");

    private final String code;
    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
