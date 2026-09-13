package com.wimone.enjoytix.agent.knowledge.indexing;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public final class InMemoryIndexGenerationStore implements IndexGenerationStore {
    private final Map<String, IndexingStatus> statuses = new ConcurrentHashMap<>();
    private volatile String activeGenerationId;
    public void create(String id, String kb, String doc, String version, String fingerprint, int total) { statuses.put(id, IndexingStatus.RUNNING); }
    public void markReady(String id, int embedded) { statuses.replace(id, IndexingStatus.READY); }
    public void activate(String id) { if (statuses.get(id) != IndexingStatus.READY) throw new IllegalStateException("generation is not ready"); statuses.put(id, IndexingStatus.ACTIVE); activeGenerationId = id; }
    public void markFailed(String id, String message) { statuses.put(id, IndexingStatus.FAILED); }
    public String activeGenerationId() { return activeGenerationId; }
    public IndexingStatus status(String id) { return statuses.get(id); }
}
