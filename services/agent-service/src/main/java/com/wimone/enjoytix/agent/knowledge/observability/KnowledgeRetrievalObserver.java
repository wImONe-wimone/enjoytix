package com.wimone.enjoytix.agent.knowledge.observability;
@FunctionalInterface public interface KnowledgeRetrievalObserver { void record(KnowledgeRetrievalObservation observation); }