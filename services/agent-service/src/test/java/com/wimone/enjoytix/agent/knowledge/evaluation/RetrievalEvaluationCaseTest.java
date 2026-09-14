package com.wimone.enjoytix.agent.knowledge.evaluation;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetrievalEvaluationCaseTest {
    @Test
    void successfulCaseRequiresReferenceAnswer() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                new RetrievalEvaluationCase("1", "baseline-1", "positive", "refund policy", "public-user",
                        EvaluationExpectedOutcome.SUCCESS, Set.of("cite-1"), "", Set.of("positive"));
            }
        });
    }

    @Test
    void noHitCaseCannotDeclareExpectedCitations() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                new RetrievalEvaluationCase("1", "baseline-1", "no-hit", "unknown policy", "public-user",
                        EvaluationExpectedOutcome.NO_HIT, Set.of("cite-1"), null, Set.of("no-hit"));
            }
        });
    }

    @Test
    void caseRequiresSchemaVersionAndStableIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new RetrievalEvaluationCase("", "baseline-1", "case-1",
                "refund policy", "public-user", EvaluationExpectedOutcome.SUCCESS, Set.of("cite-1"),
                "Refunds are available.", Set.of("positive")));
        assertThrows(IllegalArgumentException.class, () -> new RetrievalEvaluationCase("1", "baseline-1", "",
                "refund policy", "public-user", EvaluationExpectedOutcome.SUCCESS, Set.of("cite-1"),
                "Refunds are available.", Set.of("positive")));
    }
}
