package com.wimone.enjoytix.agent.knowledge.retrieval;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeCitationAssemblerTest {
    @Test
    void emitsOpaqueSafeCitationsOnlyForRetrievedAuthorizedChunks() {
        Document authorized = Document.builder().id("idx-internal-secret-id")
                .text("refund policy").score(0.88)
                .metadata(Map.of("knowledgeBaseId", "kb-1", "documentId", "doc-1", "versionId", "v2",
                        "chunkId", "chunk-7", "title", "Refund policy", "sourceName", "policy.md",
                        "sourcePage", 4, "objectKey", "s3://bucket/private/path", "secret", "do-not-leak"))
                .build();
        Document unauthorized = Document.builder().id("idx-other").text("other")
                .score(0.99).metadata(Map.of("knowledgeBaseId", "kb-2", "documentId", "doc-2",
                        "versionId", "v1", "chunkId", "chunk-1", "title", "Other")).build();

        List<KnowledgeCitation> citations = new KnowledgeCitationAssembler()
                .assemble(List.of(authorized, unauthorized), Set.of("kb-1"));

        assertThat(citations).hasSize(1);
        KnowledgeCitation citation = citations.get(0);
        assertThat(citation.knowledgeBaseId()).isEqualTo("kb-1");
        assertThat(citation.sourceLocation()).isEqualTo("policy.md page 4");
        assertThat(citation.citationKey()).startsWith("cite-").doesNotContain("idx-internal-secret-id");
        assertThat(citation.sourceLocation()).doesNotContain("s3://", "private", "secret");
    }

    @Test
    void omitsChunksWithoutCompleteProvenance() {
        Document incomplete = Document.builder().id("idx-incomplete").text("content").score(0.8)
                .metadata(Map.of("knowledgeBaseId", "kb-1", "documentId", "doc-1")).build();

        assertThat(new KnowledgeCitationAssembler().assemble(List.of(incomplete), Set.of("kb-1"))).isEmpty();
    }
}
