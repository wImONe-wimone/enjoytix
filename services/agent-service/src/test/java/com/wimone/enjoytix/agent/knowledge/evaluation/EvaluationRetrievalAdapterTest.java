package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.retrieval.AuthorizedKnowledgeRetrievalService;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationRetrievalAdapterTest {
    @Test
    void evaluatesRegisteredCaseWithServerResolvedSubjectThroughAuthorizedRetrieval() {
        RagProperties properties = properties();
        RecordingStore store = new RecordingStore(List.of(document("idx-1", "kb-public", "doc-1", "v1", "c1")));
        List<AgentUserContext> authorizedUsers = new ArrayList<>();
        AuthorizedKnowledgeRetrievalService retrieval = new AuthorizedKnowledgeRetrievalService(
                store,
                user -> {
                    authorizedUsers.add(user);
                    return Set.of("kb-public");
                },
                (query, documents) -> documents,
                properties);
        String citationKey = citationKey("idx-1");
        EvaluationRetrievalAdapter adapter = adapter(retrieval, properties,
                new RetrievalEvaluationCase("1", "baseline-1", "case-1", "refund policy", "alice",
                        EvaluationExpectedOutcome.SUCCESS, Set.of(citationKey), "Refund policy answer.", Set.of("positive")));

        EvaluationEvidenceRecord evidence = adapter.evaluate("baseline", "case-1");

        assertThat(authorizedUsers).containsExactly(new AgentUserContext(7L, "alice"));
        assertThat(store.requests).hasSize(1);
        assertThat(store.requests.get(0).getQuery()).isEqualTo("refund policy");
        assertThat(evidence.retrievalOutcome()).isEqualTo(KnowledgeRetrievalOutcome.SUCCESS);
        assertThat(evidence.citations()).extracting(EvaluationEvidenceCitation::citationKey)
                .containsExactly(citationKey);
        assertThat(evidence.citations()).extracting(EvaluationEvidenceCitation::versionId).containsExactly("v1");
        assertThat(evidence.contexts()).extracting(EvaluationEvidenceContext::content)
                .containsExactly("Refund content.");
        assertThat(evidence.candidateCount()).isEqualTo(1);
        assertThat(evidence.finalCount()).isEqualTo(1);
        assertThat(evidence.retrievalDurationMillis()).isGreaterThanOrEqualTo(0);
        assertThat(evidence.deterministicResult().thresholdsMet()).isTrue();
    }

    @Test
    void emitsEmptyEvidenceForRegisteredNoHitCase() {
        RagProperties properties = properties();
        AuthorizedKnowledgeRetrievalService retrieval = new AuthorizedKnowledgeRetrievalService(
                new RecordingStore(List.of()),
                user -> Set.of(),
                (query, documents) -> documents,
                properties);
        EvaluationRetrievalAdapter adapter = adapter(retrieval, properties,
                new RetrievalEvaluationCase("1", "baseline-1", "case-2", "unrelated", "alice",
                        EvaluationExpectedOutcome.NO_HIT, Set.of(), null, Set.of("no-hit")));

        EvaluationEvidenceRecord evidence = adapter.evaluate("baseline", "case-2");

        assertThat(evidence.retrievalOutcome()).isEqualTo(KnowledgeRetrievalOutcome.NO_HIT);
        assertThat(evidence.citations()).isEmpty();
        assertThat(evidence.contexts()).isEmpty();
        assertThat(evidence.deterministicResult().thresholdsMet()).isTrue();
        assertThat(evidence.failure()).isNull();
    }

    @Test
    void classifiesAuthorizedRetrievalFailureWithoutExposingCause() {
        RagProperties properties = properties();
        AuthorizedKnowledgeRetrievalService retrieval = new AuthorizedKnowledgeRetrievalService(
                new RecordingStore(new IllegalStateException("api-key=secret storage=C:\\private")),
                user -> Set.of("kb-public"),
                (query, documents) -> documents,
                properties);
        EvaluationRetrievalAdapter adapter = adapter(retrieval, properties,
                new RetrievalEvaluationCase("1", "baseline-1", "case-3", "refund policy", "alice",
                        EvaluationExpectedOutcome.SUCCESS, Set.of("cite-missing"), "Refund policy answer.", Set.of("failure")));

        EvaluationEvidenceRecord evidence = adapter.evaluate("baseline", "case-3");

        assertThat(evidence.retrievalOutcome()).isEqualTo(KnowledgeRetrievalOutcome.FAILURE);
        assertThat(evidence.failure()).isNotNull();
        assertThat(evidence.failure().category()).isEqualTo("VECTOR_STORE");
        assertThat(evidence.failure().safeMessage()).doesNotContain("api-key", "secret", "C:\\private");
        assertThat(evidence.citations()).isEmpty();
        assertThat(evidence.contexts()).isEmpty();
    }

    private EvaluationRetrievalAdapter adapter(AuthorizedKnowledgeRetrievalService retrieval,
                                               RagProperties properties,
                                               RetrievalEvaluationCase evaluationCase) {
        EvaluationDataset dataset = new EvaluationDataset("baseline", "1", "baseline-1", List.of(evaluationCase));
        return new EvaluationRetrievalAdapter(
                new EvaluationDatasetRegistry(Map.of("baseline", dataset)),
                new EvaluationSubjectProfileRegistry(Map.of("alice",
                        new EvaluationSubjectProfile("alice", new AgentUserContext(7L, "alice")))),
                retrieval,
                properties);
    }

    private RagProperties properties() {
        RagProperties properties = new RagProperties();
        properties.setRetrievalEnabled(true);
        properties.setTopK(5);
        properties.setCandidateLimit(20);
        properties.setMinimumScore(0.5);
        properties.setEvaluationRecallAtKThreshold(1.0);
        properties.setEvaluationRankingThreshold(1.0);
        properties.setEvaluationCitationCoverageThreshold(1.0);
        properties.setEvaluationNoHitPrecisionThreshold(1.0);
        return properties;
    }

    private Document document(String id, String knowledgeBaseId, String documentId, String versionId, String chunkId) {
        return Document.builder().id(id).text("Refund content.").score(0.95)
                .metadata(Map.of("knowledgeBaseId", knowledgeBaseId, "documentId", documentId,
                        "versionId", versionId, "chunkId", chunkId, "title", "Refund policy",
                        "sourceName", "policy.md", "active", true)).build();
    }

    private String citationKey(String documentId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(documentId.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("cite-");
            for (int index = 0; index < 12; index++) result.append(String.format("%02x", digest[index]));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class RecordingStore implements VectorStore {
        private final List<SearchRequest> requests = new ArrayList<>();
        private final List<Document> documents;
        private final RuntimeException failure;

        private RecordingStore(List<Document> documents) {
            this.documents = documents;
            this.failure = null;
        }

        private RecordingStore(RuntimeException failure) {
            this.documents = List.of();
            this.failure = failure;
        }

        @Override
        public String getName() { return "recording"; }

        @Override
        public void add(List<Document> documents) { }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            requests.add(request);
            if (failure != null) throw failure;
            return documents;
        }

        @Override
        public void delete(List<String> ids) { }

        @Override
        public void delete(Filter.Expression expression) { }
    }
}
