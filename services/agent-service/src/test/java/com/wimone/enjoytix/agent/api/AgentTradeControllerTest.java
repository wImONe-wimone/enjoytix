package com.wimone.enjoytix.agent.api;

import com.wimone.enjoytix.agent.auth.AgentAuthenticationContext;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.dto.AgentOrderCreateResponse;
import com.wimone.enjoytix.agent.trade.AgentOrderCreateRequest;
import com.wimone.enjoytix.agent.trade.PurchaseConfirmation;
import com.wimone.enjoytix.agent.trade.PurchaseConfirmationRequest;
import com.wimone.enjoytix.agent.trade.PurchaseDraft;
import com.wimone.enjoytix.agent.trade.PurchaseDraftRequest;
import com.wimone.enjoytix.agent.trade.PurchaseDraftService;
import com.wimone.enjoytix.agent.trade.PurchaseOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentTradeControllerTest {
    @AfterEach
    void clearContext() { AgentUserContextHolder.clear(); }

    @Test
    void createsDraftUsingAuthenticatedContextAndClearsItAfterRequest() {
        PurchaseDraftService drafts = mock(PurchaseDraftService.class);
        PurchaseOrderService orders = mock(PurchaseOrderService.class);
        AgentAuthenticationContext auth = new AgentAuthenticationContext(request -> new AgentUserContext(7L, "alice"));
        PurchaseDraftRequest request = request();
        PurchaseDraft draft = new PurchaseDraft("draft", 7L, 21L, 5L, List.of(101L, 102L), 2,
                new BigDecimal("299.00"), new BigDecimal("598.00"), "fingerprint", Instant.now().plusSeconds(300));
        when(drafts.createDraft(request)).thenReturn(draft);

        var result = new AgentTradeController(drafts, orders, auth).createDraft(mock(HttpServletRequest.class), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().draftId()).isEqualTo("draft");
        assertThat(AgentUserContextHolder.current()).isNull();
        verify(drafts).createDraft(request);
    }

    @Test
    void issuesConfirmationAndCreatesOrderOnlyThroughServices() {
        PurchaseDraftService drafts = mock(PurchaseDraftService.class);
        PurchaseOrderService orders = mock(PurchaseOrderService.class);
        AgentAuthenticationContext auth = new AgentAuthenticationContext(request -> new AgentUserContext(7L, "alice"));
        when(drafts.issueConfirmation("draft")).thenReturn(new PurchaseConfirmation("token", "draft", Instant.now().plusSeconds(300)));
        AgentOrderCreateResponse response = new AgentOrderCreateResponse(99L, "EO99", 5001L,
                new BigDecimal("598.00"), "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(15));
        when(orders.create("draft", "token")).thenReturn(response);
        AgentTradeController controller = new AgentTradeController(drafts, orders, auth);

        assertThat(controller.issueConfirmation(mock(HttpServletRequest.class), new PurchaseConfirmationRequest("draft")).getData().token())
                .isEqualTo("token");
        assertThat(controller.createOrder(mock(HttpServletRequest.class), new AgentOrderCreateRequest("draft", "token", 21L, 5L, 2, List.of(101L, 102L))).getData())
                .isEqualTo(response);
        verify(orders).create("draft", "token");
    }

    private PurchaseDraftRequest request() {
        return new PurchaseDraftRequest(21L, 5L, List.of(101L, 102L), 2);
    }
}
