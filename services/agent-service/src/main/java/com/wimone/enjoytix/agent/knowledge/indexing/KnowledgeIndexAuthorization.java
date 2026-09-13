package com.wimone.enjoytix.agent.knowledge.indexing;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
@FunctionalInterface
public interface KnowledgeIndexAuthorization {
    boolean canIndex(AgentUserContext user, String knowledgeBaseId, String documentId);
}
