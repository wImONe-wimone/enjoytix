package com.wimone.enjoytix.framework.convention.exception;

import com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode;
import com.wimone.enjoytix.framework.convention.errorcode.IErrorCode;

public abstract class AbstractException extends RuntimeException {

    private final String code;

    protected AbstractException(String message) {
        this(BaseErrorCode.SERVICE_ERROR, message, null);
    }

    protected AbstractException(IErrorCode errorCode) {
        this(errorCode, errorCode.message(), null);
    }

    protected AbstractException(IErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    protected AbstractException(IErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.code();
    }

    public String code() {
        return code;
    }
}
