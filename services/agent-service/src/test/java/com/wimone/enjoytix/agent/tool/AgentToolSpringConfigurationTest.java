package com.wimone.enjoytix.agent.tool;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolSpringConfigurationTest {

    @Test
    void doesNotRegisterShowSessionToolWithoutQueryService() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(AgentToolConfiguration.class, AgentToolRegistrar.class, InMemoryAgentToolRegistry.class);
            context.refresh();

            assertThat(context.getBean(AgentToolRegistry.class).find("search_show_sessions")).isEmpty();
        }
    }

    @Test
    void registersShowSessionToolWhenQueryServiceExists() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TestQueryServiceConfiguration.class, AgentToolConfiguration.class,
                    AgentToolRegistrar.class, InMemoryAgentToolRegistry.class);
            context.refresh();

            assertThat(context.getBean(AgentToolRegistry.class).find("search_show_sessions"))
                    .isPresent()
                    .get()
                    .isInstanceOf(ShowSessionQueryTool.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestQueryServiceConfiguration {
        @Bean
        ShowSessionQueryService showSessionQueryService() {
            return (city, date, keyword) -> List.of();
        }
    }
}