package com.wimone.enjoytix.agent.knowledge.rollout;

import com.wimone.enjoytix.agent.auth.AgentUserContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class RagRolloutDecider {
    private final RagRolloutProperties properties;

    public RagRolloutDecider(RagRolloutProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled(AgentUserContext user, String cohortKey) {
        if (!properties.isEnabled() || properties.isRollback()) {
            return false;
        }
        if (internalUserIds().contains(user.userId())) {
            return true;
        }
        int percentage = properties.getCanaryPercentage();
        if (percentage <= 0) {
            return false;
        }
        if (percentage >= 100) {
            return true;
        }
        return bucket(user.userId(), cohortKey) < percentage;
    }

    public boolean isRollback() {
        return properties.isRollback();
    }

    public boolean isConfiguredEnabled() {
        return properties.isEnabled();
    }

    private Set<Long> internalUserIds() {
        return Arrays.stream(properties.getInternalUserIds().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(this::parseUserId)
                .filter(value -> value != null)
                .collect(Collectors.toSet());
    }

    private Long parseUserId(String value) {
        try {
            long userId = Long.parseLong(value);
            return userId > 0 ? userId : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int bucket(Long userId, String cohortKey) {
        try {
            byte[] input = (userId + "|" + (cohortKey == null ? "" : cohortKey))
                    .getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            long value = Integer.toUnsignedLong(ByteBuffer.wrap(digest).getInt());
            return (int) (value % 100);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}