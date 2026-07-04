package com.wimone.enjoytix.framework.database.handler;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;

public class CustomIdGenerator implements IdentifierGenerator {

    private final IdGeneratorManager idGeneratorManager;

    public CustomIdGenerator(IdGeneratorManager idGeneratorManager) {
        this.idGeneratorManager = idGeneratorManager;
    }

    @Override
    public Number nextId(Object entity) {
        return idGeneratorManager.nextId();
    }
}
