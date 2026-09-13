package com.wimone.enjoytix.agent.knowledge.config;

import com.wimone.enjoytix.agent.knowledge.embedding.DashScopeEmbeddingAdapter;
import com.wimone.enjoytix.agent.knowledge.embedding.EmbeddingPort;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(EmbeddingModel.class)
public class RagEmbeddingConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingPort.class)
    public EmbeddingPort dashScopeEmbeddingPort(EmbeddingModel embeddingModel, RagProperties properties) {
        return new DashScopeEmbeddingAdapter(embeddingModel, properties);
    }
}
