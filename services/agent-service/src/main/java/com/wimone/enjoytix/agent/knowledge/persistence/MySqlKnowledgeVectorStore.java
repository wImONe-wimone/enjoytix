package com.wimone.enjoytix.agent.knowledge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.knowledge.embedding.EmbeddingPort;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MySqlKnowledgeVectorStore implements VectorStore {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingPort embeddingPort;
    private final ObjectMapper objectMapper;

    public MySqlKnowledgeVectorStore(JdbcTemplate jdbcTemplate,
                                     EmbeddingPort embeddingPort,
                                     ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbc template is required");
        this.embeddingPort = Objects.requireNonNull(embeddingPort, "embedding port is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "object mapper is required");
    }

    @Override
    public String getName() {
        return "mysql-knowledge-vector-store";
    }

    @Override
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        List<String> texts = documents.stream().map(Document::getText).toList();
        List<float[]> vectors = embeddingPort.embed(texts);
        if (vectors.size() != documents.size()) {
            throw new IllegalArgumentException("embedding count does not match document count");
        }
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            Map<String, Object> metadata = document.getMetadata();
            jdbcTemplate.update("INSERT INTO agent_knowledge_embedding " +
                            "(embedding_id, generation_id, knowledge_base_id, document_id, version_id, chunk_id, content, metadata_json, " +
                            "content_fingerprint, embedding_model, embedding_dimension, embedding_json, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'READY')",
                    document.getId(), required(metadata, "generationId"), required(metadata, "knowledgeBaseId"),
                    required(metadata, "documentId"), required(metadata, "versionId"), required(metadata, "chunkId"),
                    document.getText(), writeJson(metadata), required(metadata, "contentFingerprint"),
                    required(metadata, "embeddingModel"), vectors.get(index).length, writeJson(vectors.get(index)));
        }
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        String knowledgeBaseId = extractKnowledgeBaseScope(request.getFilterExpression());
        float[] queryVector = embeddingPort.embed(List.of(request.getQuery())).get(0);
        List<VectorRow> rows = jdbcTemplate.query(
                "SELECT embedding_id, content, metadata_json, embedding_json " +
                        "FROM agent_knowledge_embedding e " +
                        "JOIN agent_knowledge_index_generation g ON g.generation_id = e.generation_id " +
                        "WHERE e.knowledge_base_id = ? AND e.status = 'READY' AND g.active = TRUE AND g.status = 'READY'",
                (resultSet, rowNum) -> new VectorRow(
                        resultSet.getString("embedding_id"),
                        resultSet.getString("content"),
                        resultSet.getString("metadata_json"),
                        resultSet.getString("embedding_json")),
                knowledgeBaseId);

        return rows.stream()
                .map(row -> toScoredDocument(row, queryVector))
                .filter(document -> document.getScore() != null && document.getScore() >= request.getSimilarityThreshold())
                .sorted(Comparator.comparing(Document::getScore).reversed())
                .limit(request.getTopK())
                .toList();
    }

    @Override
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        List<Object> arguments = new ArrayList<>(ids);
        jdbcTemplate.update("UPDATE agent_knowledge_embedding SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP(6) " +
                "WHERE embedding_id IN (" + placeholders + ")", arguments.toArray());
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        throw new UnsupportedOperationException("filtered vector deletion requires an indexing tombstone");
    }

    private Document toScoredDocument(VectorRow row, float[] queryVector) {
        try {
            Map<String, Object> metadata = new HashMap<>(readJson(row.metadataJson(), METADATA_TYPE));
            double score = cosineSimilarity(queryVector, readJson(row.embeddingJson(), float[].class));
            return Document.builder()
                    .id(row.id())
                    .text(row.content())
                    .metadata(metadata)
                    .score(score)
                    .build();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("stored embedding is invalid", ex);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) throws JsonProcessingException {
        String json = value;
        if (value != null && value.startsWith(String.valueOf((char) 34)) && value.endsWith(String.valueOf((char) 34))) {
            json = objectMapper.readValue(value, String.class);
        }
        return objectMapper.readValue(json, type);
    }

    private <T> T readJson(String value, Class<T> type) throws JsonProcessingException {
        String json = value;
        if (value != null && value.startsWith(String.valueOf((char) 34)) && value.endsWith(String.valueOf((char) 34))) {
            json = objectMapper.readValue(value, String.class);
        }
        return objectMapper.readValue(json, type);
    }

    private String extractKnowledgeBaseScope(Filter.Expression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("knowledge base scope is required");
        }
        if (expression.type() != Filter.ExpressionType.EQ) {
            throw new IllegalArgumentException("knowledge base scope must be an equality filter");
        }
        if (!(expression.left() instanceof Filter.Key key) ||
                !(expression.right() instanceof Filter.Value value) ||
                !"knowledgeBaseId".equals(key.key()) || !(value.value() instanceof String scope) || scope.isBlank()) {
            throw new IllegalArgumentException("knowledge base scope is required");
        }
        return scope;
    }

    private String required(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("metadata " + key + " is required");
        }
        return value.toString();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("embedding metadata cannot be serialized", ex);
        }
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

    private record VectorRow(String id, String content, String metadataJson, String embeddingJson) {
    }
}
