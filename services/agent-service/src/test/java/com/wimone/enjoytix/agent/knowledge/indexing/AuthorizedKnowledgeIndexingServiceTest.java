package com.wimone.enjoytix.agent.knowledge.indexing;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.knowledge.domain.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class AuthorizedKnowledgeIndexingServiceTest {
    @Test
    void deniesUnauthorizedIndexingWithoutCallingJobAndAuditsSanitizedIdentifiers() {
        RecordingJob job = new RecordingJob();
        RecordingAuditSink audit = new RecordingAuditSink();
        AuthorizedKnowledgeIndexingService service = new AuthorizedKnowledgeIndexingService(
                (user, knowledgeBaseId, documentId) -> false, job, audit);

        IndexingProgress result = service.rebuild(new AgentUserContext(7L, "alice"),
                document(), version(), List.of(chunk()), "text-embedding-v3");

        assertThat(result.status()).isEqualTo(IndexingStatus.FAILED);
        assertThat(result.failureMessage()).isEqualTo("Knowledge indexing is not authorized.");
        assertThat(job.called).isFalse();
        assertThat(audit.event).isNotNull();
        assertThat(audit.event.outcome()).isEqualTo("DENIED");
        assertThat(audit.event.subject()).isEqualTo("user:7");
        assertThat(audit.event.knowledgeBaseId()).isEqualTo("kb-1");
        assertThat(audit.event.documentId()).isEqualTo("doc-1");
        assertThat(audit.event).hasToString(org.assertj.core.api.Assertions.assertThat(audit.event).actual().toString());
        assertThat(audit.event.toString()).doesNotContain("secret", "object-key", "raw content");
    }

    @Test
    void auditsSuccessfulIndexingWithoutRawDocumentContent() {
        RecordingAuditSink audit = new RecordingAuditSink();
        AuthorizedKnowledgeIndexingService service = new AuthorizedKnowledgeIndexingService(
                (user, knowledgeBaseId, documentId) -> true, new RecordingJob(), audit);

        IndexingProgress result = service.upsert(new AgentUserContext(7L, "alice"), document(), version(),
                List.of(chunk()), "text-embedding-v3");

        assertThat(result.status()).isEqualTo(IndexingStatus.ACTIVE);
        assertThat(audit.event.outcome()).isEqualTo("SUCCESS");
        assertThat(audit.event.contentFingerprint()).isEqualTo("fingerprint");
        assertThat(audit.event.toString()).doesNotContain("raw content", "object-key", "secret");
    }

    private KnowledgeDocument document() { return new KnowledgeDocument("doc-1", "kb-1", "Policy", true); }
    private KnowledgeDocumentVersion version() { KnowledgeDocumentVersion v = KnowledgeDocumentVersion.pending("doc-1", 2, "checksum", "object-key", Instant.parse("2026-09-13T00:00:00Z")); v.startProcessing(); v.markSuccessful(1); return v; }
    private KnowledgeChunk chunk() { return new KnowledgeChunk("c1", "doc-1:v2", 0, "raw content secret", "fingerprint", 18, 3, Map.of("secret", "hidden"), new KnowledgeProvenance("policy.md", 1, "Policy")); }

    static final class RecordingJob extends KnowledgeIndexingJob {
        boolean called;
        RecordingJob() { super(new org.springframework.ai.vectorstore.VectorStore() { public String getName(){return "test";} public void add(List<org.springframework.ai.document.Document> d){} public List<org.springframework.ai.document.Document> similaritySearch(org.springframework.ai.vectorstore.SearchRequest r){return List.of();} public void delete(List<String> ids){} public void delete(org.springframework.ai.vectorstore.filter.Filter.Expression e){} }, new InMemoryIndexGenerationStore(), 10, 1); }
        @Override public IndexingProgress rebuild(KnowledgeDocument d, KnowledgeDocumentVersion v, List<KnowledgeChunk> c, String m) { called=true; return new IndexingProgress("g", IndexingStatus.ACTIVE, c.size(), c.size(), 1, 1, true, null); }
        @Override public IndexingProgress upsert(KnowledgeDocument d, KnowledgeDocumentVersion v, List<KnowledgeChunk> c, String m) { called=true; return rebuild(d,v,c,m); }
    }
    static final class RecordingAuditSink implements KnowledgeIndexAuditSink { KnowledgeIndexAuditEvent event; public void record(KnowledgeIndexAuditEvent event){this.event=event;} }
}
