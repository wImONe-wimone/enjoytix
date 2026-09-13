package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(service.consume(confirmation.token(), confirmation.draftId())).isEqualTo(draft);
        assertThatThrownBy(() -> service.consume(confirmation.token(), confirmation.draftId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("confirmation is invalid");
    }

    @Test
    void unconfirmedQuoteCannotBeConsumed() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        InMemoryPurchaseDraftService service = new InMemoryPurchaseDraftService(Duration.ofMinutes(5), quoteService());
        PurchaseDraft draft = service.createDraft(request());
        assertThatThrownBy(() -> service.consume("not-issued", draft.draftId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("confirmation is invalid");
    }

    @Test
    void rejectsExpiredQuoteBeforeConfirmationOrConsumption() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        InMemoryPurchaseDraftService service = new InMemoryPurchaseDraftService(Duration.ofMillis(1), quoteService());
        PurchaseDraft draft = service.createDraft(request());

        awaitExpiry(draft.expiresAt());
        PurchaseConfirmation confirmation = service.issueConfirmation(draft.draftId());
        assertThat(confirmation.token()).isEmpty();
        assertThat(service.confirm("expired", draft.draftId(), request())).isFalse();
    }

    @Test
    void rejectsWrongTokenAndCrossUserConsumption() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        InMemoryPurchaseDraftService service = new InMemoryPurchaseDraftService(Duration.ofMinutes(5), quoteService());
        PurchaseDraft draft = service.createDraft(request());
        PurchaseConfirmation confirmation = service.issueConfirmation(draft.draftId());
        assertThat(service.confirm(confirmation.token(), draft.draftId(), request())).isTrue();

        assertThatThrownBy(() -> service.consume("wrong-token", draft.draftId()))
                .isInstanceOf(IllegalArgumentException.class);
        AgentUserContextHolder.set(new AgentUserContext(8L, "mallory"));
        assertThatThrownBy(() -> service.consume(confirmation.token(), draft.draftId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidatesConfirmationWhenAuthoritativePriceChanges() {
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        TicketPurchaseQuoteService changingQuote = new TicketPurchaseQuoteService() {
            private int calls;
            @Override
            public PurchaseQuote quote(PurchaseDraftRequest request) {
                calls++;
                BigDecimal unitPrice = calls == 1 ? new BigDecimal("299.00") : new BigDecimal("349.00");
                return new PurchaseQuote(request.seatIds(), unitPrice, unitPrice.multiply(BigDecimal.valueOf(request.quantity())));
            }
        };
        InMemoryPurchaseDraftService service = new InMemoryPurchaseDraftService(Duration.ofMinutes(5), changingQuote);
        PurchaseDraft draft = service.createDraft(request());
        assertThat(service.issueConfirmation(draft.draftId()).token()).isEmpty();
    }

    private void awaitExpiry(Instant expiresAt) {
        while (Instant.now().isBefore(expiresAt)) {
            Thread.yield();
        }
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
