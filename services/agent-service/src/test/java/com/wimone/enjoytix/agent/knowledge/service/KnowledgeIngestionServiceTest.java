package com.wimone.enjoytix.agent.knowledge.service;

import com.wimone.enjoytix.agent.knowledge.chunk.TextChunker;
import com.wimone.enjoytix.agent.knowledge.domain.*;
import com.wimone.enjoytix.agent.knowledge.parser.DocumentParser;
import com.wimone.enjoytix.agent.knowledge.parser.ParsedDocument;
import com.wimone.enjoytix.agent.knowledge.port.KnowledgeRepository;
import com.wimone.enjoytix.agent.knowledge.source.DocumentFetcher;
import com.wimone.enjoytix.agent.knowledge.storage.KnowledgeObjectStorage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KnowledgeIngestionServiceTest {
    private final DocumentFetcher uploadFetcher = mock(DocumentFetcher.class);
    private final DocumentFetcher httpFetcher = mock(DocumentFetcher.class);
    private final KnowledgeObjectStorage storage = mock(KnowledgeObjectStorage.class);
    private final DocumentParser parser = mock(DocumentParser.class);
    private final TextChunker chunker = mock(TextChunker.class);
    private final KnowledgeRepository repository = mock(KnowledgeRepository.class);
    private final KnowledgeIngestionService service = new KnowledgeIngestionService(uploadFetcher, httpFetcher, storage, parser, chunker, repository);

    @Test
    void ingestsStoresChunksAndPublishesSuccessfulVersion() {
        DocumentFetcher.DocumentFetchRequest request = DocumentFetcher.DocumentFetchRequest.upload("policy.txt", "text/plain", new ByteArrayInputStream(new byte[]{1}));
        DocumentFetcher.FetchResult fetched = new DocumentFetcher.FetchResult("policy.txt", "text/plain", 1, "checksum-1", new byte[]{1});
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Policy", true);
        KnowledgeChunk chunk = new KnowledgeChunk("doc-1:v2:0", "doc-1:v2", 0, "policy", "hash", 6, 1, Map.of(), new KnowledgeProvenance("policy.txt", null, null));
        when(uploadFetcher.fetch(request)).thenReturn(fetched);
        when(storage.objectKey("kb-1", "doc-1", 2, "checksum-1")).thenReturn("knowledge/doc-1/v2/checksum-1");
        when(repository.saveVersion(any(), any(), any(), eq(ProcessMode.CHUNK))).thenReturn(new KnowledgeRepository.VersionSaveResult("doc-1:v2", true));
        when(parser.parse(fetched.content(), fetched.contentType(), fetched.fileName())).thenReturn(new ParsedDocument("policy", Map.of()));
        when(chunker.chunk(eq("doc-1:v2"), eq("policy.txt"), any())).thenReturn(List.of(chunk));

        KnowledgeIngestionService.IngestionResult result = service.ingest(document, 2, request, ProcessMode.CHUNK);

        assertThat(result).isEqualTo(new KnowledgeIngestionService.IngestionResult("doc-1:v2", "checksum-1", 1, true));
        verify(storage).put("knowledge/doc-1/v2/checksum-1", fetched);
        verify(repository).replaceChunks("doc-1:v2", List.of(chunk));
        verify(repository).markSuccessful("doc-1:v2", 1);
        verify(repository).publish(any(KnowledgeDocumentVersion.class));
    }

    @Test
    void duplicateChecksumSkipsProcessing() {
        DocumentFetcher.DocumentFetchRequest request = DocumentFetcher.DocumentFetchRequest.upload("policy.txt", "text/plain", new ByteArrayInputStream(new byte[]{1}));
        DocumentFetcher.FetchResult fetched = new DocumentFetcher.FetchResult("policy.txt", "text/plain", 1, "checksum-1", new byte[]{1});
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Policy", true);
        when(uploadFetcher.fetch(request)).thenReturn(fetched);
        when(storage.objectKey(any(), any(), anyInt(), any())).thenReturn("key");
        when(repository.saveVersion(any(), any(), any(), any())).thenReturn(new KnowledgeRepository.VersionSaveResult("doc-1:v1", false));

        KnowledgeIngestionService.IngestionResult result = service.ingest(document, 1, request, ProcessMode.CHUNK);

        assertThat(result.created()).isFalse();
        verifyNoInteractions(parser, chunker);
        verify(repository, never()).markProcessing(any());
    }

    @Test
    void parserFailureMarksVersionFailedWithoutPublishing() {
        DocumentFetcher.DocumentFetchRequest request = DocumentFetcher.DocumentFetchRequest.upload("policy.txt", "text/plain", new ByteArrayInputStream(new byte[]{1}));
        DocumentFetcher.FetchResult fetched = new DocumentFetcher.FetchResult("policy.txt", "text/plain", 1, "checksum-1", new byte[]{1});
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Policy", true);
        when(uploadFetcher.fetch(request)).thenReturn(fetched);
        when(storage.objectKey(any(), any(), anyInt(), any())).thenReturn("key");
        when(repository.saveVersion(any(), any(), any(), any())).thenReturn(new KnowledgeRepository.VersionSaveResult("doc-1:v1", true));
        when(parser.parse(any(), any(), any())).thenThrow(new IllegalArgumentException("unsupported document"));

        assertThatThrownBy(() -> service.ingest(document, 1, request, ProcessMode.CHUNK))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("unsupported document");
        verify(repository).markFailed("doc-1:v1", "unsupported document");
        verify(repository, never()).publish(any(KnowledgeDocumentVersion.class));
    }
}
