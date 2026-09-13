package com.wimone.enjoytix.agent.knowledge.chunk;

import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeChunk;
import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeProvenance;
import com.wimone.enjoytix.agent.knowledge.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Component
public class FixedSizeTextChunker implements TextChunker {
    private final TextChunkProperties properties;

    public FixedSizeTextChunker(TextChunkProperties properties) { this.properties = properties; }

    @Override
    public List<KnowledgeChunk> chunk(String versionId, String sourceName, ParsedDocument document) {
        String text = document.text().trim();
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int start = 0;
        int ordinal = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + properties.maxCharacters());
            if (end < text.length()) {
                int boundary = text.lastIndexOf('\n', end);
                if (boundary > start + properties.maxCharacters() / 2) end = boundary;
            }
            String content = text.substring(start, end).trim();
            if (!content.isBlank()) {
                String hash = sha256(content);
                chunks.add(new KnowledgeChunk(versionId + ":" + ordinal, versionId, ordinal, content, hash,
                        content.length(), tokenCount(content), document.metadata(), new KnowledgeProvenance(sourceName, null, null)));
                ordinal++;
            }
            if (end >= text.length()) break;
            start = Math.max(end - properties.overlapCharacters(), start + 1);
        }
        return List.copyOf(chunks);
    }

    private int tokenCount(String value) { return value.isBlank() ? 0 : value.trim().split("\\s+").length; }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
}
