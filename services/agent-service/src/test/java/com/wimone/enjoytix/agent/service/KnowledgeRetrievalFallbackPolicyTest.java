package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRetrievalFallbackPolicyTest {
    @Test
    void noHitRequestsClarificationOrAuthoritativeTicketToolsWithoutInventingKnowledge() {
        String instruction = new KnowledgeRetrievalFallbackPolicy().instructionFor(KnowledgeRetrievalResult.noHit());

        assertThat(instruction).contains("no matching authorized knowledge");
        assertThat(instruction).contains("ticket tools").contains("clarifying question");
        assertThat(instruction).contains("Do not invent");
    }

    @Test
    void failureExplainsKnowledgeUnavailableWithoutLeakingProviderDetails() {
        String instruction = new KnowledgeRetrievalFallbackPolicy().instructionFor(
                KnowledgeRetrievalResult.failure(new RuntimeException("api-key=secret"), "VECTOR_STORE"));

        assertThat(instruction).contains("temporarily unavailable");
        assertThat(instruction).contains("Do not invent");
        assertThat(instruction).doesNotContain("api-key", "secret", "VECTOR_STORE");
    }
}
