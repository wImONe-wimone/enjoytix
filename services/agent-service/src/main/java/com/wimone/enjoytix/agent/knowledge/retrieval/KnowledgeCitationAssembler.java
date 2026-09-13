package com.wimone.enjoytix.agent.knowledge.retrieval;

import org.springframework.ai.document.Document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class KnowledgeCitationAssembler {
    public List<KnowledgeCitation> assemble(List<Document> retrievedDocuments, Set<String> authorizedKnowledgeBases) {
        if (retrievedDocuments == null || retrievedDocuments.isEmpty() || authorizedKnowledgeBases == null) {
            return List.of();
        }
        List<KnowledgeCitation> citations = new ArrayList<>();
        for (Document document : retrievedDocuments) {
            if (document == null) continue;
            Map<String, Object> metadata = document.getMetadata();
            String knowledgeBaseId = text(metadata, "knowledgeBaseId");
            String documentId = text(metadata, "documentId");
            String versionId = text(metadata, "versionId");
            String chunkId = text(metadata, "chunkId");
            if (knowledgeBaseId == null || !authorizedKnowledgeBases.contains(knowledgeBaseId)
                    || documentId == null || versionId == null || chunkId == null) continue;
            String title = safeText(metadata.get("title"), "Knowledge document");
            String source = safeText(metadata.get("sourceName"), "knowledge-base");
            if (metadata.get("sourcePage") != null) source += " page " + safeText(metadata.get("sourcePage"), "");
            if (metadata.get("sourceSection") != null) source += " section " + safeText(metadata.get("sourceSection"), "");
            double score = document.getScore() == null ? 0 : Math.max(0, Math.min(1, document.getScore()));
            citations.add(new KnowledgeCitation("cite-" + digest(document.getId()), knowledgeBaseId, documentId,
                    versionId, chunkId, title, source, score));
        }
        return List.copyOf(citations);
    }

    private String text(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private String safeText(Object value, String fallback) {
        if (value == null) return fallback;
        String text = value.toString().replaceAll("[\\r\\n\\t]", " ").trim();
        if (text.isBlank() || text.contains("secret") || text.contains("api-key") || text.startsWith("s3://") || text.contains("\\\\")) {
            return fallback;
        }
        return text;
    }

    private String digest(String value) {
        Objects.requireNonNull(value, "document id is required");
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < 12 && index < bytes.length; index++) result.append(String.format("%02x", bytes[index]));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
