package com.wimone.enjoytix.framework.database.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.wimone.enjoytix.framework.database.handler.CustomIdGenerator;
import com.wimone.enjoytix.framework.database.handler.MyMetaObjectHandler;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class MybatisPlusAutoConfiguration {

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();
    }

    @Bean
    public IdentifierGenerator identifierGenerator(IdGeneratorManager idGeneratorManager) {
        return new CustomIdGenerator(idGeneratorManager);
    }
}
