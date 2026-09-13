package com.wimone.enjoytix.agent.knowledge.config;

import com.wimone.enjoytix.agent.knowledge.source.KnowledgeSourceProperties;
import com.wimone.enjoytix.agent.knowledge.storage.KnowledgeStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KnowledgeSourceProperties.class, KnowledgeStorageProperties.class})
public class KnowledgeSourceConfiguration {
}
