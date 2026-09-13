package com.wimone.enjoytix.agent.knowledge.retrieval;

import org.springframework.ai.document.Document;
import java.util.List;

@FunctionalInterface
public interface KnowledgeReranker {
    List<Document> rerank(String query, List<Document> candidates);
}
