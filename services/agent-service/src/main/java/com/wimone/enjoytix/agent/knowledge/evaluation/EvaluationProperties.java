package com.wimone.enjoytix.agent.knowledge.evaluation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "agent.knowledge.evaluation")
public class EvaluationProperties {
    private boolean enabled;
    private String datasetPath = "classpath:evaluation/rag-retrieval/datasets/baseline.jsonl";
    private String trustedToken = "";
    private Map<String, SubjectProfileProperties> subjectProfiles = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDatasetPath() {
        return datasetPath;
    }

    public void setDatasetPath(String datasetPath) {
        if (datasetPath == null || datasetPath.isBlank()) {
            throw new IllegalArgumentException("evaluation dataset path is required");
        }
        this.datasetPath = datasetPath;
    }

    public String getTrustedToken() {
        return trustedToken;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken == null ? "" : trustedToken;
    }

    public Map<String, SubjectProfileProperties> getSubjectProfiles() {
        return subjectProfiles;
    }

    public void setSubjectProfiles(Map<String, SubjectProfileProperties> subjectProfiles) {
        this.subjectProfiles = subjectProfiles == null ? new LinkedHashMap<>() : new LinkedHashMap<>(subjectProfiles);
    }

    public static class SubjectProfileProperties {
        private Long userId;
        private String username;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
