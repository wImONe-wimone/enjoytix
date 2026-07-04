package com.wimone.enjoytix.framework.convention.result;

import com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode;
import com.wimone.enjoytix.framework.convention.errorcode.IErrorCode;

import java.io.Serializable;

public class Result<T> implements Serializable {

    private String code;
    private String message;
    private T data;
    private String requestId;

    public Result() {
    }

    public Result(String code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(BaseErrorCode.SUCCESS.code(), BaseErrorCode.SUCCESS.message(), data, null);
    }

    public static <T> Result<T> failure(IErrorCode errorCode) {
        return failure(errorCode, errorCode.message());
    }

    public static <T> Result<T> failure(IErrorCode errorCode, String message) {
        return new Result<>(errorCode.code(), message, null, null);
    }

    public boolean isSuccess() {
        return BaseErrorCode.SUCCESS.code().equals(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
