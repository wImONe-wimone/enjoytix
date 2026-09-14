package com.wimone.enjoytix.agent.knowledge.observability;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class MicrometerKnowledgeRetrievalObserver implements KnowledgeRetrievalObserver {
    private static final String OUTCOME_TAG = "outcome";

    private final MeterRegistry meters;

    public MicrometerKnowledgeRetrievalObserver(MeterRegistry meters) {
        this.meters = Objects.requireNonNull(meters, "meter registry is required");
    }

    @Override
    public void record(KnowledgeRetrievalObservation observation) {
        Objects.requireNonNull(observation, "observation is required");
        String outcome = observation.outcome().name();
        meters.counter("enjoytix.agent.rag.retrieval", OUTCOME_TAG, outcome).increment();
        meters.summary("enjoytix.agent.rag.candidates", OUTCOME_TAG, outcome)
                .record(observation.candidateCount());
        meters.summary("enjoytix.agent.rag.results", OUTCOME_TAG, outcome)
                .record(observation.finalCount());
        meters.summary("enjoytix.agent.rag.citation.coverage", OUTCOME_TAG, outcome)
                .record(observation.citationCoverage());
        meters.timer("enjoytix.agent.rag.latency", OUTCOME_TAG, outcome)
                .record(observation.latencyMillis(), TimeUnit.MILLISECONDS);
    }
}