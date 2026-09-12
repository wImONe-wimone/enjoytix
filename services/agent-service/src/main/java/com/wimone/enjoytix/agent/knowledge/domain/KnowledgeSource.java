package com.wimone.enjoytix.agent.knowledge.domain;

public record KnowledgeSource(
        SourceType type,
        String location,
        String originalFilename,
        String contentType,
        long contentLength,
        String checksum,
        String objectKey) {
    public KnowledgeSource {
        if (type == null) throw new IllegalArgumentException("source type is required");
        if (location == null || location.isBlank()) throw new IllegalArgumentException("source location is required");
        if (checksum == null || checksum.isBlank()) throw new IllegalArgumentException("source checksum is required");
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("source object key is required");
        if (contentLength < 0) throw new IllegalArgumentException("source content length cannot be negative");
    }
}
