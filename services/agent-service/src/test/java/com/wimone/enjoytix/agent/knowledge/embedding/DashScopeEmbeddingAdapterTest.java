package com.wimone.enjoytix.agent.knowledge.embedding;

import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashScopeEmbeddingAdapterTest {

    @Test
    void embedsBatchAndValidatesConfiguredDimension() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(List.of("one", "two"))).thenReturn(List.of(
                new float[]{0.1f, 0.2f}, new float[]{0.3f, 0.4f}));

        DashScopeEmbeddingAdapter adapter = new DashScopeEmbeddingAdapter(model, properties(2));

        assertThat(adapter.embed(List.of("one", "two"))).containsExactly(
                new float[]{0.1f, 0.2f}, new float[]{0.3f, 0.4f});
    }

    @Test
    void rejectsDimensionMismatchAndProviderFailuresWithSafeErrors() {
        EmbeddingModel mismatch = mock(EmbeddingModel.class);
        when(mismatch.embed(List.of("one"))).thenReturn(List.of(new float[]{0.1f}));
        DashScopeEmbeddingAdapter adapter = new DashScopeEmbeddingAdapter(mismatch, properties(2));

        assertThatThrownBy(() -> adapter.embed(List.of("one")))
                .isInstanceOf(EmbeddingProviderException.class)
                .hasMessage("Embedding dimension does not match configured dimension.");

        EmbeddingModel unavailable = mock(EmbeddingModel.class);
        when(unavailable.embed(List.of("one"))).thenThrow(new RuntimeException("api-key=secret"));
        DashScopeEmbeddingAdapter unavailableAdapter = new DashScopeEmbeddingAdapter(unavailable, properties(2));
        assertThatThrownBy(() -> unavailableAdapter.embed(List.of("one")))
                .isInstanceOf(EmbeddingProviderException.class)
                .hasMessage("Embedding provider is temporarily unavailable.")
                .hasRootCauseMessage("api-key=secret");

        EmbeddingModel timeout = mock(EmbeddingModel.class);
        when(timeout.embed(List.of("one"))).thenThrow(new RuntimeException(new TimeoutException("slow")));
        assertThatThrownBy(() -> new DashScopeEmbeddingAdapter(timeout, properties(2)).embed(List.of("one")))
                .isInstanceOf(EmbeddingProviderException.class)
                .hasMessage("Embedding provider timed out.");
    }

    private RagProperties properties(int dimension) {
        RagProperties properties = new RagProperties();
        properties.setEmbeddingDimension(dimension);
        return properties;
    }
}
