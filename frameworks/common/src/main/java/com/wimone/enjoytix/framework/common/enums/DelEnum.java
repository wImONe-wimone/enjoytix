package com.wimone.enjoytix.framework.common.enums;

public enum DelEnum {

    NORMAL(0),
    DELETED(1);

    private final int code;

    DelEnum(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
