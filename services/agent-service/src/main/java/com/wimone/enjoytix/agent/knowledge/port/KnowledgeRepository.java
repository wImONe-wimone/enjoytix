package com.wimone.enjoytix.agent.knowledge.port;

import com.wimone.enjoytix.agent.knowledge.domain.*;
import java.util.List;

public interface KnowledgeRepository {
    VersionSaveResult saveVersion(KnowledgeDocument document, KnowledgeDocumentVersion version, KnowledgeSource source, ProcessMode processMode);
    void markProcessing(String versionId);
    void markSuccessful(String versionId, int chunkCount);
    void markFailed(String versionId, String failureMessage);
    void replaceChunks(String versionId, List<KnowledgeChunk> chunks);
    void publishVersion(String documentId, String versionId);
    void publish(KnowledgeDocumentVersion version);
    List<KnowledgeChunk> findEffectiveChunks(String knowledgeBaseId, String documentId);
    record VersionSaveResult(String versionId, boolean created) {}
}
