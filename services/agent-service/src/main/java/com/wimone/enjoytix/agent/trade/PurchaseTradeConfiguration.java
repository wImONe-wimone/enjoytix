package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.remote.AgentOrderCreateRemoteService;
import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class PurchaseTradeConfiguration {
    @Bean
    @ConditionalOnMissingBean(TicketPurchaseQuoteService.class)
    public TicketPurchaseQuoteService ticketPurchaseQuoteService(TicketReadRemoteService ticketReadRemoteService) {
        return new RemoteTicketPurchaseQuoteService(ticketReadRemoteService);
    }

    @Bean
    @ConditionalOnMissingBean(PurchaseDraftService.class)
    public PurchaseDraftService purchaseDraftService(TicketPurchaseQuoteService quoteService) {
        return new InMemoryPurchaseDraftService(Duration.ofMinutes(5), quoteService);
    }

    @Bean
    @ConditionalOnMissingBean(PurchaseOrderService.class)
    public PurchaseOrderService purchaseOrderService(PurchaseDraftService draftService,
                                                      AgentOrderCreateRemoteService orderRemoteService) {
        return new PurchaseOrderService(draftService, orderRemoteService);
    }
}
