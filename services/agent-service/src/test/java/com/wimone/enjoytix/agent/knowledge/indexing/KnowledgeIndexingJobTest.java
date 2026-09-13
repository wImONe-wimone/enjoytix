package com.wimone.enjoytix.agent.knowledge.indexing;

import com.wimone.enjoytix.agent.knowledge.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIndexingJobTest {
    @Test
    void rebuildsInBatchesRetriesFailuresAndActivatesOnlyAfterReady() {
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        InMemoryIndexGenerationStore generations = new InMemoryIndexGenerationStore();
        KnowledgeIndexingJob job = new KnowledgeIndexingJob(vectorStore, generations, 2, 2);
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Policy", true);

        IndexingProgress progress = job.rebuild(document, version(), List.of(
                chunk("c1", "one", "h1"), chunk("c2", "two", "h2"), chunk("c3", "three", "h3")), "text-embedding-v3");

        assertThat(progress.status()).isEqualTo(IndexingStatus.ACTIVE);
        assertThat(progress.totalChunks()).isEqualTo(3);
        assertThat(progress.embeddedChunks()).isEqualTo(3);
        assertThat(progress.attempts()).isEqualTo(3);
        assertThat(progress.batches()).isEqualTo(2);
        assertThat(progress.activated()).isTrue();
        assertThat(generations.activeGenerationId()).isEqualTo(progress.generationId());
        assertThat(vectorStore.batchSizes()).containsExactly(2, 2, 1);
    }

    @Test
    void failedBatchLeavesGenerationFailedAndDoesNotActivatePartialResults() {
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        vectorStore.alwaysFail = true;
        InMemoryIndexGenerationStore generations = new InMemoryIndexGenerationStore();
        KnowledgeIndexingJob job = new KnowledgeIndexingJob(vectorStore, generations, 2, 1);

        IndexingProgress progress = job.rebuild(new KnowledgeDocument("doc-1", "kb-1", "Policy", true), version(),
                List.of(chunk("c1", "one", "h1"), chunk("c2", "two", "h2")), "text-embedding-v3");

        assertThat(progress.status()).isEqualTo(IndexingStatus.FAILED);
        assertThat(progress.embeddedChunks()).isZero();
        assertThat(progress.activated()).isFalse();
        assertThat(generations.activeGenerationId()).isNull();
    }

    @Test
    void serializesConcurrentRebuildsForTheSameDocument() throws Exception {
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        InMemoryIndexGenerationStore generations = new InMemoryIndexGenerationStore();
        KnowledgeIndexingJob job = new KnowledgeIndexingJob(vectorStore, generations, 1, 2);
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Policy", true);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> job.rebuild(document, version(),
                    List.of(chunk("c1", "one", "h1")), "text-embedding-v3"));
            var second = executor.submit(() -> job.rebuild(document, version(),
                    List.of(chunk("c2", "two", "h2")), "text-embedding-v3"));

            assertThat(first.get().status()).isEqualTo(IndexingStatus.ACTIVE);
            assertThat(second.get().status()).isEqualTo(IndexingStatus.ACTIVE);
            assertThat(generations.activeGenerationId()).isNotNull();
        } finally {
            executor.shutdownNow();
        }
    }
    private KnowledgeDocumentVersion version() {
        KnowledgeDocumentVersion version = KnowledgeDocumentVersion.pending("doc-1", 2, "checksum-v2", "object-key",
                Instant.parse("2026-09-13T00:00:00Z"));
        version.startProcessing();
        version.markSuccessful(3);
        return version;
    }

    private KnowledgeChunk chunk(String id, String content, String hash) {
        return new KnowledgeChunk(id, "doc-1:v2", 0, content, hash, content.length(), 1, Map.of(),
                new KnowledgeProvenance("policy.md", null, null));
    }

    static final class RecordingVectorStore implements VectorStore {
        private final List<Integer> batchSizes = new ArrayList<>();
        private boolean failOnce = true;
        private boolean alwaysFail;
        @Override public String getName() { return "test"; }
        @Override public void add(List<Document> documents) {
            batchSizes.add(documents.size());
            if (alwaysFail || failOnce && batchSizes.size() == 1) {
                failOnce = false;
                throw new IllegalStateException("temporary embedding failure");
            }
        }
        @Override public List<Document> similaritySearch(SearchRequest request) { return List.of(); }
        @Override public void delete(List<String> ids) { }
        @Override public void delete(Filter.Expression expression) { }
        List<Integer> batchSizes() { return batchSizes; }
    }
}
