package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.remote.OrderReadRemoteService;
import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;
import com.wimone.enjoytix.agent.remote.UserReadRemoteService;
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

    @Test
    void registersReadOnlyToolsWhenRemoteContractsExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TestRemoteContractConfiguration.class, AgentToolConfiguration.class,
                    AgentToolRegistrar.class, InMemoryAgentToolRegistry.class);
            context.refresh();

            AgentToolRegistry registry = context.getBean(AgentToolRegistry.class);
            assertThat(registry.list()).extracting(AgentTool::name).containsExactlyInAnyOrder(
                    "get_ticket_availability", "get_seat_availability", "get_current_user",
                    "list_current_user_orders", "get_current_user_order");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestQueryServiceConfiguration {
        @Bean
        ShowSessionQueryService showSessionQueryService() {
            return (city, date, keyword) -> List.of();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestRemoteContractConfiguration {
        @Bean
        TicketReadRemoteService ticketReadRemoteService() {
            return null;
        }

        @Bean
        UserReadRemoteService userReadRemoteService() {
            return null;
        }

        @Bean
        OrderReadRemoteService orderReadRemoteService() {
            return null;
        }
    }
}
