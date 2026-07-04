package com.wimone.enjoytix.framework.common.enums;

public enum StatusEnum {

    DISABLED(0),
    ENABLED(1);

    private final int code;

    StatusEnum(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
