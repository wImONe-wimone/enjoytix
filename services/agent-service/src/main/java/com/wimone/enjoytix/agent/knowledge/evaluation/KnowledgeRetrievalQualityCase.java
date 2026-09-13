package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult;
import java.util.Set;

public record KnowledgeRetrievalQualityCase(String queryId, Set<String> expectedCitationKeys, KnowledgeRetrievalResult result) {
    public KnowledgeRetrievalQualityCase {
        if (queryId == null || queryId.isBlank() || expectedCitationKeys == null || result == null) throw new IllegalArgumentException("quality case fields are required");
        expectedCitationKeys = Set.copyOf(expectedCitationKeys);
    }
}
