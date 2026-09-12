package com.wimone.enjoytix.agent.api;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.AgentOrderCreateRemoteService;
import com.wimone.enjoytix.agent.trade.InMemoryPurchaseDraftService;
import com.wimone.enjoytix.agent.trade.PurchaseDraft;
import com.wimone.enjoytix.agent.trade.PurchaseDraftRequest;
import com.wimone.enjoytix.agent.trade.PurchaseOrderService;
import com.wimone.enjoytix.agent.trade.PurchaseQuote;
import com.wimone.enjoytix.agent.trade.TicketPurchaseQuoteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentTradeSecurityFlowTest {
    @AfterEach
    void clearContext() { AgentUserContextHolder.clear(); }

    @Test
    void rejectsCrossUserConfirmationWithoutCallingOrderService() {
        InMemoryPurchaseDraftService drafts = new InMemoryPurchaseDraftService(Duration.ofMinutes(5), quoteService());
        AgentOrderCreateRemoteService remote = mock(AgentOrderCreateRemoteService.class);
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        PurchaseDraft draft = drafts.createDraft(request());
        var confirmation = drafts.issueConfirmation(draft.draftId());
        AgentUserContextHolder.set(new AgentUserContext(8L, "mallory"));

        PurchaseOrderService service = new PurchaseOrderService(drafts, remote);
        assertThatThrownBy(() -> service.create(draft.draftId(), confirmation.token()))
                .isInstanceOf(IllegalStateException.class);
        verify(remote, never()).create(any());
    }

    @Test
    void rejectsExpiredConfirmationWithoutCallingOrderService() {
        InMemoryPurchaseDraftService drafts = new InMemoryPurchaseDraftService(Duration.ofMillis(1), quoteService());
        AgentOrderCreateRemoteService remote = mock(AgentOrderCreateRemoteService.class);
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        PurchaseDraft draft = drafts.createDraft(request());
        var confirmation = drafts.issueConfirmation(draft.draftId());
        try {
            Thread.sleep(5);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }

        PurchaseOrderService service = new PurchaseOrderService(drafts, remote);
        assertThatThrownBy(() -> service.create(draft.draftId(), confirmation.token()))
                .isInstanceOf(IllegalStateException.class);
        verify(remote, never()).create(any());
    }

    @Test
    void consumedConfirmationCannotCreateOrderTwice() {
        InMemoryPurchaseDraftService drafts = new InMemoryPurchaseDraftService(Duration.ofMinutes(5), quoteService());
        AgentOrderCreateRemoteService remote = mock(AgentOrderCreateRemoteService.class);
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        PurchaseDraft draft = drafts.createDraft(request());
        var confirmation = drafts.issueConfirmation(draft.draftId());
        PurchaseOrderService service = new PurchaseOrderService(drafts, remote);

        when(remote.create(any())).thenReturn(com.wimone.enjoytix.framework.convention.result.Result.success(
                new com.wimone.enjoytix.agent.remote.dto.AgentOrderCreateResponse(99L, "EO99", 5001L,
                        new BigDecimal("598.00"), "PENDING_PAYMENT", java.time.LocalDateTime.now().plusMinutes(15))));
        assertThat(service.create(draft.draftId(), confirmation.token())).isNotNull();
        assertThatThrownBy(() -> service.create(draft.draftId(), confirmation.token()))
                .isInstanceOf(IllegalStateException.class);
        verify(remote, times(1)).create(any());
    }

    private PurchaseDraftRequest request() {
        return new PurchaseDraftRequest(21L, 5L, List.of(101L, 102L), 2);
    }

    private PurchaseDraftRequest changedRequest() {
        return new PurchaseDraftRequest(21L, 5L, List.of(101L, 103L), 2);
    }

    private TicketPurchaseQuoteService quoteService() {
        return request -> new PurchaseQuote(request.seatIds(), new BigDecimal("299.00"), new BigDecimal("598.00"));
    }
}
