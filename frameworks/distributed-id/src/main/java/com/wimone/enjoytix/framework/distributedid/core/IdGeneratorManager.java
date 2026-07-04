package com.wimone.enjoytix.framework.distributedid.core;

public class IdGeneratorManager {

    private final IdGenerator idGenerator;

    public IdGeneratorManager(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public long nextId() {
        return idGenerator.nextId();
    }
}
