package com.wimone.enjoytix.agent.knowledge.health;

import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.embedding.EmbeddingPort;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.actuate.health.Status;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagHealthIndicatorTest {
    @Test
    void disabledRagIsHealthyWithoutProviderCredentialsOrDependencies() {
        RagProperties properties = new RagProperties();
        RagHealthIndicator indicator = new RagHealthIndicator(properties, null, null);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails()).containsEntry("ragEnabled", false);
    }

    @Test
    void enabledRagReportsDownWhenEmbeddingOrVectorStoreIsUnavailable() {
        RagProperties properties = new RagProperties();
        properties.setRetrievalEnabled(true);
        RagHealthIndicator indicator = new RagHealthIndicator(properties, null, null);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails()).containsEntry("reason", "DEPENDENCY_UNAVAILABLE");
    }

    @Test
    void enabledRagReportsUpWhenDependenciesAreAvailable() {
        RagProperties properties = new RagProperties();
        properties.setRetrievalEnabled(true);
        EmbeddingPort embedding = texts -> texts.stream().map(text -> new float[]{1, 0}).toList();
        VectorStore store = new VectorStore() {
            public String getName() { return "test-store"; }
            public void add(List<Document> documents) { }
            public List<Document> similaritySearch(SearchRequest request) { return List.of(); }
            public void delete(List<String> ids) { }
            public void delete(Filter.Expression expression) { }
        };

        assertThat(new RagHealthIndicator(properties, embedding, store).health().getStatus()).isEqualTo(Status.UP);
    }
}