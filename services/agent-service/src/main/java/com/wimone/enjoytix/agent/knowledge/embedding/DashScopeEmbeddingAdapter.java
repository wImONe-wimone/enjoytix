package com.wimone.enjoytix.agent.knowledge.embedding;

import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.Objects;

public class DashScopeEmbeddingAdapter implements EmbeddingPort {

    private final EmbeddingModel embeddingModel;
    private final int expectedDimension;

    public DashScopeEmbeddingAdapter(EmbeddingModel embeddingModel, RagProperties properties) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel, "embedding model is required");
        this.expectedDimension = properties.getEmbeddingDimension();
        if (expectedDimension <= 0) {
            throw new IllegalArgumentException("embedding dimension must be positive");
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty() || texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("texts are required");
        }
        try {
            List<float[]> vectors = embeddingModel.embed(texts);
            validate(texts, vectors);
            return List.copyOf(vectors);
        } catch (EmbeddingProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (containsTimeout(ex)) {
                throw new EmbeddingProviderException("Embedding provider timed out.", ex);
            }
            throw new EmbeddingProviderException("Embedding provider is temporarily unavailable.", ex);
        }
    }

    private void validate(List<String> texts, List<float[]> vectors) {
        if (vectors == null || vectors.size() != texts.size() || vectors.stream().anyMatch(vector -> vector == null)) {
            throw new EmbeddingProviderException("Embedding provider returned an invalid batch.");
        }
        if (vectors.stream().anyMatch(vector -> vector.length != expectedDimension)) {
            throw new EmbeddingProviderException("Embedding dimension does not match configured dimension.");
        }
    }

    private boolean containsTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
