package com.wimone.enjoytix.gateway.support;

import com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Order(-2)
@Component
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatusCode statusCode = resolveStatusCode(ex);
        BaseErrorCode errorCode = resolveErrorCode(statusCode);
        String message = resolveMessage(errorCode, ex);
        logFailure(exchange, ex, statusCode, message);
        return GatewayResponseWriter.write(exchange, errorCode, message, statusCode);
    }

    private HttpStatusCode resolveStatusCode(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private BaseErrorCode resolveErrorCode(HttpStatusCode statusCode) {
        if (HttpStatus.NOT_FOUND.equals(statusCode)) {
            return BaseErrorCode.NOT_FOUND;
        }
        if (HttpStatus.UNAUTHORIZED.equals(statusCode)) {
            return BaseErrorCode.UNAUTHORIZED;
        }
        if (HttpStatus.FORBIDDEN.equals(statusCode)) {
            return BaseErrorCode.FORBIDDEN;
        }
        if (HttpStatus.TOO_MANY_REQUESTS.equals(statusCode)) {
            return BaseErrorCode.TOO_MANY_REQUESTS;
        }
        if (statusCode.is4xxClientError()) {
            return BaseErrorCode.CLIENT_ERROR;
        }
        return BaseErrorCode.SERVICE_ERROR;
    }

    private String resolveMessage(BaseErrorCode errorCode, Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {
            return responseStatusException.getReason();
        }
        return errorCode.message();
    }

    private void logFailure(ServerWebExchange exchange, Throwable ex, HttpStatusCode statusCode, String message) {
        String method = exchange.getRequest().getMethod() == null ? "UNKNOWN" : exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();
        if (statusCode.is4xxClientError()) {
            if (HttpStatus.NOT_FOUND.equals(statusCode) && "/favicon.ico".equals(path)) {
                log.debug("Gateway resource not found method={} path={} status={}", method, path, statusCode.value());
            } else {
                log.warn("Gateway request rejected method={} path={} status={} message={}",
                        method, path, statusCode.value(), message);
            }
            return;
        }
        log.error("Gateway request failed method={} path={} status={} message={}",
                method, path, statusCode.value(), message, ex);
    }
}
