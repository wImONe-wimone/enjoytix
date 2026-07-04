package com.wimone.enjoytix.framework.convention.exception;

import com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode;
import com.wimone.enjoytix.framework.convention.errorcode.IErrorCode;

public class RemoteException extends AbstractException {

    public RemoteException(String message) {
        super(BaseErrorCode.REMOTE_ERROR, message);
    }

    public RemoteException(IErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
