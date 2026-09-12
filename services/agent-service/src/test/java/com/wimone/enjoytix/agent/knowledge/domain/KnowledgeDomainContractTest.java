package com.wimone.enjoytix.agent.knowledge.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeDomainContractTest {
    @Test
    void documentVersionPublishesOnlyAfterSuccessfulProcessing() {
        KnowledgeDocumentVersion version = KnowledgeDocumentVersion.pending(
                "doc-1", 2, "sha256", "object/doc-1/v2", Instant.now());

        assertThat(version.status()).isEqualTo(DocumentStatus.PENDING);
        assertThatThrownBy(version::publish)
                .isInstanceOf(IllegalStateException.class);

        version.startProcessing();
        version.markSuccessful(3);
        version.publish();

        assertThat(version.status()).isEqualTo(DocumentStatus.SUCCESS);
        assertThat(version.effective()).isTrue();
        assertThat(version.chunkCount()).isEqualTo(3);
    }

    @Test
    void failedVersionCannotBecomeEffective() {
        KnowledgeDocumentVersion version = KnowledgeDocumentVersion.pending(
                "doc-1", 1, "sha256", "object/doc-1/v1", Instant.now());

        version.startProcessing();
        version.markFailed("embedding unavailable");

        assertThat(version.status()).isEqualTo(DocumentStatus.FAILED);
        assertThat(version.failureMessage()).isEqualTo("embedding unavailable");
        assertThatThrownBy(version::publish)
                .isInstanceOf(IllegalStateException.class);
        assertThat(version.effective()).isFalse();
    }

    @Test
    void documentAcceptsOnlyItsPublishedSuccessfulVersion() {
        KnowledgeDocument document = new KnowledgeDocument("doc-1", "kb-1", "Ticket policy", true);
        KnowledgeDocumentVersion version = KnowledgeDocumentVersion.pending(
                "doc-1", 1, "sha256", "object/doc-1/v1", Instant.now());

        assertThatThrownBy(() -> document.activate(version))
                .isInstanceOf(IllegalStateException.class);

        version.startProcessing();
        version.markSuccessful(1);
        version.publish();
        document.activate(version);

        assertThat(document.effectiveVersion()).isSameAs(version);
    }

    @Test
    void chunkKeepsSourceMetadataAndStableIdentityInputs() {
        KnowledgeChunk chunk = new KnowledgeChunk("chunk-1", "version-1", 0,
                "ticket policy", "hash-1", 13, 4,
                Map.of("heading", "Refund policy"),
                new KnowledgeProvenance("policy.pdf", 2, "Refund policy"));

        assertThat(chunk.id()).isEqualTo("chunk-1");
        assertThat(chunk.metadata()).containsEntry("heading", "Refund policy");
        assertThat(chunk.provenance().page()).isEqualTo(2);
    }
}
