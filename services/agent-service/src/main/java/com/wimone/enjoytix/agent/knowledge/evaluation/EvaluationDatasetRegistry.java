package com.wimone.enjoytix.agent.knowledge.evaluation;

import java.util.Map;

public final class EvaluationDatasetRegistry {
    private final Map<String, EvaluationDataset> datasets;

    public EvaluationDatasetRegistry(Map<String, EvaluationDataset> datasets) {
        if (datasets == null || datasets.isEmpty()) throw new IllegalArgumentException("evaluation datasets are required");
        this.datasets = Map.copyOf(datasets);
    }

    public EvaluationDataset resolve(String datasetId) {
        EvaluationDataset dataset = datasets.get(datasetId);
        if (dataset == null) throw new IllegalArgumentException("unknown dataset: " + datasetId);
        return dataset;
    }
}
