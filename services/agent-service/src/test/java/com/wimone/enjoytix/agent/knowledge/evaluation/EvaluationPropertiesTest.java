package com.wimone.enjoytix.agent.knowledge.evaluation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationPropertiesTest {
    @Test
    void evaluationIsDisabledByDefaultAndDoesNotExposeCredentials() {
        EvaluationProperties properties = new EvaluationProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getTrustedToken()).isEmpty();
        assertThat(properties.getDatasetPath()).isEqualTo("classpath:evaluation/rag-retrieval/datasets/baseline.jsonl");
    }
}
