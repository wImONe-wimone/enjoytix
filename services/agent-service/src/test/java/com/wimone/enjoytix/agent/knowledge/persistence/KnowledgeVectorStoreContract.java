package com.wimone.enjoytix.agent.knowledge.persistence;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class KnowledgeVectorStoreContract {

    private KnowledgeVectorStoreContract() {
    }

    static void assertAddSearchAndDelete(VectorStore store) {
        List<Document> documents = List.of(
                new Document("refund policy", metadata("chunk-1")),
                new Document("venue map", metadata("chunk-2")));
        store.add(documents);

        SearchRequest request = SearchRequest.builder()
                .query("refund")
                .topK(1)
                .similarityThreshold(0.8)
                .filterExpression(new FilterExpressionBuilder().eq("knowledgeBaseId", "kb-1").build())
                .build();
        List<Document> results = store.similaritySearch(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getText()).isEqualTo("refund policy");
        assertThat(results.get(0).getScore()).isGreaterThan(0.99);
        assertThat(results.get(0).getMetadata()).containsEntry("knowledgeBaseId", "kb-1");

        assertThatThrownBy(() -> store.similaritySearch(SearchRequest.builder().query("refund").topK(1).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("knowledge base scope is required");

        store.delete(List.of(results.get(0).getId()));
        assertThat(store.similaritySearch(request)).isEmpty();
    }

    private static Map<String, Object> metadata(String chunkId) {
        return Map.of(
                "knowledgeBaseId", "kb-1", "documentId", "doc-1", "versionId", "v1",
                "chunkId", chunkId, "generationId", "gen-1", "contentFingerprint", "fp-1",
                "embeddingModel", "test");
    }
}
