package com.wimone.enjoytix.framework.convention.exception;

import com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode;
import com.wimone.enjoytix.framework.convention.errorcode.IErrorCode;

public class ClientException extends AbstractException {

    public ClientException(String message) {
        super(BaseErrorCode.CLIENT_ERROR, message);
    }

    public ClientException(IErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
