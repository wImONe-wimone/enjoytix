package com.wimone.enjoytix.agent.knowledge.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagPropertiesTest {

    @Test
    void defaultsKeepRagDisabledAndUseSafeBounds() {
        RagProperties properties = new RagProperties();

        assertThat(properties.isIndexingEnabled()).isFalse();
        assertThat(properties.isRetrievalEnabled()).isFalse();
        assertThat(properties.isRerankingEnabled()).isFalse();
        assertThat(properties.isCitationsEnabled()).isFalse();
        assertThat(properties.getTopK()).isEqualTo(5);
        assertThat(properties.getCandidateLimit()).isEqualTo(20);
        assertThat(properties.getMinimumScore()).isEqualTo(0.65);
    }
}
