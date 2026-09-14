package com.wimone.enjoytix.agent.knowledge.rollout;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult;

public final class RagFallbackReasons {
    public static final String NO_HIT = "NO_KNOWLEDGE_MATCH";
    public static final String RETRIEVAL_FAILURE = "RAG_RETRIEVAL_FAILURE";
    public static final String CONTEXT_ASSEMBLY_FAILURE = "RAG_CONTEXT_ASSEMBLY_FAILURE";
    private RagFallbackReasons() { }
    public static String forRetrieval(KnowledgeRetrievalResult result) {
        if (result == null || result.outcome() == KnowledgeRetrievalOutcome.NO_HIT) return NO_HIT;
        if (result.outcome() == KnowledgeRetrievalOutcome.FAILURE) return RETRIEVAL_FAILURE;
        return null;
    }
}