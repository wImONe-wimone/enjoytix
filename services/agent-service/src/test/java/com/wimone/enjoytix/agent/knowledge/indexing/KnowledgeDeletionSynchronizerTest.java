package com.wimone.enjoytix.agent.knowledge.indexing;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeDeletionSynchronizerTest {
    @Test
    void deletesDocumentVersionChunkAndFingerprintTargetsImmediatelyAndDeduplicatesRetries() {
        RecordingVectorStore vectors = new RecordingVectorStore();
        InMemoryKnowledgeTombstoneStore tombstones = new InMemoryKnowledgeTombstoneStore();
        KnowledgeDeletionSynchronizer synchronizer = new KnowledgeDeletionSynchronizer(vectors, tombstones);
        KnowledgeIndexDocument document = index("idx-1", "doc-1", "v2", "chunk-1", "fp-1");

        synchronizer.syncDocument(List.of(document));
        synchronizer.syncVersion(List.of(document));
        synchronizer.syncChunk(List.of(document));
        synchronizer.syncFingerprint(List.of(document));
        synchronizer.syncChunk(List.of(document));

        assertThat(vectors.deletedIds()).containsExactly("idx-1", "idx-1", "idx-1", "idx-1");
        assertThat(vectors.visibleIds()).doesNotContain("idx-1");
        assertThat(tombstones.pending()).isEmpty();
        assertThat(tombstones.all()).hasSize(4);
        assertThat(tombstones.processedCount()).isEqualTo(4);
    }

    @Test
    void failedDeletionRemainsPendingAndRetryIsIdempotent() {
        RecordingVectorStore vectors = new RecordingVectorStore();
        vectors.failOnce = true;
        InMemoryKnowledgeTombstoneStore tombstones = new InMemoryKnowledgeTombstoneStore();
        KnowledgeDeletionSynchronizer synchronizer = new KnowledgeDeletionSynchronizer(vectors, tombstones);
        KnowledgeIndexDocument document = index("idx-2", "doc-1", "v2", "chunk-2", "fp-2");

        synchronizer.syncChunk(List.of(document));

        assertThat(tombstones.pending()).hasSize(1);
        synchronizer.retryPending();
        assertThat(tombstones.pending()).isEmpty();
        assertThat(tombstones.processedCount()).isEqualTo(1);
    }

    private KnowledgeIndexDocument index(String id, String doc, String version, String chunk, String fingerprint) {
        return new KnowledgeIndexDocument(id, "content", Map.of("knowledgeBaseId", "kb-1"), "kb-1", doc,
                doc + ":" + version, chunk, fingerprint, "version-fp", "generation", "embedding");
    }

    static final class RecordingVectorStore implements VectorStore {
        final List<String> deletedIds = new ArrayList<>();
        final Set<String> visibleIds = new HashSet<>(Set.of("idx-1", "idx-2"));
        boolean failOnce;
        @Override public String getName() { return "test"; }
        @Override public void add(List<Document> documents) { visibleIds.addAll(documents.stream().map(Document::getId).toList()); }
        @Override public List<Document> similaritySearch(SearchRequest request) { return List.of(); }
        @Override public void delete(List<String> ids) { if (failOnce) { failOnce = false; throw new IllegalStateException("delete failed"); } deletedIds.addAll(ids); visibleIds.removeAll(ids); }
        @Override public void delete(Filter.Expression expression) { }
        List<String> deletedIds() { return deletedIds; }
        Set<String> visibleIds() { return visibleIds; }
    }
}
