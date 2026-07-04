package com.wimone.enjoytix.framework.convention.exception;

import com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode;
import com.wimone.enjoytix.framework.convention.errorcode.IErrorCode;

public class ServiceException extends AbstractException {

    public ServiceException(String message) {
        super(BaseErrorCode.SERVICE_ERROR, message);
    }

    public ServiceException(IErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ServiceException(IErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
