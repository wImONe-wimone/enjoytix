package com.wimone.enjoytix.framework.distributedid.config;

import com.wimone.enjoytix.framework.distributedid.core.IdGenerator;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.framework.distributedid.core.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(DistributedIdProperties.class)
public class DistributedIdAutoConfiguration {

    @Bean
    public IdGenerator idGenerator(DistributedIdProperties properties) {
        return new SnowflakeIdGenerator(properties.getWorkerId());
    }

    @Bean
    public IdGeneratorManager idGeneratorManager(IdGenerator idGenerator) {
        return new IdGeneratorManager(idGenerator);
    }
}
