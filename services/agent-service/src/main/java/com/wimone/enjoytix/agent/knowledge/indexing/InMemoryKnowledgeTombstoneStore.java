package com.wimone.enjoytix.agent.knowledge.indexing;
import java.util.*;
public final class InMemoryKnowledgeTombstoneStore implements KnowledgeTombstoneStore {
    private final Map<String, KnowledgeIndexTombstone> all = new LinkedHashMap<>();
    private final Set<String> processed = new HashSet<>();
    public synchronized boolean enqueue(KnowledgeIndexTombstone tombstone) { return all.putIfAbsent(tombstone.key(), tombstone) == null; }
    public synchronized List<KnowledgeIndexTombstone> pending() { return all.entrySet().stream().filter(e -> !processed.contains(e.getKey())).map(Map.Entry::getValue).toList(); }
    public synchronized void markProcessed(KnowledgeIndexTombstone tombstone) { processed.add(tombstone.key()); }
    public synchronized List<KnowledgeIndexTombstone> all() { return List.copyOf(all.values()); }
    public synchronized long processedCount() { return processed.size(); }
}
