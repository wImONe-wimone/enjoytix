package com.wimone.enjoytix.framework.distributedid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enjoytix.distributed-id")
public class DistributedIdProperties {

    private long workerId = 1L;

    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workerId) {
        this.workerId = workerId;
    }
}
