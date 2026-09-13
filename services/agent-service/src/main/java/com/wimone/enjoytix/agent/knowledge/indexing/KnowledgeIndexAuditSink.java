package com.wimone.enjoytix.agent.knowledge.indexing;
@FunctionalInterface
public interface KnowledgeIndexAuditSink { void record(KnowledgeIndexAuditEvent event); }
