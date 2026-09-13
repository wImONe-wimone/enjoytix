package com.wimone.enjoytix.agent.knowledge.storage;

import com.wimone.enjoytix.agent.knowledge.source.DocumentFetcher;

public interface KnowledgeObjectStorage {
    String objectKey(String knowledgeBaseId, String documentId, int version, String checksum);
    void put(String key, DocumentFetcher.FetchResult document);
    boolean exists(String key);
}
