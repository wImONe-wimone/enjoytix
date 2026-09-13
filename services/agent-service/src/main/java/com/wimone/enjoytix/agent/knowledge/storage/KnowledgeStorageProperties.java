package com.wimone.enjoytix.agent.knowledge.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("agent.knowledge.storage")
public record KnowledgeStorageProperties(boolean enabled, String endpoint, String region, String bucket,
                                         String accessKey, String secretKey, boolean pathStyleAccess, long maxBytes) {
    @Override public String toString() {
        return "KnowledgeStorageProperties[enabled=" + enabled + ", endpoint=" + endpoint + ", region=" + region
                + ", bucket=" + bucket + ", pathStyleAccess=" + pathStyleAccess + ", maxBytes=" + maxBytes + "]";
    }
}
