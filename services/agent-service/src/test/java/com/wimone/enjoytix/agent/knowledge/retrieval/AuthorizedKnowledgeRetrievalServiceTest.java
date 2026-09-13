package com.wimone.enjoytix.agent.knowledge.retrieval;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizedKnowledgeRetrievalServiceTest {
    private final AgentUserContext user = new AgentUserContext(7L, "alice");

    @Test
    void searchesOnlyAuthorizedKnowledgeBasesAndBoundsCandidates() {
        RecordingStore store = new RecordingStore(List.of(document("kb-allowed", "doc-1", "v1", "c1", 0.9)));
        RagProperties properties = new RagProperties();
        properties.setRetrievalEnabled(true);
        properties.setCandidateLimit(3);
        properties.setTopK(1);
        AuthorizedKnowledgeRetrievalService service = new AuthorizedKnowledgeRetrievalService(
                store, ignored -> Set.of("kb-allowed"), (query, documents) -> documents, properties);

        KnowledgeRetrievalResult result = service.retrieve(new KnowledgeRetrievalRequest(user, "refund", 1, 0.5));

        assertThat(result.outcome()).isEqualTo(KnowledgeRetrievalOutcome.SUCCESS);
        assertThat(result.citations()).hasSize(1);
        assertThat(store.requests).hasSize(1);
        assertThat(store.requests.get(0).getTopK()).isEqualTo(3);
        assertThat(store.requests.get(0).getFilterExpression().toString()).contains("kb-allowed");
    }

    @Test
    void deniesWhenNoAuthorizedScopeExistsWithoutCallingVectorStore() {
        RecordingStore store = new RecordingStore(List.of());
        AuthorizedKnowledgeRetrievalService service = new AuthorizedKnowledgeRetrievalService(
                store, ignored -> Set.of(), (query, documents) -> documents, new RagProperties());

        KnowledgeRetrievalResult result = service.retrieve(new KnowledgeRetrievalRequest(user, "refund", 2, 0.5));

        assertThat(result.outcome()).isEqualTo(KnowledgeRetrievalOutcome.NO_HIT);
        assertThat(store.requests).isEmpty();
    }

    @Test
    void classifiesVectorStoreFailure() {
        RagProperties properties = new RagProperties();
        properties.setRetrievalEnabled(true);
        VectorStore failing = new RecordingStore(new IllegalStateException("provider timeout"));
        AuthorizedKnowledgeRetrievalService service = new AuthorizedKnowledgeRetrievalService(
                failing, ignored -> Set.of("kb-allowed"), (query, documents) -> documents, properties);

        KnowledgeRetrievalResult result = service.retrieve(new KnowledgeRetrievalRequest(user, "refund", 2, 0.5));

        assertThat(result.outcome()).isEqualTo(KnowledgeRetrievalOutcome.FAILURE);
        assertThat(result.metadata()).containsEntry("failureCategory", "VECTOR_STORE");
    }

    @Test
    void filtersUnauthorizedTenantDocumentVersionAndChunkBeforeReturningCitations() {
        RecordingStore store = new RecordingStore(List.of(
                document("kb-allowed", "doc-1", "v1", "c1", 0.9),
                document("kb-other-tenant", "doc-2", "v9", "c9", 0.99),
                documentWithActive("kb-allowed", "doc-1", "v0", "c0", 0.98, false),
                documentWithoutChunk("kb-allowed", "doc-1", "v2", 0.97)));
        RagProperties properties = new RagProperties();
        properties.setRetrievalEnabled(true);
        properties.setCandidateLimit(10);
        properties.setTopK(10);
        AuthorizedKnowledgeRetrievalService service = new AuthorizedKnowledgeRetrievalService(
                store, ignored -> Set.of("kb-allowed"), (query, documents) -> documents, properties);

        KnowledgeRetrievalResult result = service.retrieve(new KnowledgeRetrievalRequest(user, "refund", 10, 0.5));

        assertThat(result.outcome()).isEqualTo(KnowledgeRetrievalOutcome.SUCCESS);
        assertThat(result.citations()).extracting(KnowledgeCitation::knowledgeBaseId).containsExactly("kb-allowed");
        assertThat(result.citations()).extracting(KnowledgeCitation::versionId).containsExactly("v1");
        assertThat(result.citations()).extracting(KnowledgeCitation::chunkId).containsExactly("c1");
        assertThat(result.metadata()).containsEntry("candidateCount", "1");
    }

    private Document documentWithActive(String kb, String doc, String version, String chunk,
                                        double score, boolean active) {
        return Document.builder().id(kb + "-" + version + "-" + chunk).text("refund content").score(score)
                .metadata(java.util.Map.of("knowledgeBaseId", kb, "documentId", doc,
                        "versionId", version, "chunkId", chunk, "active", active,
                        "title", "Refund policy", "sourceName", "policy.md", "generationId", "gen-1")).build();
    }

    private Document documentWithoutChunk(String kb, String doc, String version, double score) {
        return Document.builder().id(kb + "-" + version).text("refund content").score(score)
                .metadata(java.util.Map.of("knowledgeBaseId", kb, "documentId", doc,
                        "versionId", version, "title", "Refund policy", "sourceName", "policy.md", "generationId", "gen-1")).build();
    }
    private Document document(String kb, String doc, String version, String chunk, double score) {
        return Document.builder().id("idx-1").text("refund content").score(score)
                .metadata(java.util.Map.of("knowledgeBaseId", kb, "documentId", doc,
                        "versionId", version, "chunkId", chunk, "title", "Refund policy",
                        "sourceName", "policy.md", "generationId", "gen-1")).build();
    }

    private static final class RecordingStore implements VectorStore {
        private final List<SearchRequest> requests = new ArrayList<>();
        private final List<Document> documents;
        private final RuntimeException failure;
        private RecordingStore(List<Document> documents) { this.documents = documents; this.failure = null; }
        private RecordingStore(RuntimeException failure) { this.documents = List.of(); this.failure = failure; }
        public String getName() { return "recording"; }
        public void add(List<Document> documents) { }
        public List<Document> similaritySearch(SearchRequest request) {
            requests.add(request);
            if (failure != null) throw failure;
            return documents;
        }
        public void delete(List<String> ids) { }
        public void delete(Filter.Expression expression) { }
    }
}
