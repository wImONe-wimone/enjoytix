package com.wimone.enjoytix.agent.knowledge.config;

import com.wimone.enjoytix.agent.knowledge.observability.KnowledgeRetrievalObserver;
import com.wimone.enjoytix.agent.knowledge.observability.MicrometerKnowledgeRetrievalObserver;
import com.wimone.enjoytix.agent.knowledge.evaluation.EvaluationDataset;
import com.wimone.enjoytix.agent.knowledge.evaluation.EvaluationDatasetLoader;
import com.wimone.enjoytix.agent.knowledge.evaluation.EvaluationDatasetRegistry;
import com.wimone.enjoytix.agent.knowledge.evaluation.EvaluationProperties;
import com.wimone.enjoytix.agent.knowledge.evaluation.EvaluationRetrievalAdapter;
import com.wimone.enjoytix.agent.knowledge.evaluation.EvaluationSubjectProfile;
import com.wimone.enjoytix.agent.knowledge.evaluation.EvaluationSubjectProfileRegistry;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.knowledge.retrieval.AuthorizedKnowledgeRetrievalService;
import com.wimone.enjoytix.agent.knowledge.rollout.RagRolloutDecider;
import com.wimone.enjoytix.agent.knowledge.rollout.RagRolloutProperties;
import com.wimone.enjoytix.agent.knowledge.rollout.RagRuntimePolicy;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.ResourceLoader;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({RagProperties.class, RagRolloutProperties.class, EvaluationProperties.class})
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

    @Bean
    @Profile("evaluation")
    @ConditionalOnProperty(prefix = "agent.knowledge.evaluation", name = "enabled", havingValue = "true")
    EvaluationDataset evaluationDataset(EvaluationProperties properties, ResourceLoader resourceLoader,
                                        EvaluationSubjectProfileRegistry subjectProfiles) {
        String datasetId = "baseline";
        return new EvaluationDatasetLoader().load(resourceLoader.getResource(properties.getDatasetPath()), datasetId, subjectProfiles);
    }

    @Bean
    @Profile("evaluation")
    @ConditionalOnProperty(prefix = "agent.knowledge.evaluation", name = "enabled", havingValue = "true")
    EvaluationDatasetRegistry evaluationDatasetRegistry(EvaluationDataset dataset) {
        return new EvaluationDatasetRegistry(Map.of(dataset.datasetId(), dataset));
    }

    @Bean
    @Profile("evaluation")
    @ConditionalOnProperty(prefix = "agent.knowledge.evaluation", name = "enabled", havingValue = "true")
    @ConditionalOnBean(AuthorizedKnowledgeRetrievalService.class)
    EvaluationRetrievalAdapter evaluationRetrievalAdapter(EvaluationDatasetRegistry datasets,
                                                           EvaluationSubjectProfileRegistry subjectProfiles,
                                                           AuthorizedKnowledgeRetrievalService retrievalService,
                                                           RagProperties ragProperties) {
        return new EvaluationRetrievalAdapter(datasets, subjectProfiles, retrievalService, ragProperties);
    }

    @Bean
    @Profile("evaluation")
    @ConditionalOnProperty(prefix = "agent.knowledge.evaluation", name = "enabled", havingValue = "true")
    EvaluationSubjectProfileRegistry evaluationSubjectProfileRegistry(EvaluationProperties properties) {
        Map<String, EvaluationSubjectProfile> profiles = new LinkedHashMap<>();
        properties.getSubjectProfiles().forEach((profileId, profile) -> {
            if (profile.getUserId() == null) throw new IllegalArgumentException("evaluation profile userId is required: " + profileId);
            profiles.put(profileId, new EvaluationSubjectProfile(profileId,
                    new AgentUserContext(profile.getUserId(), profile.getUsername())));
        });
        return new EvaluationSubjectProfileRegistry(profiles);
    }
}
