package com.wimone.enjoytix.agent.knowledge.retrieval;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import java.util.Set;

@FunctionalInterface
public interface KnowledgeRetrievalAuthorization {
    Set<String> authorizedKnowledgeBases(AgentUserContext user);
}
