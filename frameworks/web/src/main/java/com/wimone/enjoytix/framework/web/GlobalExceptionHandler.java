package com.wimone.enjoytix.framework.web;

import com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode;
import com.wimone.enjoytix.framework.convention.exception.AbstractException;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AbstractException.class)
    public Result<Void> handleAbstractException(AbstractException ex) {
        log.warn("Business exception: code={}, message={}", ex.code(), ex.getMessage());
        Result<Void> result = new Result<>();
        result.setCode(ex.code());
        result.setMessage(ex.getMessage());
        return result;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Results.failure(BaseErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(Throwable.class)
    public Result<Void> handleThrowable(Throwable ex) {
        log.error("Unhandled server exception", ex);
        return Results.failure(BaseErrorCode.SERVICE_ERROR);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}
