package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.remote.CommentReadRemoteService;
import com.wimone.enjoytix.agent.remote.OrderReadRemoteService;
import com.wimone.enjoytix.agent.remote.PerformanceRemoteService;
import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;
import com.wimone.enjoytix.agent.remote.UserReadRemoteService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentToolExecutionProperties.class)
public class AgentToolConfiguration {

    @Bean
    @ConditionalOnBean(ShowSessionQueryService.class)
    @ConditionalOnMissingBean(ShowSessionQueryTool.class)
    public ShowSessionQueryTool showSessionQueryTool(ShowSessionQueryService queryService) {
        return new ShowSessionQueryTool(queryService);
    }

    @Bean
    @ConditionalOnBean(PerformanceRemoteService.class)
    @ConditionalOnMissingBean(PerformanceDetailQueryTool.class)
    public PerformanceDetailQueryTool performanceDetailQueryTool(PerformanceRemoteService performanceRemoteService) {
        return new PerformanceDetailQueryTool(performanceRemoteService);
    }

    @Bean
    @ConditionalOnBean(CommentReadRemoteService.class)
    @ConditionalOnMissingBean(PerformanceRatingSummaryQueryTool.class)
    public PerformanceRatingSummaryQueryTool performanceRatingSummaryQueryTool(
            CommentReadRemoteService commentReadRemoteService) {
        return new PerformanceRatingSummaryQueryTool(commentReadRemoteService);
    }

    @Bean
    @ConditionalOnBean(TicketReadRemoteService.class)
    @ConditionalOnMissingBean(TicketAvailabilityQueryTool.class)
    public TicketAvailabilityQueryTool ticketAvailabilityQueryTool(TicketReadRemoteService ticketReadRemoteService) {
        return new TicketAvailabilityQueryTool(ticketReadRemoteService);
    }

    @Bean
    @ConditionalOnBean(TicketReadRemoteService.class)
    @ConditionalOnMissingBean(SeatAvailabilityQueryTool.class)
    public SeatAvailabilityQueryTool seatAvailabilityQueryTool(TicketReadRemoteService ticketReadRemoteService) {
        return new SeatAvailabilityQueryTool(ticketReadRemoteService);
    }

    @Bean
    @ConditionalOnBean(UserReadRemoteService.class)
    @ConditionalOnMissingBean(CurrentUserQueryTool.class)
    public CurrentUserQueryTool currentUserQueryTool(UserReadRemoteService userReadRemoteService) {
        return new CurrentUserQueryTool(userReadRemoteService);
    }

    @Bean
    @ConditionalOnBean(OrderReadRemoteService.class)
    @ConditionalOnMissingBean(OrderListQueryTool.class)
    public OrderListQueryTool orderListQueryTool(OrderReadRemoteService orderReadRemoteService) {
        return new OrderListQueryTool(orderReadRemoteService);
    }

    @Bean
    @ConditionalOnBean(OrderReadRemoteService.class)
    @ConditionalOnMissingBean(OrderDetailQueryTool.class)
    public OrderDetailQueryTool orderDetailQueryTool(OrderReadRemoteService orderReadRemoteService) {
        return new OrderDetailQueryTool(orderReadRemoteService);
    }
}
