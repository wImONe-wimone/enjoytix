package com.wimone.enjoytix.agent.api;

import com.wimone.enjoytix.agent.auth.AgentAuthenticationContext;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.model.AgentConversation;
import com.wimone.enjoytix.agent.model.AgentStreamEvent;
import com.wimone.enjoytix.agent.service.AgentApplicationService;
import com.wimone.enjoytix.agent.service.AgentStreamListener;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/agent/conversations")
public class AgentConversationController {
    private final AgentApplicationService agentApplicationService;
    private final AgentAuthenticationContext authenticationContext;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentConversationController(AgentApplicationService agentApplicationService) {
        this(agentApplicationService, null);
    }

    @Autowired
    public AgentConversationController(AgentApplicationService agentApplicationService,
                                       AgentAuthenticationContext authenticationContext) {
        this.agentApplicationService = agentApplicationService;
        this.authenticationContext = authenticationContext;
    }

    @PostMapping
    public Result<AgentConversation> create(HttpServletRequest request) {
        AgentUserContext user = resolve(request);
        return Results.success(withContext(user, () -> agentApplicationService.createConversation(user.userId())));
    }

    @GetMapping("/{conversationId}")
    public Result<AgentConversation> get(HttpServletRequest request, @PathVariable Long conversationId) {
        AgentUserContext user = resolve(request);
        return Results.success(withContext(user, () -> agentApplicationService.getConversation(conversationId, user.userId())));
    }

    @PostMapping("/{conversationId}/messages")
    public SseEmitter send(HttpServletRequest request, @PathVariable Long conversationId,
                           @Valid @RequestBody AgentMessageReq messageRequest) {
        AgentUserContext user = resolve(request);
        SseEmitter emitter = new SseEmitter(30_000L);
        AtomicBoolean cancelled = new AtomicBoolean();
        Future<?> task = executor.submit(() -> {
            try {
                AgentUserContextHolder.set(user);
                agentApplicationService.sendMessage(conversationId, user.userId(), messageRequest.content(),
                        new AgentStreamListener() {
                            @Override
                            public void onEvent(AgentStreamEvent event) {
                                try {
                                    emitter.send(SseEmitter.event().name(event.type())
                                            .data(event.data(), MediaType.APPLICATION_JSON));
                                } catch (IOException ex) {
                                    cancelled.set(true);
                                    throw new UncheckedIOException(ex);
                                }
                            }

                            @Override
                            public boolean isCancelled() {
                                return cancelled.get();
                            }
                        });
                if (!cancelled.get()) emitter.complete();
            } catch (RuntimeException ex) {
                if (!cancelled.get()) emitter.completeWithError(ex);
            } finally {
                AgentUserContextHolder.clear();
            }
        });
        emitter.onCompletion(() -> { cancelled.set(true); task.cancel(true); });
        emitter.onTimeout(() -> { cancelled.set(true); task.cancel(true); });
        emitter.onError(error -> { cancelled.set(true); task.cancel(true); });
        return emitter;
    }

    private AgentUserContext resolve(HttpServletRequest request) {
        if (authenticationContext != null) return authenticationContext.resolve(request);
        String value = request.getHeader("X-User-Id");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Authenticated user context is required");
        return new AgentUserContext(Long.valueOf(value), request.getHeader("X-Username"));
    }

    private <T> T withContext(AgentUserContext user, java.util.function.Supplier<T> action) {
        AgentUserContext previous = AgentUserContextHolder.current();
        try {
            AgentUserContextHolder.set(user);
            return action.get();
        } finally {
            if (previous == null) AgentUserContextHolder.clear(); else AgentUserContextHolder.set(previous);
        }
    }

    @PreDestroy
    public void shutdown() { executor.shutdownNow(); }
}