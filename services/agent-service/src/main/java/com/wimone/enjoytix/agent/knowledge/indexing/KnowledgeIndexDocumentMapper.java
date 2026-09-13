package com.wimone.enjoytix.agent.knowledge.indexing;

import com.wimone.enjoytix.agent.knowledge.domain.*;
import org.springframework.ai.document.Document;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class KnowledgeIndexDocumentMapper {
    public List<KnowledgeIndexDocument> map(KnowledgeDocument document, KnowledgeDocumentVersion version,
                                             List<KnowledgeChunk> chunks, String embeddingModel) {
        Objects.requireNonNull(document); Objects.requireNonNull(version); Objects.requireNonNull(chunks);
        if (embeddingModel == null || embeddingModel.isBlank()) throw new IllegalArgumentException("embedding model is required");
        if (version.status() != DocumentStatus.SUCCESS) throw new IllegalArgumentException("only successful version can be indexed");
        if (!version.documentId().equals(document.id())) throw new IllegalArgumentException("version does not belong to document");
        String versionId = versionId(version);
        for (KnowledgeChunk chunk : chunks) if (!versionId.equals(chunk.versionId())) throw new IllegalArgumentException("chunk version does not match version");
        String versionFingerprint = fingerprint(version.checksum(), chunks);
        String generationId = "gen-" + digest(document.knowledgeBaseId() + "|" + document.id() + "|" + versionId + "|" + versionFingerprint);
        List<KnowledgeIndexDocument> result = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            Map<String,Object> metadata = new LinkedHashMap<>();
            metadata.put("knowledgeBaseId", document.knowledgeBaseId()); metadata.put("documentId", document.id());
            metadata.put("versionId", versionId); metadata.put("chunkId", chunk.id()); metadata.put("generationId", generationId);
            metadata.put("contentFingerprint", chunk.contentHash()); metadata.put("versionFingerprint", versionFingerprint);
            metadata.put("embeddingModel", embeddingModel); metadata.put("title", document.name());
            metadata.put("sourceName", chunk.provenance().sourceName());
            if (chunk.provenance().page() != null) metadata.put("sourcePage", chunk.provenance().page());
            if (chunk.provenance().section() != null) metadata.put("sourceSection", chunk.provenance().section());
            chunk.metadata().forEach((key,value) -> { if (!Set.of("objectKey","checksum","secret","token").contains(key)) metadata.putIfAbsent(key,value); });
            String id = "idx-" + digest(generationId + "|" + chunk.id() + "|" + chunk.contentHash());
            result.add(new KnowledgeIndexDocument(id, chunk.content(), metadata, document.knowledgeBaseId(), document.id(), versionId,
                    chunk.id(), chunk.contentHash(), versionFingerprint, generationId, embeddingModel));
        }
        return List.copyOf(result);
    }
    private String versionId(KnowledgeDocumentVersion version) { return version.documentId() + ":v" + version.version(); }
    private String fingerprint(String checksum, List<KnowledgeChunk> chunks) {
        return digest(checksum + "|" + chunks.stream().sorted(Comparator.comparing(KnowledgeChunk::ordinal))
                .map(c -> c.id() + ":" + c.ordinal() + ":" + c.contentHash()).reduce("", (a,b) -> a + "|" + b));
    }
    private String digest(String value) { try { byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder out=new StringBuilder(); for(byte b:bytes) out.append(String.format("%02x",b)); return out.toString(); } catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
}
