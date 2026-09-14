package com.wimone.enjoytix.agent.knowledge.rollout;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RagFallbackReasonsTest {
    @Test void classifiesNoHitAndFailureWithoutLeakingDetails() {
        assertThat(RagFallbackReasons.forRetrieval(KnowledgeRetrievalResult.noHit())).isEqualTo(RagFallbackReasons.NO_HIT);
        assertThat(RagFallbackReasons.forRetrieval(KnowledgeRetrievalResult.failure(new RuntimeException("secret"), "VECTOR_STORE"))).isEqualTo(RagFallbackReasons.RETRIEVAL_FAILURE);
        assertThat(RagFallbackReasons.forRetrieval(null)).isEqualTo(RagFallbackReasons.NO_HIT);
    }
}