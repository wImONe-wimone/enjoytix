package com.wimone.enjoytix.agent.knowledge.chunk;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("agent.knowledge.chunk")
public record TextChunkProperties(int maxCharacters, int overlapCharacters) {
    public TextChunkProperties {
        if (maxCharacters <= 0) throw new IllegalArgumentException("maximum chunk characters must be positive");
        if (overlapCharacters < 0 || overlapCharacters >= maxCharacters) throw new IllegalArgumentException("chunk overlap must be less than maximum chunk characters");
    }
}
