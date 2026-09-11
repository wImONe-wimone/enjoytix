package com.wimone.enjoytix.agent.api;

import com.wimone.enjoytix.agent.model.AgentConversation;
import com.wimone.enjoytix.agent.model.AgentStreamEvent;
import com.wimone.enjoytix.agent.service.AgentApplicationService;
import com.wimone.enjoytix.agent.service.AgentStreamListener;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentConversationController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    @PostMapping
    public Result<AgentConversation> create(@RequestHeader("X-User-Id") Long userId) {
        return Results.success(agentApplicationService.createConversation(userId));
    }

    @GetMapping("/{conversationId}")
    public Result<AgentConversation> get(@RequestHeader("X-User-Id") Long userId,
                                         @PathVariable Long conversationId) {
        return Results.success(agentApplicationService.getConversation(conversationId, userId));
    }

    @PostMapping("/{conversationId}/messages")
    public SseEmitter send(@RequestHeader("X-User-Id") Long userId,
                           @PathVariable Long conversationId,
                           @Valid @RequestBody AgentMessageReq request) {
        SseEmitter emitter = new SseEmitter(30_000L);
        AtomicBoolean cancelled = new AtomicBoolean();
        Future<?> task = executor.submit(() -> {
            try {
                agentApplicationService.sendMessage(conversationId, userId, request.content(),
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
                if (!cancelled.get()) {
                    emitter.complete();
                }
            } catch (RuntimeException ex) {
                if (!cancelled.get()) {
                    emitter.completeWithError(ex);
                }
            }
        });
        emitter.onCompletion(() -> {
            cancelled.set(true);
            task.cancel(true);
        });
        emitter.onTimeout(() -> {
            cancelled.set(true);
            task.cancel(true);
        });
        emitter.onError(error -> {
            cancelled.set(true);
            task.cancel(true);
        });
        return emitter;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
