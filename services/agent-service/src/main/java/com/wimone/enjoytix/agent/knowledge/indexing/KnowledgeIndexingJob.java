package com.wimone.enjoytix.agent.knowledge.indexing;

import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeChunk;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeDocument;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeDocumentVersion;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class KnowledgeIndexingJob {
    private final VectorStore vectorStore;
    private final IndexGenerationStore generations;
    private final int batchSize;
    private final int maxRetries;
    private final Map<String, Object> documentLocks = new ConcurrentHashMap<>();

    public KnowledgeIndexingJob(VectorStore vectorStore, IndexGenerationStore generations,
                                int batchSize, int maxRetries) {
        if (batchSize <= 0 || maxRetries <= 0) {
            throw new IllegalArgumentException("batch size and retries must be positive");
        }
        this.vectorStore = Objects.requireNonNull(vectorStore);
        this.generations = Objects.requireNonNull(generations);
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    public IndexingProgress upsert(KnowledgeDocument document, KnowledgeDocumentVersion version,
                                   List<KnowledgeChunk> chunks, String embeddingModel) {
        return rebuild(document, version, chunks, embeddingModel);
    }

    public IndexingProgress rebuild(KnowledgeDocument document, KnowledgeDocumentVersion version,
                                    List<KnowledgeChunk> chunks, String embeddingModel) {
        Objects.requireNonNull(document, "document is required");
        Object lock = documentLocks.computeIfAbsent(document.id(), ignored -> new Object());
        synchronized (lock) {
            return rebuildLocked(document, version, chunks, embeddingModel);
        }
    }

    private IndexingProgress rebuildLocked(KnowledgeDocument document, KnowledgeDocumentVersion version,
                                           List<KnowledgeChunk> chunks, String embeddingModel) {
        List<KnowledgeIndexDocument> indexed = new KnowledgeIndexDocumentMapper()
                .map(document, version, chunks, embeddingModel);
        String generationId = indexed.isEmpty() ? "gen-empty" : indexed.get(0).generationId();
        generations.create(generationId, document.knowledgeBaseId(), document.id(), versionId(version),
                indexed.isEmpty() ? "" : indexed.get(0).versionFingerprint(), indexed.size());
        int attempts = 0;
        int batches = 0;
        int embedded = 0;
        try {
            for (int start = 0; start < indexed.size(); start += batchSize) {
                List<Document> batch = indexed.subList(start, Math.min(start + batchSize, indexed.size()))
                        .stream().map(KnowledgeIndexDocument::toVectorDocument).toList();
                boolean done = false;
                for (int retry = 0; retry < maxRetries && !done; retry++) {
                    attempts++;
                    try {
                        vectorStore.add(batch);
                        done = true;
                    } catch (RuntimeException failure) {
                        if (retry + 1 == maxRetries) {
                            throw failure;
                        }
                    }
                }
                batches++;
                embedded += batch.size();
            }
            generations.markReady(generationId, embedded);
            generations.activate(generationId);
            return new IndexingProgress(generationId, IndexingStatus.ACTIVE, indexed.size(), embedded,
                    attempts, batches, true, null);
        } catch (RuntimeException failure) {
            generations.markFailed(generationId, safeMessage(failure));
            return IndexingProgress.failed(generationId, indexed.size(), attempts, batches,
                    safeMessage(failure));
        }
    }

    private String versionId(KnowledgeDocumentVersion version) {
        return version.documentId() + ":v" + version.version();
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}