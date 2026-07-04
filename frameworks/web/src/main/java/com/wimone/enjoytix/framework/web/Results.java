package com.wimone.enjoytix.framework.web;

import com.wimone.enjoytix.framework.base.trace.TraceContext;
import com.wimone.enjoytix.framework.convention.errorcode.IErrorCode;
import com.wimone.enjoytix.framework.convention.result.Result;

public final class Results {

    private Results() {
    }

    public static <T> Result<T> success() {
        return attachRequestId(Result.success());
    }

    public static <T> Result<T> success(T data) {
        return attachRequestId(Result.success(data));
    }

    public static <T> Result<T> failure(IErrorCode errorCode) {
        return attachRequestId(Result.failure(errorCode));
    }

    public static <T> Result<T> failure(IErrorCode errorCode, String message) {
        return attachRequestId(Result.failure(errorCode, message));
    }

    private static <T> Result<T> attachRequestId(Result<T> result) {
        result.setRequestId(TraceContext.getRequestId());
        return result;
    }
}
