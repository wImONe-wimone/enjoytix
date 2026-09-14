package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EvaluationDatasetLoader {
    private final ObjectMapper objectMapper;

    public EvaluationDatasetLoader() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public EvaluationDatasetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EvaluationDataset load(Path path, String datasetId) {
        if (path == null) throw new IllegalArgumentException("dataset path is required");
        try {
            return load(new InputStreamReader(Files.newInputStream(path), java.nio.charset.StandardCharsets.UTF_8), datasetId, null);
        } catch (IOException failure) {
            throw new IllegalArgumentException("unable to read evaluation dataset", failure);
        }
    }

    public EvaluationDataset load(Path path, String datasetId, EvaluationSubjectProfileRegistry subjectProfiles) {
        if (path == null) throw new IllegalArgumentException("dataset path is required");
        if (subjectProfiles == null) throw new IllegalArgumentException("evaluation subject profiles are required");
        try {
            return load(new InputStreamReader(Files.newInputStream(path), java.nio.charset.StandardCharsets.UTF_8), datasetId, subjectProfiles);
        } catch (IOException failure) {
            throw new IllegalArgumentException("unable to read evaluation dataset", failure);
        }
    }

    public EvaluationDataset load(Resource resource, String datasetId) {
        if (resource == null) throw new IllegalArgumentException("dataset resource is required");
        try {
            return load(new InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8), datasetId, null);
        } catch (IOException failure) {
            throw new IllegalArgumentException("unable to read evaluation dataset", failure);
        }
    }

    public EvaluationDataset load(Resource resource, String datasetId, EvaluationSubjectProfileRegistry subjectProfiles) {
        if (resource == null) throw new IllegalArgumentException("dataset resource is required");
        if (subjectProfiles == null) throw new IllegalArgumentException("evaluation subject profiles are required");
        try {
            return load(new InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8), datasetId, subjectProfiles);
        } catch (IOException failure) {
            throw new IllegalArgumentException("unable to read evaluation dataset", failure);
        }
    }

    private EvaluationDataset load(java.io.Reader source, String datasetId, EvaluationSubjectProfileRegistry subjectProfiles) {
        if (datasetId == null || datasetId.isBlank()) throw new IllegalArgumentException("dataset id is required");
        List<RetrievalEvaluationCase> cases = new ArrayList<>();
        Set<String> caseIds = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(source)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                RetrievalEvaluationCase item;
                try {
                    item = objectMapper.readValue(line, RetrievalEvaluationCase.class);
                } catch (IOException | RuntimeException failure) {
                    throw new IllegalArgumentException("invalid dataset JSON at line " + lineNumber, failure);
                }
                if (!caseIds.add(item.caseId())) {
                    throw new IllegalArgumentException("duplicate caseId: " + item.caseId());
                }
                if (subjectProfiles != null) {
                    subjectProfiles.resolve(item.subjectProfileId());
                }
                cases.add(item);
            }
        } catch (IOException failure) {
            throw new IllegalArgumentException("unable to read evaluation dataset", failure);
        }
        if (cases.isEmpty()) throw new IllegalArgumentException("evaluation dataset is empty");
        RetrievalEvaluationCase first = cases.get(0);
        return new EvaluationDataset(datasetId, first.schemaVersion(), first.datasetVersion(), cases);
    }
}
