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
import com.wimone.enjoytix.agent.trade.PurchaseDraftResponse;
import com.wimone.enjoytix.agent.trade.PurchaseDraftService;
import com.wimone.enjoytix.agent.trade.PurchaseOrderService;
import com.wimone.enjoytix.framework.web.Results;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/trade")
public class AgentTradeController {
    private final PurchaseDraftService draftService;
    private final PurchaseOrderService orderService;
    private final AgentAuthenticationContext authenticationContext;

    public AgentTradeController(PurchaseDraftService draftService, PurchaseOrderService orderService,
                                AgentAuthenticationContext authenticationContext) {
        this.draftService = draftService;
        this.orderService = orderService;
        this.authenticationContext = authenticationContext;
    }

    @PostMapping("/drafts")
    public com.wimone.enjoytix.framework.convention.result.Result<PurchaseDraftResponse> createDraft(
            HttpServletRequest request, @Valid @RequestBody PurchaseDraftRequest draftRequest) {
        return withUser(request, () -> {
            PurchaseDraft draft = draftService.createDraft(draftRequest);
            return Results.success(PurchaseDraftResponse.from(draft));
        });
    }

    @PostMapping("/confirmations")
    public com.wimone.enjoytix.framework.convention.result.Result<PurchaseConfirmation> issueConfirmation(
            HttpServletRequest request, @Valid @RequestBody PurchaseConfirmationRequest confirmationRequest) {
        return withUser(request, () -> Results.success(
                draftService.issueConfirmation(confirmationRequest.draftId())));
    }

    @PostMapping("/orders")
    public com.wimone.enjoytix.framework.convention.result.Result<AgentOrderCreateResponse> createOrder(
            HttpServletRequest request, @Valid @RequestBody AgentOrderCreateRequest orderRequest) {
        return withUser(request, () -> Results.success(orderService.create(orderRequest.draftId(),
                orderRequest.confirmationToken())));
    }

    private <T> com.wimone.enjoytix.framework.convention.result.Result<T> withUser(
            HttpServletRequest request, java.util.function.Supplier<com.wimone.enjoytix.framework.convention.result.Result<T>> action) {
        AgentUserContext previous = AgentUserContextHolder.current();
        try {
            AgentUserContextHolder.set(authenticationContext.resolve(request));
            return action.get();
        } finally {
            if (previous == null) AgentUserContextHolder.clear(); else AgentUserContextHolder.set(previous);
        }
    }
}
