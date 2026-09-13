package com.wimone.enjoytix.agent.knowledge.persistence;

import com.wimone.enjoytix.agent.knowledge.embedding.EmbeddingPort;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryKnowledgeVectorStore implements VectorStore {

    private final EmbeddingPort embeddingPort;
    private final Map<String, StoredDocument> documents = new ConcurrentHashMap<>();

    public InMemoryKnowledgeVectorStore(EmbeddingPort embeddingPort) {
        this.embeddingPort = Objects.requireNonNull(embeddingPort, "embedding port is required");
    }

    @Override
    public String getName() {
        return "in-memory-knowledge-vector-store";
    }

    @Override
    public void add(List<Document> documentsToAdd) {
        if (documentsToAdd == null || documentsToAdd.isEmpty()) {
            return;
        }
        List<float[]> vectors = embeddingPort.embed(documentsToAdd.stream().map(Document::getText).toList());
        if (vectors.size() != documentsToAdd.size()) {
            throw new IllegalArgumentException("embedding count does not match document count");
        }
        for (int index = 0; index < documentsToAdd.size(); index++) {
            Document document = documentsToAdd.get(index);
            documents.put(document.getId(), new StoredDocument(document, vectors.get(index)));
        }
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        String knowledgeBaseId = extractKnowledgeBaseScope(request.getFilterExpression());
        float[] queryVector = embeddingPort.embed(List.of(request.getQuery())).get(0);
        return documents.values().stream()
                .filter(document -> knowledgeBaseId.equals(document.document().getMetadata().get("knowledgeBaseId")))
                .map(document -> scored(document, queryVector))
                .filter(document -> document.getScore() != null && document.getScore() >= request.getSimilarityThreshold())
                .sorted(Comparator.comparing(Document::getScore).reversed())
                .limit(request.getTopK())
                .toList();
    }

    @Override
    public void delete(List<String> ids) {
        if (ids == null) {
            return;
        }
        ids.forEach(documents::remove);
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        throw new UnsupportedOperationException("filtered vector deletion requires an indexing tombstone");
    }

    private Document scored(StoredDocument storedDocument, float[] queryVector) {
        return storedDocument.document().mutate()
                .score(cosineSimilarity(queryVector, storedDocument.vector()))
                .build();
    }

    private String extractKnowledgeBaseScope(Filter.Expression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("knowledge base scope is required");
        }
        if (expression.type() != Filter.ExpressionType.EQ ||
                !(expression.left() instanceof Filter.Key key) ||
                !(expression.right() instanceof Filter.Value value) ||
                !"knowledgeBaseId".equals(key.key()) || !(value.value() instanceof String scope) || scope.isBlank()) {
            throw new IllegalArgumentException("knowledge base scope is required");
        }
        return scope;
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left.length != right.length) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record StoredDocument(Document document, float[] vector) {
    }
}
