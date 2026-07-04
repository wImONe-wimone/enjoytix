package com.wimone.enjoytix.user.common.enums;

public enum UserStatusEnum {

    DISABLED(0),
    ENABLED(1);

    private final int code;

    UserStatusEnum(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
