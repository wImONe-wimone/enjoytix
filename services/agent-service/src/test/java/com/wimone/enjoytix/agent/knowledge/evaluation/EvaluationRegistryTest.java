package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class EvaluationRegistryTest {
    @Test
    void resolvesOnlyRegisteredDatasetAndSubjectIds() {
        RetrievalEvaluationCase item = new RetrievalEvaluationCase("1", "baseline-1", "case-1", "refund", "public-user",
                EvaluationExpectedOutcome.NO_HIT, java.util.Set.of(), null, java.util.Set.of("no-hit"));
        EvaluationDataset dataset = new EvaluationDataset("baseline", "1", "baseline-1", java.util.List.of(item));
        EvaluationSubjectProfile profile = new EvaluationSubjectProfile("public-user", new AgentUserContext(7L, "alice"));

        assertThat(new EvaluationDatasetRegistry(Map.of("baseline", dataset)).resolve("baseline")).isSameAs(dataset);
        assertThat(new EvaluationSubjectProfileRegistry(Map.of("public-user", profile)).resolve("public-user")).isSameAs(profile);
        assertThatIllegalArgumentException().isThrownBy(() -> new EvaluationDatasetRegistry(Map.of("baseline", dataset)).resolve("other"));
        assertThatIllegalArgumentException().isThrownBy(() -> new EvaluationSubjectProfileRegistry(Map.of("public-user", profile)).resolve("other"));
    }
}
