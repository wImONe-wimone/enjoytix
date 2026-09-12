package com.wimone.enjoytix.agent.knowledge.domain;

import java.time.Instant;

public final class KnowledgeDocumentVersion {
    private final String documentId;
    private final int version;
    private final String checksum;
    private final String objectKey;
    private final Instant createdAt;
    private DocumentStatus status;
    private int chunkCount;
    private String failureMessage;
    private boolean effective;

    private KnowledgeDocumentVersion(String documentId, int version, String checksum,
                                     String objectKey, Instant createdAt) {
        if (documentId == null || documentId.isBlank()) throw new IllegalArgumentException("document id is required");
        if (version <= 0) throw new IllegalArgumentException("version must be positive");
        if (checksum == null || checksum.isBlank()) throw new IllegalArgumentException("checksum is required");
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("object key is required");
        if (createdAt == null) throw new IllegalArgumentException("created at is required");
        this.documentId = documentId;
        this.version = version;
        this.checksum = checksum;
        this.objectKey = objectKey;
        this.createdAt = createdAt;
        this.status = DocumentStatus.PENDING;
    }

    public static KnowledgeDocumentVersion pending(String documentId, int version, String checksum,
                                                   String objectKey, Instant createdAt) {
        return new KnowledgeDocumentVersion(documentId, version, checksum, objectKey, createdAt);
    }

    public void startProcessing() {
        requireStatus(DocumentStatus.PENDING);
        status = DocumentStatus.RUNNING;
    }

    public void markSuccessful(int chunkCount) {
        requireStatus(DocumentStatus.RUNNING);
        if (chunkCount <= 0) throw new IllegalArgumentException("successful version must contain chunks");
        this.chunkCount = chunkCount;
        this.failureMessage = null;
        this.status = DocumentStatus.SUCCESS;
    }

    public void markFailed(String failureMessage) {
        requireStatus(DocumentStatus.RUNNING);
        if (failureMessage == null || failureMessage.isBlank()) throw new IllegalArgumentException("failure message is required");
        this.failureMessage = failureMessage;
        this.status = DocumentStatus.FAILED;
    }

    public void publish() {
        if (status != DocumentStatus.SUCCESS) throw new IllegalStateException("only successful version can be published");
        effective = true;
    }

    private void requireStatus(DocumentStatus expected) {
        if (status != expected) throw new IllegalStateException("version status must be " + expected.code());
    }

    public String documentId() { return documentId; }
    public int version() { return version; }
    public String checksum() { return checksum; }
    public String objectKey() { return objectKey; }
    public Instant createdAt() { return createdAt; }
    public DocumentStatus status() { return status; }
    public int chunkCount() { return chunkCount; }
    public String failureMessage() { return failureMessage; }
    public boolean effective() { return effective; }
}
