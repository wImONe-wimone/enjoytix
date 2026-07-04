package com.wimone.enjoytix.gateway.filter;

import com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode;
import com.wimone.enjoytix.gateway.auth.AuthUser;
import com.wimone.enjoytix.gateway.auth.DevTokenParser;
import com.wimone.enjoytix.gateway.support.GatewayResponseWriter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TokenValidateGatewayFilterFactory extends AbstractGatewayFilterFactory<TokenValidateGatewayFilterFactory.Config> {

    public TokenValidateGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            if (!isProtectedPath(config, path)) {
                return chain.filter(exchange);
            }
            Optional<AuthUser> authUser = DevTokenParser.parse(
                    exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)
            );
            if (authUser.isEmpty()) {
                return GatewayResponseWriter.write(exchange, BaseErrorCode.UNAUTHORIZED, "Missing or invalid token");
            }
            ServerWebExchange authorizedExchange = withUserHeaders(exchange, authUser.get());
            return chain.filter(authorizedExchange);
        };
    }

    private boolean isProtectedPath(Config config, String path) {
        return config.getBlackPathPre().stream().anyMatch(path::startsWith);
    }

    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, AuthUser authUser) {
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header("X-User-Id", String.valueOf(authUser.getUserId()))
                .header("X-Username", authUser.getUsername())
                .build();
        return exchange.mutate().request(request).build();
    }

    public static class Config {

        private List<String> blackPathPre = new ArrayList<>();

        public List<String> getBlackPathPre() {
            return blackPathPre;
        }

        public void setBlackPathPre(List<String> blackPathPre) {
            this.blackPathPre = blackPathPre == null ? new ArrayList<>() : blackPathPre;
        }
    }
}
