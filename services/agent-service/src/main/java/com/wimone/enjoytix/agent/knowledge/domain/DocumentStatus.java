package com.wimone.enjoytix.agent.knowledge.domain;

public enum DocumentStatus {
    PENDING("pending"),
    RUNNING("running"),
    FAILED("failed"),
    SUCCESS("success");

    private final String code;

    DocumentStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
