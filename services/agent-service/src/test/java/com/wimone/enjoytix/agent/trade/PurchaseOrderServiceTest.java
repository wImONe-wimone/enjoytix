package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.AgentOrderCreateRemoteService;
import com.wimone.enjoytix.agent.remote.dto.AgentOrderCreateResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseOrderServiceTest {
    private static final String TOKEN = String.valueOf(new char[]{'t','o','k','e','n'});
    private static final String DRAFT = String.valueOf(new char[]{'d','r','a','f','t'});
    private static final String ALICE = String.valueOf(new char[]{'a','l','i','c','e'});

    @AfterEach
    void clearContext() { AgentUserContextHolder.clear(); }

    @Test
    void confirmsDraftBeforeCreatingOrder() {
        AgentUserContextHolder.set(new AgentUserContext(7L, ALICE));
        PurchaseDraftService drafts = mock(PurchaseDraftService.class);
        AgentOrderCreateRemoteService remote = mock(AgentOrderCreateRemoteService.class);
        PurchaseDraft draft = draft();
        when(drafts.consume(TOKEN, DRAFT)).thenReturn(draft);
        AgentOrderCreateResponse response = new AgentOrderCreateResponse(99L,
                String.valueOf(new char[]{'E','O','9','9'}), 5001L, new BigDecimal("598.00"),
                "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(15));
        when(remote.create(any())).thenReturn(Result.success(response));

        PurchaseOrderService service = new PurchaseOrderService(drafts, remote);
        assertThat(service.create(DRAFT, TOKEN)).isEqualTo(response);
        assertThat(response.lockId()).isEqualTo(5001L);
        assertThat(response.totalAmount()).isEqualByComparingTo("598.00");
        assertThat(response.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.payExpireTime()).isNotNull();

        verify(drafts).consume(TOKEN, DRAFT);
        verify(remote).create(new AgentOrderCreateRequest(DRAFT, TOKEN, 21L, 5L, 2, List.of(101L, 102L)));
    }

    @Test
    void doesNotCreateOrderWhenConfirmationFails() {
        AgentUserContextHolder.set(new AgentUserContext(7L, ALICE));
        PurchaseDraftService drafts = mock(PurchaseDraftService.class);
        AgentOrderCreateRemoteService remote = mock(AgentOrderCreateRemoteService.class);
        when(drafts.consume(TOKEN, DRAFT)).thenThrow(new IllegalArgumentException("invalid"));

        PurchaseOrderService service = new PurchaseOrderService(drafts, remote);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(DRAFT, TOKEN))
                .isInstanceOf(IllegalStateException.class);
        verify(remote, never()).create(any());
    }

    @Test
    void createsOrderFromConsumedServerDraft() {
        AgentUserContextHolder.set(new AgentUserContext(7L, ALICE));
        PurchaseDraftService drafts = mock(PurchaseDraftService.class);
        AgentOrderCreateRemoteService remote = mock(AgentOrderCreateRemoteService.class);
        PurchaseDraft draft = new PurchaseDraft(DRAFT, 7L, 21L, 5L, List.of(101L, 102L), 2,
                new BigDecimal("299.00"), new BigDecimal("598.00"), "fingerprint", java.time.Instant.now().plusSeconds(60));
        when(drafts.consume(TOKEN, DRAFT)).thenReturn(draft);
        AgentOrderCreateResponse response = new AgentOrderCreateResponse(99L, "EO99", 5001L,
                new BigDecimal("598.00"), "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(15));
        when(remote.create(any())).thenReturn(Result.success(response));

        assertThat(new PurchaseOrderService(drafts, remote).create(DRAFT, TOKEN)).isEqualTo(response);

        verify(remote).create(new AgentOrderCreateRequest(DRAFT, TOKEN, 21L, 5L, 2, List.of(101L, 102L)));
    }

    @Test
    void returnsOriginalOutcomeForRepeatedIdempotencyKeyWithoutCreatingSecondOrder() {
        AgentUserContextHolder.set(new AgentUserContext(7L, ALICE));
        PurchaseDraftService drafts = mock(PurchaseDraftService.class);
        AgentOrderCreateRemoteService remote = mock(AgentOrderCreateRemoteService.class);
        PurchaseDraft draft = draft();
        when(drafts.consume(TOKEN, DRAFT)).thenReturn(draft);
        AgentOrderCreateResponse response = new AgentOrderCreateResponse(99L, "EO99", 5001L,
                new BigDecimal("598.00"), "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(15));
        when(remote.create(any())).thenReturn(Result.success(response));

        PurchaseOrderService service = new PurchaseOrderService(drafts, remote);
        assertThat(service.create(DRAFT, TOKEN, "request-1")).isEqualTo(response);
        assertThat(service.create(DRAFT, TOKEN, "request-1")).isEqualTo(response);

        verify(drafts, times(1)).consume(TOKEN, DRAFT);
        verify(remote, times(1)).create(any());
    }

    @Test
    void rejectsReuseOfIdempotencyKeyForDifferentRequest() {
        AgentUserContextHolder.set(new AgentUserContext(7L, ALICE));
        PurchaseDraftService drafts = mock(PurchaseDraftService.class);
        AgentOrderCreateRemoteService remote = mock(AgentOrderCreateRemoteService.class);
        PurchaseDraft draft = draft();
        when(drafts.consume(anyString(), anyString())).thenReturn(draft);
        when(remote.create(any())).thenReturn(Result.success(new AgentOrderCreateResponse(99L, "EO99", 5001L,
                new BigDecimal("598.00"), "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(15))));

        PurchaseOrderService service = new PurchaseOrderService(drafts, remote);
        service.create(DRAFT, TOKEN, "request-1");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create("another-draft", "another-token", "request-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idempotency key is already used for another request");
        verify(remote, times(1)).create(any());
    }

    private PurchaseDraftRequest request() {
        return new PurchaseDraftRequest(21L, 5L, List.of(101L, 102L), 2);
    }

    private PurchaseDraft draft() {
        return new PurchaseDraft(DRAFT, 7L, 21L, 5L, List.of(101L, 102L), 2,
                new BigDecimal("299.00"), new BigDecimal("598.00"), "fingerprint", java.time.Instant.now().plusSeconds(60));
    }
}
