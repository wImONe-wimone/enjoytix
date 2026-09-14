package com.wimone.enjoytix.agent.knowledge.config;

import com.wimone.enjoytix.agent.knowledge.observability.KnowledgeRetrievalObserver;
import com.wimone.enjoytix.agent.knowledge.observability.MicrometerKnowledgeRetrievalObserver;
import com.wimone.enjoytix.agent.knowledge.rollout.RagRolloutDecider;
import com.wimone.enjoytix.agent.knowledge.rollout.RagRolloutProperties;
import com.wimone.enjoytix.agent.knowledge.rollout.RagRuntimePolicy;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({RagProperties.class, RagRolloutProperties.class})
public class RagConfiguration {
    @Bean
    RagRolloutDecider ragRolloutDecider(RagRolloutProperties properties) {
        return new RagRolloutDecider(properties);
    }

    @Bean
    RagRuntimePolicy ragRuntimePolicy(RagRolloutDecider decider) {
        return new RagRuntimePolicy(decider);
    }

    @Bean
    KnowledgeRetrievalObserver knowledgeRetrievalObserver(MeterRegistry meters) {
        return new MicrometerKnowledgeRetrievalObserver(meters);
    }
}