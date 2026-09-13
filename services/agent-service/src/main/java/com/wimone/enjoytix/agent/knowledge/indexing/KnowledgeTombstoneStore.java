package com.wimone.enjoytix.agent.knowledge.indexing;
import java.util.*;
public interface KnowledgeTombstoneStore {
    boolean enqueue(KnowledgeIndexTombstone tombstone);
    List<KnowledgeIndexTombstone> pending();
    void markProcessed(KnowledgeIndexTombstone tombstone);
}
