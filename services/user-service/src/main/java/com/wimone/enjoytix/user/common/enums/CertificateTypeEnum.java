package com.wimone.enjoytix.user.common.enums;

public enum CertificateTypeEnum {

    ID_CARD("ID_CARD"),
    PASSPORT("PASSPORT");

    private final String code;

    CertificateTypeEnum(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
