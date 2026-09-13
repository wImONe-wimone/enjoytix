package com.wimone.enjoytix.agent.knowledge.source;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.Set;

@ConfigurationProperties("agent.knowledge.source")
public record KnowledgeSourceProperties(long maxBytes, Duration connectTimeout, Duration readTimeout,
                                        Set<String> allowedContentTypes) {
    public KnowledgeSourceProperties {
        if (maxBytes <= 0) throw new IllegalArgumentException("maximum size must be positive");
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) throw new IllegalArgumentException("connect timeout must be positive");
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) throw new IllegalArgumentException("read timeout must be positive");
        allowedContentTypes = allowedContentTypes == null ? Set.of() : Set.copyOf(allowedContentTypes);
    }
}
