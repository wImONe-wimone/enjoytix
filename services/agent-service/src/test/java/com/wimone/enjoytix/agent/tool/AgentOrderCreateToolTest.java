package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.dto.AgentOrderCreateResponse;
import com.wimone.enjoytix.agent.trade.PurchaseOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentOrderCreateToolTest {
    @AfterEach
    void clearContext() {
        AgentUserContextHolder.clear();
    }

    @Test
    void createsOrderOnlyThroughPurchaseOrderService() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        PurchaseOrderService orders = mock(PurchaseOrderService.class);
        AgentOrderCreateResponse response = new AgentOrderCreateResponse(99L, "EO99", 5001L,
                new BigDecimal("598.00"), "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(15));
        when(orders.create("draft-1", "token-1", "request-1")).thenReturn(response);

        AgentToolResult result = new AgentOrderCreateTool(orders).execute(new AgentToolRequest(Map.of(
                "draftId", "draft-1", "confirmationToken", "token-1", "idempotencyKey", "request-1")));

        assertThat(result).isEqualTo(AgentToolResult.success(response));
        verify(orders).create("draft-1", "token-1", "request-1");
    }

    @Test
    void rejectsMissingConfirmationBeforeCallingOrderService() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        PurchaseOrderService orders = mock(PurchaseOrderService.class);

        AgentToolResult result = new AgentOrderCreateTool(orders).execute(new AgentToolRequest(Map.of(
                "draftId", "draft-1")));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_ARGUMENT");
        verifyNoInteractions(orders);
    }

    @Test
    void rejectsUnauthenticatedSideEffect() {
        PurchaseOrderService orders = mock(PurchaseOrderService.class);

        AgentToolResult result = new AgentOrderCreateTool(orders).execute(new AgentToolRequest(Map.of(
                "draftId", "draft-1", "confirmationToken", "token-1", "idempotencyKey", "request-1")));

        assertThat(result.errorCode()).isEqualTo("UNAUTHENTICATED");
        verifyNoInteractions(orders);
    }

    @Test
    void sanitizesDomainFailure() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        PurchaseOrderService orders = mock(PurchaseOrderService.class);
        when(orders.create("draft-1", "token-1", "request-1"))
                .thenThrow(new IllegalStateException("secret remote stack detail"));

        AgentToolResult result = new AgentOrderCreateTool(orders).execute(new AgentToolRequest(Map.of(
                "draftId", "draft-1", "confirmationToken", "token-1", "idempotencyKey", "request-1")));

        assertThat(result.errorCode()).isEqualTo("ORDER_CREATION_REJECTED");
        assertThat(result.errorMessage()).doesNotContain("secret remote stack detail");
    }

    @Test
    void requiresIdempotencyKeyForSideEffectingTool() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        PurchaseOrderService orders = mock(PurchaseOrderService.class);

        AgentToolResult result = new AgentOrderCreateTool(orders).execute(new AgentToolRequest(Map.of(
                "draftId", "draft-1", "confirmationToken", "token-1")));

        assertThat(result.errorCode()).isEqualTo("INVALID_ARGUMENT");
        verifyNoInteractions(orders);
    }
}
