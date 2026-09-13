package com.wimone.enjoytix.agent.knowledge.persistence;

import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeChunk;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeDocument;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeDocumentVersion;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeProvenance;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeSource;
import com.wimone.enjoytix.agent.knowledge.domain.ProcessMode;
import com.wimone.enjoytix.agent.knowledge.domain.SourceType;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import com.wimone.enjoytix.agent.knowledge.port.KnowledgeRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeRepositoryTest {

    private final KnowledgeDocumentMapper documentMapper = org.mockito.Mockito.mock(KnowledgeDocumentMapper.class);
    private final KnowledgeChunkMapper chunkMapper = org.mockito.Mockito.mock(KnowledgeChunkMapper.class);
    private final KnowledgeRepository repository = new MybatisKnowledgeRepository(documentMapper, chunkMapper);

    @Test
    void duplicateChecksumReturnsExistingVersionWithoutInserting() {
        KnowledgeDocument document = document();
        KnowledgeSource source = source("checksum-1");
        KnowledgeDocumentVersion version = version("checksum-1", 2);
        when(documentMapper.findVersionIdByChecksum(document.id(), source.checksum())).thenReturn("doc-1:v2");

        KnowledgeRepository.VersionSaveResult result = repository.saveVersion(document, version, source, ProcessMode.CHUNK);

        assertThat(result).isEqualTo(new KnowledgeRepository.VersionSaveResult("doc-1:v2", false));
        verify(documentMapper, never()).insertVersion(any());
    }

    @Test
    void publicationClearsOldEffectiveVersionOnlyAfterNewVersionIsReady() {
        KnowledgeDocumentVersion version = version("checksum-2", 2);
        version.startProcessing();
        version.markSuccessful(1);
        version.publish();

        when(documentMapper.markVersionEffective("doc-1:v2")).thenReturn(1);
        when(documentMapper.updateEffectiveVersion("doc-1", "doc-1:v2")).thenReturn(1);

        repository.publishVersion("doc-1", "doc-1:v2");

        InOrder order = inOrder(documentMapper);
        order.verify(documentMapper).clearEffectiveVersion("doc-1");
        order.verify(documentMapper).markVersionEffective("doc-1:v2");
        order.verify(documentMapper).updateEffectiveVersion("doc-1", "doc-1:v2");
    }

    @Test
    void effectiveChunkQueryDelegatesToVersionFilteredQuery() {
        KnowledgeChunk chunk = new KnowledgeChunk("chunk-1", "doc-1:v2", 0, "policy", "hash", 6, 2,
                Map.of("heading", "Ticket"), new KnowledgeProvenance("policy.md", 1, "Ticket"));
        when(chunkMapper.findEffectiveChunks("kb-1", "doc-1")).thenReturn(List.of(new KnowledgeChunkMapper.ChunkRow("chunk-1", "doc-1:v2", 0, "policy", "hash", 6, 2, "{\"heading\":\"Ticket\"}", "policy.md", 1, "Ticket")));

        assertThat(repository.findEffectiveChunks("kb-1", "doc-1")).containsExactly(chunk);
        verify(chunkMapper).findEffectiveChunks("kb-1", "doc-1");
    }

    @Test
    void failedVersionIsNotPublishedByRepository() {
        KnowledgeDocumentVersion version = version("checksum-3", 3);
        version.startProcessing();
        version.markFailed("parser failed");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.publish(version))
                .isInstanceOf(IllegalStateException.class);
        verify(documentMapper, never()).clearEffectiveVersion(any());
    }

    private KnowledgeDocument document() {
        return new KnowledgeDocument("doc-1", "kb-1", "Ticket policy", true);
    }

    private KnowledgeDocumentVersion version(String checksum, int number) {
        return KnowledgeDocumentVersion.pending("doc-1", number, checksum, "kb/doc-1/v" + number,
                Instant.parse("2026-09-12T00:00:00Z"));
    }

    private KnowledgeSource source(String checksum) {
        return new KnowledgeSource(SourceType.UPLOAD, "upload://policy", "policy.md", "text/markdown", 10,
                checksum, "kb/doc-1/v2");
    }
}
