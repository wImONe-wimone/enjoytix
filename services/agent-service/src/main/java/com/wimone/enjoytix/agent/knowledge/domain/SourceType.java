package com.wimone.enjoytix.agent.knowledge.domain;

public enum SourceType {
    UPLOAD("upload"),
    URL("url");

    private final String code;

    SourceType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
