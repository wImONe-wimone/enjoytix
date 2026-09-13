package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult;

public final class KnowledgeRetrievalFallbackPolicy {
    public String instructionFor(KnowledgeRetrievalResult result) {
        if (result == null || result.outcome() == KnowledgeRetrievalOutcome.NO_HIT) {
            return "The knowledge base has no matching authorized knowledge. Do not invent an answer. "
                    + "You may use authoritative ticket tools when appropriate or ask a clarifying question.";
        }
        if (result.outcome() == KnowledgeRetrievalOutcome.FAILURE) {
            return "The knowledge base is temporarily unavailable. Do not invent knowledge from it. "
                    + "You may use authoritative ticket tools when appropriate or ask a clarifying question.";
        }
        return "Use only the retrieved authorized knowledge as quoted evidence; do not invent unsupported facts.";
    }
}
