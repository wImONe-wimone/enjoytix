package com.wimone.enjoytix.agent.knowledge.service;

import com.wimone.enjoytix.agent.knowledge.chunk.TextChunker;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeDocument;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeDocumentVersion;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeSource;
import com.wimone.enjoytix.agent.knowledge.domain.ProcessMode;
import com.wimone.enjoytix.agent.knowledge.parser.DocumentParser;
import com.wimone.enjoytix.agent.knowledge.port.KnowledgeRepository;
import com.wimone.enjoytix.agent.knowledge.source.DocumentFetcher;
import com.wimone.enjoytix.agent.knowledge.storage.KnowledgeObjectStorage;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.time.Instant;

@Service
@ConditionalOnBean({KnowledgeObjectStorage.class, KnowledgeRepository.class})
public class KnowledgeIngestionService {
    private final DocumentFetcher uploadFetcher;
    private final DocumentFetcher httpFetcher;
    private final KnowledgeObjectStorage objectStorage;
    private final DocumentParser parser;
    private final TextChunker chunker;
    private final KnowledgeRepository repository;

    public KnowledgeIngestionService(@Qualifier("uploadDocumentFetcher") DocumentFetcher uploadFetcher,
                                     @Qualifier("httpDocumentFetcher") DocumentFetcher httpFetcher,
                                     KnowledgeObjectStorage objectStorage, DocumentParser parser,
                                     TextChunker chunker, KnowledgeRepository repository) {
        this.uploadFetcher = uploadFetcher;
        this.httpFetcher = httpFetcher;
        this.objectStorage = objectStorage;
        this.parser = parser;
        this.chunker = chunker;
        this.repository = repository;
    }

    public IngestionResult ingest(KnowledgeDocument document, int versionNumber,
                                  DocumentFetcher.DocumentFetchRequest request, ProcessMode processMode) {
        if (document == null || request == null || processMode == null) throw new IllegalArgumentException("ingestion arguments are required");
        DocumentFetcher.FetchResult fetched = fetcher(request).fetch(request);
        String key = objectStorage.objectKey(document.knowledgeBaseId(), document.id(), versionNumber, fetched.checksum());
        KnowledgeSource source = new KnowledgeSource(request.type(), request.type().code() + ":" + (request.location() == null ? fetched.fileName() : request.location()),
                fetched.fileName(), fetched.contentType(), fetched.contentLength(), fetched.checksum(), key);
        KnowledgeDocumentVersion version = KnowledgeDocumentVersion.pending(document.id(), versionNumber, fetched.checksum(), key, Instant.now());
        KnowledgeRepository.VersionSaveResult saved = repository.saveVersion(document, version, source, processMode);
        if (!saved.created()) return new IngestionResult(saved.versionId(), fetched.checksum(), 0, false);
        repository.markProcessing(saved.versionId());
        version.startProcessing();
        try {
            if (!objectStorage.exists(key)) objectStorage.put(key, fetched);
            var parsed = parser.parse(fetched.content(), fetched.contentType(), fetched.fileName());
            var chunks = chunker.chunk(saved.versionId(), fetched.fileName(), parsed);
            if (chunks.isEmpty()) throw new IllegalArgumentException("document produced no chunks");
            repository.replaceChunks(saved.versionId(), chunks);
            version.markSuccessful(chunks.size());
            repository.markSuccessful(saved.versionId(), chunks.size());
            version.publish();
            repository.publish(version);
            return new IngestionResult(saved.versionId(), fetched.checksum(), chunks.size(), true);
        } catch (RuntimeException exception) {
            if (version.status() == com.wimone.enjoytix.agent.knowledge.domain.DocumentStatus.RUNNING) version.markFailed(message(exception));
            repository.markFailed(saved.versionId(), message(exception));
            throw exception;
        }
    }

    private DocumentFetcher fetcher(DocumentFetcher.DocumentFetchRequest request) {
        return request.type() == com.wimone.enjoytix.agent.knowledge.domain.SourceType.UPLOAD ? uploadFetcher : httpFetcher;
    }

    private String message(RuntimeException exception) { return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(); }

    public record IngestionResult(String versionId, String checksum, int chunkCount, boolean created) {}
}
