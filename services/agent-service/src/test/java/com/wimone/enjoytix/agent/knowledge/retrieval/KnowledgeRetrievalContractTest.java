package com.wimone.enjoytix.agent.knowledge.retrieval;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeRetrievalContractTest {

    @Test
    void requestRequiresAuthenticatedScopeAndBoundedQuery() {
        assertThatThrownBy(() -> new KnowledgeRetrievalRequest(null, "policy", 5, 0.2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("authenticated user is required");
        assertThatThrownBy(() -> new KnowledgeRetrievalRequest(new AgentUserContext(7L, "alice"), " ", 5, 0.2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query is required");
        assertThatThrownBy(() -> new KnowledgeRetrievalRequest(new AgentUserContext(7L, "alice"), "policy", 0, 0.2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topK must be positive and at most 50");
    }

    @Test
    void citationAndResultAreImmutableAndExposeOnlySafeProvenance() {
        KnowledgeCitation citation = new KnowledgeCitation(
                "cite-1", "kb-1", "doc-1", "v2", "chunk-3", "Refund policy", "page 2", 0.91);
        KnowledgeRetrievalResult result = KnowledgeRetrievalResult.success(List.of(citation), Map.of("attempted", "true"));

        assertThat(result.outcome()).isEqualTo(KnowledgeRetrievalOutcome.SUCCESS);
        assertThat(result.citations()).containsExactly(citation);
        assertThat(result.metadata()).containsEntry("attempted", "true");
        assertThatThrownBy(() -> result.citations().add(citation)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void supportsNoHitAndFailureWithoutRawProviderMessage() {
        assertThat(KnowledgeRetrievalResult.noHit().outcome()).isEqualTo(KnowledgeRetrievalOutcome.NO_HIT);
        KnowledgeRetrievalResult failure = KnowledgeRetrievalResult.failure(
                new RuntimeException("api-key=secret-value"));
        assertThat(failure.outcome()).isEqualTo(KnowledgeRetrievalOutcome.FAILURE);
        assertThat(failure.safeMessage()).isEqualTo("Knowledge retrieval is temporarily unavailable.");
        assertThat(failure.safeMessage()).doesNotContain("secret-value");
    }
}
