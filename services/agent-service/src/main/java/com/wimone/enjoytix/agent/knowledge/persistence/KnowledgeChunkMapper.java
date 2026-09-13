package com.wimone.enjoytix.agent.knowledge.persistence;

import com.wimone.enjoytix.agent.knowledge.domain.KnowledgeChunk;
import org.apache.ibatis.annotations.*;
import org.springframework.context.annotation.Profile;
import java.util.List;

@Mapper
@Profile("postgres")
public interface KnowledgeChunkMapper {
    @Delete("DELETE FROM agent_knowledge_chunk WHERE version_id = #{versionId}") int deleteByVersionId(@Param("versionId") String versionId);
    @Insert("INSERT INTO agent_knowledge_chunk (id, version_id, ordinal, content, content_hash, character_count, token_count, metadata, source_name, source_page, source_section) VALUES (#{chunk.id}, #{chunk.versionId}, #{chunk.ordinal}, #{chunk.content}, #{chunk.contentHash}, #{chunk.characterCount}, #{chunk.tokenCount}, CAST(#{metadata} AS jsonb), #{sourceName}, #{sourcePage}, #{sourceSection})")
    int insert(@Param("chunk") KnowledgeChunk chunk, @Param("metadata") String metadata, @Param("sourceName") String sourceName, @Param("sourcePage") Integer sourcePage, @Param("sourceSection") String sourceSection);
    @Select("SELECT c.id, c.version_id, c.ordinal, c.content, c.content_hash, c.character_count, c.token_count, c.metadata, c.source_name, c.source_page, c.source_section FROM agent_knowledge_chunk c JOIN agent_knowledge_document_version v ON v.id = c.version_id JOIN agent_knowledge_document d ON d.id = v.document_id WHERE d.knowledge_base_id = #{knowledgeBaseId} AND d.id = #{documentId} AND d.enabled = true AND v.effective = true AND v.status = 'success' ORDER BY c.ordinal")
    List<ChunkRow> findEffectiveChunks(@Param("knowledgeBaseId") String knowledgeBaseId, @Param("documentId") String documentId);
    record ChunkRow(String id, String versionId, int ordinal, String content, String contentHash, int characterCount, int tokenCount, String metadata, String sourceName, Integer sourcePage, String sourceSection) {}
}
