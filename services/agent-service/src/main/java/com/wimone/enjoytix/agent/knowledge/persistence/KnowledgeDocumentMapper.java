package com.wimone.enjoytix.agent.knowledge.persistence;

import org.apache.ibatis.annotations.*;
import org.springframework.context.annotation.Profile;

@Mapper
@Profile("postgres")
public interface KnowledgeDocumentMapper {
    @Select("SELECT id FROM agent_knowledge_document_version WHERE document_id = #{documentId} AND checksum = #{checksum} LIMIT 1")
    String findVersionIdByChecksum(@Param("documentId") String documentId, @Param("checksum") String checksum);
    @Insert("INSERT INTO agent_knowledge_document (id, knowledge_base_id, name, enabled) VALUES (#{id}, #{knowledgeBaseId}, #{name}, #{enabled}) ON CONFLICT (id) DO NOTHING")
    int insertDocument(@Param("id") String id, @Param("knowledgeBaseId") String knowledgeBaseId, @Param("name") String name, @Param("enabled") boolean enabled);
    @Insert("INSERT INTO agent_knowledge_document_version (id, document_id, version_no, checksum, object_key, status, process_mode, chunk_count, failure_message, source_metadata, created_at) VALUES (#{id}, #{documentId}, #{version}, #{checksum}, #{objectKey}, #{status}, #{processMode}, #{chunkCount}, #{failureMessage}, CAST(#{sourceMetadata} AS jsonb), #{createdAt})")
    int insertVersion(VersionRow row);
    @Update("UPDATE agent_knowledge_document_version SET status = 'running', started_at = CURRENT_TIMESTAMP WHERE id = #{versionId} AND status = 'pending'")
    int markProcessing(@Param("versionId") String versionId);
    @Update("UPDATE agent_knowledge_document_version SET status = 'success', chunk_count = #{chunkCount}, completed_at = CURRENT_TIMESTAMP, failure_message = NULL WHERE id = #{versionId} AND status = 'running'")
    int markSuccessful(@Param("versionId") String versionId, @Param("chunkCount") int chunkCount);
    @Update("UPDATE agent_knowledge_document_version SET status = 'failed', failure_message = #{failureMessage}, completed_at = CURRENT_TIMESTAMP WHERE id = #{versionId} AND status = 'running'")
    int markFailed(@Param("versionId") String versionId, @Param("failureMessage") String failureMessage);
    @Update("UPDATE agent_knowledge_document_version SET effective = false, published_at = NULL WHERE document_id = #{documentId} AND effective = true")
    int clearEffectiveVersion(@Param("documentId") String documentId);
    @Update("UPDATE agent_knowledge_document_version SET effective = true, published_at = CURRENT_TIMESTAMP WHERE id = #{versionId} AND status = 'success'")
    int markVersionEffective(@Param("versionId") String versionId);
    @Update("UPDATE agent_knowledge_document SET effective_version_id = #{versionId}, updated_at = CURRENT_TIMESTAMP WHERE id = #{documentId}")
    int updateEffectiveVersion(@Param("documentId") String documentId, @Param("versionId") String versionId);
    record VersionRow(String id, String documentId, int version, String checksum, String objectKey, String status, String processMode, int chunkCount, String failureMessage, String sourceMetadata, java.time.Instant createdAt) {}
}
