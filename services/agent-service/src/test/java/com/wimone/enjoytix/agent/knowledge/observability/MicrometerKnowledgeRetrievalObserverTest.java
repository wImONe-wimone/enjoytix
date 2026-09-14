package com.wimone.enjoytix.agent.knowledge.observability;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerKnowledgeRetrievalObserverTest {
    @Test
    void recordsOutcomeCountsAndSafeRetrievalMeasurements() {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        MicrometerKnowledgeRetrievalObserver observer = new MicrometerKnowledgeRetrievalObserver(meters);

        observer.record(new KnowledgeRetrievalObservation(KnowledgeRetrievalOutcome.SUCCESS, 4, 2, 1.0, 37, "conversation-1", "run-1"));
        observer.record(new KnowledgeRetrievalObservation(KnowledgeRetrievalOutcome.NO_HIT, 4, 0, 0.0, 12, "conversation-1", "run-2"));

        assertThat(meters.get("enjoytix.agent.rag.retrieval").tag("outcome", "SUCCESS").counter().count()).isEqualTo(1);
        assertThat(meters.get("enjoytix.agent.rag.retrieval").tag("outcome", "NO_HIT").counter().count()).isEqualTo(1);
        assertThat(meters.get("enjoytix.agent.rag.candidates").tag("outcome", "SUCCESS").summary().count()).isEqualTo(1);
        assertThat(meters.get("enjoytix.agent.rag.results").tag("outcome", "SUCCESS").summary().count()).isEqualTo(1);
        assertThat(meters.get("enjoytix.agent.rag.citation.coverage").tag("outcome", "SUCCESS").summary().count()).isEqualTo(1);
        assertThat(meters.get("enjoytix.agent.rag.latency").tag("outcome", "SUCCESS").timer().count()).isEqualTo(1);
    }
}