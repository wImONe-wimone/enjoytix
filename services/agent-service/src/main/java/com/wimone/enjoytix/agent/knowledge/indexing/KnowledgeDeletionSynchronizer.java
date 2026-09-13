package com.wimone.enjoytix.agent.knowledge.indexing;
import com.wimone.enjoytix.agent.knowledge.domain.*;
import org.springframework.ai.vectorstore.VectorStore;
import java.util.*;
public final class KnowledgeDeletionSynchronizer {
    private final VectorStore vectorStore; private final KnowledgeTombstoneStore tombstones;
    public KnowledgeDeletionSynchronizer(VectorStore vectorStore, KnowledgeTombstoneStore tombstones) { this.vectorStore=Objects.requireNonNull(vectorStore); this.tombstones=Objects.requireNonNull(tombstones); }
    public void syncDocument(List<KnowledgeIndexDocument> documents) { sync(documents, TombstoneReason.DOCUMENT); }
    public void syncVersion(List<KnowledgeIndexDocument> documents) { sync(documents, TombstoneReason.VERSION); }
    public void syncChunk(List<KnowledgeIndexDocument> documents) { sync(documents, TombstoneReason.CHUNK); }
    public void syncFingerprint(List<KnowledgeIndexDocument> documents) { sync(documents, TombstoneReason.FINGERPRINT); }
    public void retryPending() { for (KnowledgeIndexTombstone tombstone : tombstones.pending()) process(tombstone); }
    private void sync(List<KnowledgeIndexDocument> documents, TombstoneReason reason) { if (documents == null) return; for (KnowledgeIndexDocument document : documents) { KnowledgeIndexTombstone tombstone = new KnowledgeIndexTombstone(document.knowledgeBaseId(), document.documentId(), document.versionId(), document.chunkId(), document.contentFingerprint(), reason, document.id()); if (tombstones.enqueue(tombstone)) process(tombstone); } }
    private void process(KnowledgeIndexTombstone tombstone) { try { vectorStore.delete(List.of(tombstone.vectorId())); tombstones.markProcessed(tombstone); } catch (RuntimeException ignored) { } }
}
