package com.wimone.enjoytix.agent.knowledge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.knowledge.domain.*;
import com.wimone.enjoytix.agent.knowledge.port.KnowledgeRepository;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Repository
@Profile("postgres")
public class MybatisKnowledgeRepository implements KnowledgeRepository {
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final ObjectMapper objectMapper;
    @Autowired
    public MybatisKnowledgeRepository(KnowledgeDocumentMapper documentMapper, KnowledgeChunkMapper chunkMapper) { this(documentMapper, chunkMapper, new ObjectMapper()); }
    public MybatisKnowledgeRepository(KnowledgeDocumentMapper documentMapper, KnowledgeChunkMapper chunkMapper, ObjectMapper objectMapper) { this.documentMapper=documentMapper; this.chunkMapper=chunkMapper; this.objectMapper=objectMapper; }
    @Override @Transactional
    public VersionSaveResult saveVersion(KnowledgeDocument document, KnowledgeDocumentVersion version, KnowledgeSource source, ProcessMode processMode) {
        String id=versionId(version), existing=documentMapper.findVersionIdByChecksum(document.id(), source.checksum());
        if (existing != null) return new VersionSaveResult(existing,false);
        documentMapper.insertDocument(document.id(), document.knowledgeBaseId(), document.name(), document.enabled());
        documentMapper.insertVersion(new KnowledgeDocumentMapper.VersionRow(id, version.documentId(), version.version(), version.checksum(), version.objectKey(), version.status().code(), processMode.code(), version.chunkCount(), version.failureMessage(), toJson(source), version.createdAt()));
        return new VersionSaveResult(id,true);
    }
    @Override @Transactional public void markProcessing(String versionId) {
        if (documentMapper.markProcessing(versionId) != 1) throw new IllegalStateException("knowledge version cannot start processing");
    }
    @Override @Transactional public void markSuccessful(String versionId, int chunkCount) {
        if (chunkCount <= 0 || documentMapper.markSuccessful(versionId, chunkCount) != 1) throw new IllegalStateException("knowledge version cannot be marked successful");
    }
    @Override @Transactional public void markFailed(String versionId, String failureMessage) {
        if (failureMessage == null || failureMessage.isBlank() || documentMapper.markFailed(versionId, failureMessage) != 1) throw new IllegalStateException("knowledge version cannot be marked failed");
    }
    @Override @Transactional
    public void replaceChunks(String versionId, List<KnowledgeChunk> chunks) {
        if (chunks==null || chunks.isEmpty()) throw new IllegalArgumentException("chunks are required");
        chunkMapper.deleteByVersionId(versionId);
        chunks.forEach(c -> chunkMapper.insert(c,toJson(c.metadata()),c.provenance().sourceName(),c.provenance().page(),c.provenance().section()));
    }
    @Override @Transactional
    public void publishVersion(String documentId, String versionId) {
        documentMapper.clearEffectiveVersion(documentId);
        if (documentMapper.markVersionEffective(versionId)!=1 || documentMapper.updateEffectiveVersion(documentId,versionId)!=1) throw new IllegalStateException("knowledge version publication failed");
    }
    @Override public void publish(KnowledgeDocumentVersion version) {
        if (version.status()!=DocumentStatus.SUCCESS || !version.effective()) throw new IllegalStateException("only published successful version can be persisted");
        publishVersion(version.documentId(),versionId(version));
    }
    @Override public List<KnowledgeChunk> findEffectiveChunks(String knowledgeBaseId,String documentId) { return chunkMapper.findEffectiveChunks(knowledgeBaseId,documentId).stream().map(this::toDomain).toList(); }
    private String versionId(KnowledgeDocumentVersion v) { return v.documentId()+":v"+v.version(); }
    private KnowledgeChunk toDomain(KnowledgeChunkMapper.ChunkRow r) { return new KnowledgeChunk(r.id(),r.versionId(),r.ordinal(),r.content(),r.contentHash(),r.characterCount(),r.tokenCount(),fromJson(r.metadata()),new KnowledgeProvenance(r.sourceName(),r.sourcePage(),r.sourceSection())); }
    private String toJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch(JsonProcessingException e) { throw new IllegalArgumentException("knowledge metadata cannot be serialized",e); } }
    private Map<String,String> fromJson(String value) { try { if(value==null || value.isBlank()) return Map.of(); return objectMapper.readValue(value,objectMapper.getTypeFactory().constructMapType(Map.class,String.class,String.class)); } catch(JsonProcessingException e) { throw new IllegalStateException("knowledge metadata cannot be read",e); } }
}
