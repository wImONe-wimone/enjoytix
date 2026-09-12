package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.remote.AgentOrderCreateRemoteService;
import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PurchaseOrderSpringConfigurationTest {
    @Test
    void registersPurchaseServicesWhenOrderRemoteExists() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(PurchaseTradeConfiguration.class, TestRemoteConfiguration.class);
            context.refresh();

            assertThat(context.getBean(PurchaseDraftService.class)).isInstanceOf(InMemoryPurchaseDraftService.class);
            assertThat(context.getBean(PurchaseOrderService.class)).isNotNull();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestRemoteConfiguration {
        @Bean
        AgentOrderCreateRemoteService agentOrderCreateRemoteService() {
            return mock(AgentOrderCreateRemoteService.class);
        }

        @Bean
        TicketReadRemoteService ticketReadRemoteService() {
            return mock(TicketReadRemoteService.class);
        }
    }
}
