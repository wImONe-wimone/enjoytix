package com.wimone.enjoytix.agent.knowledge.domain;

public enum ProcessMode {
    CHUNK("chunk"),
    PIPELINE("pipeline");

    private final String code;

    ProcessMode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
