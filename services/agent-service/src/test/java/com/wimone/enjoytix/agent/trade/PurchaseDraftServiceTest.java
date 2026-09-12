package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseDraftServiceTest {
    @AfterEach
    void clearContext() { AgentUserContextHolder.clear(); }

    @Test
    void createsUserBoundDraftAndConsumesConfirmationOnlyOnce() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        InMemoryPurchaseDraftService service = new InMemoryPurchaseDraftService(Duration.ofMinutes(5), quoteService());
        PurchaseDraftRequest request = request();

        PurchaseDraft draft = service.createDraft(request);
        PurchaseConfirmation confirmation = service.issueConfirmation(draft.draftId());

        assertThat(draft.userId()).isEqualTo(7L);
        assertThat(service.confirm(confirmation.token(), confirmation.draftId(), request)).isTrue();
        assertThat(service.confirm(confirmation.token(), confirmation.draftId(), request)).isFalse();
    }

    @Test
    void rejectsChangedSeatsAndDifferentUser() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        InMemoryPurchaseDraftService service = new InMemoryPurchaseDraftService(Duration.ofMinutes(5), quoteService());
        PurchaseDraftRequest request = request();
        PurchaseDraft draft = service.createDraft(request);
        PurchaseConfirmation confirmation = service.issueConfirmation(draft.draftId());

        assertThat(service.confirm(confirmation.token(), confirmation.draftId(), new PurchaseDraftRequest(
                21L, 5L, List.of(101L, 103L), 2))).isFalse();
        AgentUserContextHolder.set(new AgentUserContext(8L, "mallory"));
        assertThat(service.confirm(confirmation.token(), confirmation.draftId(), request)).isFalse();
    }

    private PurchaseDraftRequest request() {
        return new PurchaseDraftRequest(21L, 5L, List.of(101L, 102L), 2);
    }

    private TicketPurchaseQuoteService quoteService() {
        return request -> new PurchaseQuote(request.seatIds(), new BigDecimal("299.00"), new BigDecimal("598.00"));
    }
}
