package com.wimone.enjoytix.framework.common.toolkit;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.exception.ServiceException;

public final class Assert {

    private Assert() {
    }

    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new ClientException(message);
        }
    }

    public static void hasText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ClientException(message);
        }
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new ClientException(message);
        }
    }

    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new ServiceException(message);
        }
    }
}
