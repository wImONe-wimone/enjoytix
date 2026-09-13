package com.wimone.enjoytix.agent.knowledge.observability;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.retrieval.*;
import com.wimone.enjoytix.agent.knowledge.observability.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRetrievalObservationTest {
    @Test
    void recordsSafeRetrievalDimensionsWithoutQueryOrDocumentBody() {
        RecordingObserver observer = new RecordingObserver();
        RagProperties properties = new RagProperties();
        properties.setRetrievalEnabled(true);
        AuthorizedKnowledgeRetrievalService service = new AuthorizedKnowledgeRetrievalService(
                new Store(), ignored -> Set.of("kb-1"), (query, documents) -> documents, properties, observer);

        service.retrieve(new KnowledgeRetrievalRequest(new AgentUserContext(7L, "alice"),
                "secret question", 2, 0.5));

        assertThat(observer.value.outcome()).isEqualTo(KnowledgeRetrievalOutcome.SUCCESS);
        assertThat(observer.value.candidateCount()).isEqualTo(1);
        assertThat(observer.value.finalCount()).isEqualTo(1);
        assertThat(observer.value.citationCoverage()).isEqualTo(1.0);
        assertThat(observer.value.latencyMillis()).isGreaterThanOrEqualTo(0);
        assertThat(observer.value.toString()).doesNotContain("secret question", "secret body", "api-key");
    }

    static final class RecordingObserver implements KnowledgeRetrievalObserver {
        KnowledgeRetrievalObservation value;
        public void record(KnowledgeRetrievalObservation observation) { value = observation; }
    }

    static final class Store implements VectorStore {
        public String getName() { return "test"; }
        public void add(List<Document> documents) { }
        public List<Document> similaritySearch(SearchRequest request) {
            return List.of(Document.builder().id("idx-1").text("secret body").score(0.9)
                    .metadata(Map.of("knowledgeBaseId", "kb-1", "documentId", "doc-1", "versionId", "v1",
                            "chunkId", "c1", "title", "Policy", "sourceName", "policy.md", "active", true)).build());
        }
        public void delete(List<String> ids) { }
        public void delete(Filter.Expression expression) { }
    }
}