package com.wimone.enjoytix.agent.knowledge.indexing;

import com.wimone.enjoytix.agent.knowledge.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeIndexDocumentMapperTest {
    private final KnowledgeIndexDocumentMapper mapper = new KnowledgeIndexDocumentMapper();

    @Test
    void mapsStableImmutableDocumentsAndSafeProvenance() {
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Ticket policy", true);
        KnowledgeDocumentVersion version = version("checksum-v2", 2);
        Map<String, String> sourceMetadata = new LinkedHashMap<>();
        sourceMetadata.put("heading", "Refund policy");
        List<KnowledgeChunk> chunks = new ArrayList<>(List.of(
                chunk("chunk-1", "doc-1:v2", "Refunds require review.", "hash-a", sourceMetadata,
                        new KnowledgeProvenance("policy.md", 2, "Refunds")),
                chunk("chunk-2", "doc-1:v2", "Tickets cannot be transferred.", "hash-b", Map.of(),
                        new KnowledgeProvenance("policy.md", 3, "Transfers"))));

        List<KnowledgeIndexDocument> first = mapper.map(document, version, chunks, "text-embedding-v3");
        List<KnowledgeIndexDocument> second = mapper.map(document, version, chunks, "text-embedding-v3");
        KnowledgeIndexDocument indexed = first.get(0);

        assertThat(first).isEqualTo(second);
        assertThat(first).extracting(KnowledgeIndexDocument::generationId).containsOnly(indexed.generationId());
        assertThat(first).extracting(KnowledgeIndexDocument::versionFingerprint).containsOnly(indexed.versionFingerprint());
        assertThat(indexed.knowledgeBaseId()).isEqualTo("kb-1");
        assertThat(indexed.documentId()).isEqualTo("doc-1");
        assertThat(indexed.versionId()).isEqualTo("doc-1:v2");
        assertThat(indexed.chunkId()).isEqualTo("chunk-1");
        assertThat(indexed.contentFingerprint()).isEqualTo("hash-a");
        assertThat(indexed.metadata()).containsEntry("title", "Ticket policy")
                .containsEntry("sourceName", "policy.md").containsEntry("sourcePage", 2)
                .containsEntry("sourceSection", "Refunds").containsEntry("heading", "Refund policy")
                .doesNotContainKeys("objectKey", "checksum");
        assertThat(indexed.toVectorDocument().getMetadata()).containsEntry("generationId", indexed.generationId())
                .containsEntry("versionFingerprint", indexed.versionFingerprint())
                .containsEntry("contentFingerprint", "hash-a").containsEntry("embeddingModel", "text-embedding-v3");

        sourceMetadata.put("heading", "changed");
        chunks.clear();
        assertThat(indexed.metadata()).containsEntry("heading", "Refund policy");
        assertThatThrownBy(() -> indexed.metadata().put("unsafe", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> first.add(indexed)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void changedContentCreatesNewFingerprintAndGeneration() {
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Ticket policy", true);
        KnowledgeDocumentVersion version = version("checksum-v2", 2);
        KnowledgeIndexDocument original = mapper.map(document, version, List.of(
                chunk("chunk-1", "doc-1:v2", "Original", "hash-a", Map.of(), new KnowledgeProvenance("policy.md", null, null))),
                "text-embedding-v3").get(0);
        KnowledgeIndexDocument changed = mapper.map(document, version, List.of(
                chunk("chunk-1", "doc-1:v2", "Changed", "hash-b", Map.of(), new KnowledgeProvenance("policy.md", null, null))),
                "text-embedding-v3").get(0);

        assertThat(changed.versionFingerprint()).isNotEqualTo(original.versionFingerprint());
        assertThat(changed.generationId()).isNotEqualTo(original.generationId());
        assertThat(changed.id()).isNotEqualTo(original.id());
    }

    @Test
    void rejectsChunkFromAnotherVersion() {
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Ticket policy", true);
        assertThatThrownBy(() -> mapper.map(document, version("checksum-v2", 2), List.of(
                chunk("chunk-1", "doc-1:v1", "Original", "hash-a", Map.of(), new KnowledgeProvenance("policy.md", null, null))),
                "text-embedding-v3")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("version");
    }

    private KnowledgeDocumentVersion version(String checksum, int number) {
        KnowledgeDocumentVersion version = KnowledgeDocumentVersion.pending("doc-1", number, checksum,
                "knowledge/kb-1/doc-1/v" + number + "/" + checksum, Instant.parse("2026-09-13T00:00:00Z"));
        version.startProcessing();
        version.markSuccessful(2);
        return version;
    }

    private KnowledgeChunk chunk(String id, String versionId, String content, String hash, Map<String, String> metadata,
                                 KnowledgeProvenance provenance) {
        return new KnowledgeChunk(id, versionId, 0, content, hash, content.length(), 4, metadata, provenance);
    }
}

