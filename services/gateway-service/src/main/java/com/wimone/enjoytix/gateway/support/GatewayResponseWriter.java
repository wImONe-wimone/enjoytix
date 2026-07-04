package com.wimone.enjoytix.gateway.support;

import com.alibaba.fastjson2.JSON;
import com.wimone.enjoytix.framework.base.trace.TraceConstants;
import com.wimone.enjoytix.framework.convention.errorcode.IErrorCode;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public final class GatewayResponseWriter {

    private GatewayResponseWriter() {
    }

    public static Mono<Void> write(ServerWebExchange exchange, IErrorCode errorCode, String message) {
        Result<Void> result = Result.failure(errorCode, message);
        result.setRequestId(exchange.getRequest().getHeaders().getFirst(TraceConstants.REQUEST_ID_HEADER));
        byte[] body = JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(resolveHttpStatus(errorCode.code()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static HttpStatus resolveHttpStatus(String code) {
        return switch (code) {
            case "A0003" -> HttpStatus.UNAUTHORIZED;
            case "A0004" -> HttpStatus.FORBIDDEN;
            case "C0002" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
