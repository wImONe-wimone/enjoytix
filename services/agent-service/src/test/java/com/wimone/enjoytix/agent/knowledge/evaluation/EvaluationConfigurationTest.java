package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.knowledge.config.RagConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RagConfiguration.class, MeterRegistryConfiguration.class);

    @Test
    void doesNotRegisterEvaluationComponentsOutsideEvaluationProfile() {
        contextRunner.withPropertyValues(
                        "agent.knowledge.evaluation.enabled=true",
                        "agent.knowledge.evaluation.subject-profiles.public-user.user-id=1",
                        "agent.knowledge.evaluation.subject-profiles.public-user.username=evaluator")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(EvaluationDataset.class)
                        .doesNotHaveBean(EvaluationDatasetRegistry.class)
                        .doesNotHaveBean(EvaluationSubjectProfileRegistry.class));
    }

    @Test
    void registersEvaluationComponentsOnlyWhenEnabledInEvaluationProfile() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=evaluation",
                        "agent.knowledge.evaluation.enabled=true",
                        "agent.knowledge.evaluation.subject-profiles.public-user.user-id=1",
                        "agent.knowledge.evaluation.subject-profiles.public-user.username=evaluator")
                .run(context -> assertThat(context)
                        .hasSingleBean(EvaluationDataset.class)
                        .hasSingleBean(EvaluationDatasetRegistry.class)
                        .hasSingleBean(EvaluationSubjectProfileRegistry.class));
    }

    @Test
    void doesNotRegisterEvaluationComponentsWhenDisabledByDefault() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=evaluation",
                        "agent.knowledge.evaluation.subject-profiles.public-user.user-id=1",
                        "agent.knowledge.evaluation.subject-profiles.public-user.username=evaluator")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(EvaluationDataset.class)
                        .doesNotHaveBean(EvaluationDatasetRegistry.class)
                        .doesNotHaveBean(EvaluationSubjectProfileRegistry.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class MeterRegistryConfiguration {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
