package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeCitation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentChatResultCompatibilityTest {
    @Test
    void legacyConstructorKeepsOptionalRagFieldsEmpty() {
        AgentChatResult result = new AgentChatResult("answer", List.of("lookup"));

        assertThat(result.citations()).isEmpty();
        assertThat(result.ragMetadata()).isEmpty();
    }

    @Test
    void ragFieldsAreImmutableAndAdditive() {
        KnowledgeCitation citation = new KnowledgeCitation(
                "cite-a1", "kb-1", "doc-1", "v1", "chunk-1", "Policy", "policy.md", 0.9);
        AgentChatResult result = AgentChatResult.withRag("answer", null, null, List.of(), null,
                List.of(citation), Map.of("outcome", "SUCCESS"));

        assertThat(result.citations()).containsExactly(citation);
        assertThat(result.ragMetadata()).containsEntry("outcome", "SUCCESS");
        assertThat(result.citations()).isUnmodifiable();
        assertThat(result.ragMetadata()).isUnmodifiable();
    }
}
