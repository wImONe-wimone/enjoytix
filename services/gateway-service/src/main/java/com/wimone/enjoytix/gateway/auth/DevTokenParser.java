package com.wimone.enjoytix.gateway.auth;

import java.util.Optional;

public final class DevTokenParser {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEV_TOKEN_PREFIX = "dev-";

    private DevTokenParser() {
    }

    public static Optional<AuthUser> parse(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        if (!token.startsWith(DEV_TOKEN_PREFIX)) {
            return Optional.empty();
        }
        String rawUserId = token.substring(DEV_TOKEN_PREFIX.length());
        try {
            Long userId = Long.valueOf(rawUserId);
            return Optional.of(new AuthUser(userId, "dev-user-" + userId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
