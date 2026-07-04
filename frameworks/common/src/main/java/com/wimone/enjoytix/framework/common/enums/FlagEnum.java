package com.wimone.enjoytix.framework.common.enums;

public enum FlagEnum {

    FALSE(0),
    TRUE(1);

    private final int code;

    FlagEnum(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
