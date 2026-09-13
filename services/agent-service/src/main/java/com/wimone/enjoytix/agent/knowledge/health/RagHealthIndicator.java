package com.wimone.enjoytix.agent.knowledge.health;

import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.embedding.EmbeddingPort;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component("rag")
public final class RagHealthIndicator implements HealthIndicator {
    private final RagProperties properties;
    private final EmbeddingPort embeddingPort;
    private final VectorStore vectorStore;

    @Autowired
    private RagHealthIndicator(RagProperties properties, ObjectProvider<EmbeddingPort> embeddingPort,
                              ObjectProvider<VectorStore> vectorStore) {
        this(properties, embeddingPort.getIfAvailable(), vectorStore.getIfAvailable());
    }

    public RagHealthIndicator(RagProperties properties, EmbeddingPort embeddingPort, VectorStore vectorStore) {
        this.properties = Objects.requireNonNull(properties, "RAG properties are required");
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
    }

    @Override
    public Health health() {
        boolean enabled = properties.isIndexingEnabled() || properties.isRetrievalEnabled()
                || properties.isRerankingEnabled() || properties.isCitationsEnabled();
        if (!enabled) {
            return Health.up().withDetails(Map.of("ragEnabled", false)).build();
        }
        if (embeddingPort == null || vectorStore == null) {
            return Health.down().withDetails(Map.of("ragEnabled", true,
                    "reason", "DEPENDENCY_UNAVAILABLE",
                    "embeddingAvailable", embeddingPort != null,
                    "vectorStoreAvailable", vectorStore != null)).build();
        }
        try {
            String storeName = vectorStore.getName();
            if (storeName == null || storeName.isBlank()) {
                return Health.down().withDetails(Map.of("ragEnabled", true,
                        "reason", "VECTOR_STORE_UNHEALTHY")).build();
            }
            return Health.up().withDetails(Map.of("ragEnabled", true,
                    "embeddingAvailable", true, "vectorStore", storeName)).build();
        } catch (RuntimeException failure) {
            return Health.down().withDetails(Map.of("ragEnabled", true,
                    "reason", "DEPENDENCY_UNAVAILABLE")).build();
        }
    }
}