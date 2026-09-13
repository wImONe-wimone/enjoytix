package com.wimone.enjoytix.agent.knowledge.persistence;

import com.wimone.enjoytix.agent.knowledge.embedding.EmbeddingPort;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

class InMemoryKnowledgeVectorStoreTest {

    @Test
    void satisfiesVectorStoreContract() {
        EmbeddingPort embeddingPort = texts -> texts.stream()
                .map(text -> text.contains("refund") ? new float[]{1.0f, 0.0f} : new float[]{0.0f, 1.0f})
                .toList();
        VectorStore store = new InMemoryKnowledgeVectorStore(embeddingPort);

        KnowledgeVectorStoreContract.assertAddSearchAndDelete(store);
    }
}
